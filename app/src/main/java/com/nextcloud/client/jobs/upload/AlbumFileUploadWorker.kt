/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.app.PendingIntent
import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.ACCOUNT
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.CURRENT_BATCH_INDEX
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.SHOW_SAME_FILE_ALREADY_EXISTS_NOTIFICATION
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.TOTAL_UPLOAD_SIZE
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.UPLOAD_IDS
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateLiveData
import com.nextcloud.utils.extensions.getPercent
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.albums.CopyFileToAlbumOperation
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * this worker is a replica of FileUploadWorker
 * this worker will take care of upload and then copying the uploaded files to selected Album
 */
@Suppress("LongParameterList")
class AlbumFileUploadWorker(
    val uploadsStorageManager: UploadsStorageManager,
    val connectivityService: ConnectivityService,
    val powerManagementService: PowerManagementService,
    val userAccountManager: UserAccountManager,
    val viewThemeUtils: ViewThemeUtils,
    val localBroadcastManager: LocalBroadcastManager,
    private val backgroundJobManager: BackgroundJobManager,
    val preferences: AppPreferences,
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params),
    OnDatatransferProgressListener {

    companion object {
        val TAG: String = AlbumFileUploadWorker::class.java.simpleName

        var currentUploadFileOperation: UploadFileOperation? = null

        private const val BATCH_SIZE = 100

        const val ALBUM_NAME = "album_name"
    }

    private var lastPercent = 0
    private val notificationManager = UploadNotificationManager(context, viewThemeUtils, Random.nextInt())
    private val intents = FileUploaderIntents(context)
    private val fileUploaderDelegate = FileUploaderDelegate()

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = try {
        Log_OC.d(TAG, "AlbumFileUploadWorker started")
        backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class))
        val result = uploadFiles()
        backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
        notificationManager.dismissNotification()
        if (result == Result.success()) {
            setIdleWorkerState()
        }
        result
    } catch (t: Throwable) {
        Log_OC.e(TAG, "Error caught at AlbumFileUploadWorker $t")
        cleanup()
        Result.failure()
    }

    private fun cleanup() {
        Log_OC.e(TAG, "AlbumFileUploadWorker stopped")

        setIdleWorkerState()
        currentUploadFileOperation?.cancel(null)
        notificationManager.dismissNotification()
    }

    private fun setWorkerState(user: User?) {
        WorkerStateLiveData.instance().setWorkState(WorkerState.UploadStarted(user))
    }

    private fun setIdleWorkerState() {
        WorkerStateLiveData.instance().setWorkState(WorkerState.UploadFinished(currentUploadFileOperation?.file))
    }

    @Suppress("ReturnCount", "LongMethod")
    private suspend fun uploadFiles(): Result = withContext(Dispatchers.IO) {
        val accountName = inputData.getString(ACCOUNT)
        if (accountName == null) {
            Log_OC.e(TAG, "accountName is null")
            return@withContext Result.failure()
        }

        val uploadIds = inputData.getLongArray(UPLOAD_IDS)
        if (uploadIds == null) {
            Log_OC.e(TAG, "uploadIds is null")
            return@withContext Result.failure()
        }

        val currentBatchIndex = inputData.getInt(CURRENT_BATCH_INDEX, -1)
        if (currentBatchIndex == -1) {
            Log_OC.e(TAG, "currentBatchIndex is -1, cancelling")
            return@withContext Result.failure()
        }

        val totalUploadSize = inputData.getInt(TOTAL_UPLOAD_SIZE, -1)
        if (totalUploadSize == -1) {
            Log_OC.e(TAG, "totalUploadSize is -1, cancelling")
            return@withContext Result.failure()
        }

        // since worker's policy is append or replace and account name comes from there no need check in the loop
        val optionalUser = userAccountManager.getUser(accountName)
        if (!optionalUser.isPresent) {
            Log_OC.e(TAG, "User not found for account: $accountName")
            return@withContext Result.failure()
        }

        val albumName = inputData.getString(ALBUM_NAME)
        if (albumName == null) {
            Log_OC.e(TAG, "album name is null")
            return@withContext Result.failure()
        }

        val user = optionalUser.get()
        val previouslyUploadedFileSize = currentBatchIndex * FileUploadHelper.MAX_FILE_COUNT
        val uploads = uploadsStorageManager.getUploadsByIds(uploadIds, accountName)
        val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
        val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)

        for ((index, upload) in uploads.withIndex()) {
            ensureActive()

            if (preferences.isGlobalUploadPaused) {
                Log_OC.d(TAG, "Upload is paused, skip uploading files!")
                notificationManager.notifyPaused(
                    intents.notificationStartIntent(null)
                )
                return@withContext Result.success()
            }

            if (canExitEarly()) {
                notificationManager.showConnectionErrorNotification()
                return@withContext Result.failure()
            }

            setWorkerState(user)
            val operation = createUploadFileOperation(upload, user)
            currentUploadFileOperation = operation

            val currentIndex = (index + 1)
            val currentUploadIndex = (currentIndex + previouslyUploadedFileSize)
            notificationManager.prepareForStart(
                operation,
                cancelPendingIntent = intents.startIntent(operation),
                startIntent = intents.notificationStartIntent(operation),
                currentUploadIndex = currentUploadIndex,
                totalUploadSize = totalUploadSize
            )

            val result = withContext(Dispatchers.IO) {
                upload(operation, albumName, user, client)
            }
            currentUploadFileOperation = null
            sendUploadFinishEvent(totalUploadSize, currentUploadIndex, operation, result)
        }

        return@withContext Result.success()
    }

    private fun sendUploadFinishEvent(
        totalUploadSize: Int,
        currentUploadIndex: Int,
        operation: UploadFileOperation,
        result: RemoteOperationResult<*>
    ) {
        val shouldBroadcast =
            (totalUploadSize > BATCH_SIZE && currentUploadIndex > 0) && currentUploadIndex % BATCH_SIZE == 0

        if (shouldBroadcast) {
            // delay broadcast
            fileUploaderDelegate.sendBroadcastUploadFinished(
                operation,
                result,
                operation.oldFile?.storagePath,
                context,
                localBroadcastManager
            )
        }
    }

    private fun canExitEarly(): Boolean {
        val result = !connectivityService.isConnected ||
            connectivityService.isInternetWalled ||
            isStopped

        if (result) {
            Log_OC.d(TAG, "No internet connection, stopping worker.")
        } else {
            notificationManager.dismissErrorNotification()
        }

        return result
    }

    private fun createUploadFileOperation(upload: OCUpload, user: User): UploadFileOperation = UploadFileOperation(
        uploadsStorageManager,
        connectivityService,
        powerManagementService,
        user,
        null,
        upload,
        upload.nameCollisionPolicy,
        upload.localAction,
        context,
        upload.isUseWifiOnly,
        upload.isWhileChargingOnly,
        true,
        FileDataStorageManager(user, context.contentResolver)
    ).apply {
        addDataTransferProgressListener(this@AlbumFileUploadWorker)
    }

    @Suppress("TooGenericExceptionCaught", "DEPRECATION")
    private fun upload(
        uploadFileOperation: UploadFileOperation,
        albumName: String,
        user: User,
        client: OwnCloudClient
    ): RemoteOperationResult<Any?> {
        lateinit var result: RemoteOperationResult<Any?>

        try {
            val storageManager = uploadFileOperation.storageManager
            result = uploadFileOperation.execute(client)
            val task = ThumbnailsCacheManager.ThumbnailGenerationTask(storageManager, user)
            val file = File(uploadFileOperation.originalStoragePath)
            val remoteId: String? = uploadFileOperation.file.remoteId
            task.execute(ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, remoteId))
            val copyAlbumFileOperation =
                CopyFileToAlbumOperation(uploadFileOperation.remotePath, albumName, storageManager)
            val copyResult = copyAlbumFileOperation.execute(client)
            if (copyResult.isSuccess) {
                Log_OC.e(TAG, "Successful copied file to Album: $albumName")
            } else {
                Log_OC.e(TAG, "Failed to copy file to Album: $albumName due to ${copyResult.logMessage}")
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error uploading", e)
            result = RemoteOperationResult<Any?>(e)
        } finally {
            cleanupUploadProcess(result, uploadFileOperation)
        }

        return result
    }

    private fun cleanupUploadProcess(result: RemoteOperationResult<Any?>, uploadFileOperation: UploadFileOperation) {
        if (!isStopped || !result.isCancelled) {
            uploadsStorageManager.updateDatabaseUploadResult(result, uploadFileOperation)
            notifyUploadResult(uploadFileOperation, result)
        }
    }

    @Suppress("ReturnCount", "LongMethod")
    private fun notifyUploadResult(
        uploadFileOperation: UploadFileOperation,
        uploadResult: RemoteOperationResult<Any?>
    ) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.code)
        val showSameFileAlreadyExistsNotification =
            inputData.getBoolean(SHOW_SAME_FILE_ALREADY_EXISTS_NOTIFICATION, false)

        if (uploadResult.isSuccess) {
            notificationManager.dismissOldErrorNotification(uploadFileOperation)
            return
        }

        if (uploadResult.isCancelled) {
            return
        }

        // Only notify if it is not same file on remote that causes conflict
        if (uploadResult.code == ResultCode.SYNC_CONFLICT &&
            FileUploadHelper().isSameFileOnRemote(
                uploadFileOperation.user,
                File(uploadFileOperation.storagePath),
                uploadFileOperation.remotePath,
                context
            )
        ) {
            if (showSameFileAlreadyExistsNotification) {
                notificationManager.showSameFileAlreadyExistsNotification(uploadFileOperation.fileName)
            }

            uploadFileOperation.handleLocalBehaviour()
            return
        }

        val notDelayed = uploadResult.code !in setOf(
            ResultCode.DELAYED_FOR_WIFI,
            ResultCode.DELAYED_FOR_CHARGING,
            ResultCode.DELAYED_IN_POWER_SAVE_MODE
        )

        val isValidFile = uploadResult.code !in setOf(
            ResultCode.LOCAL_FILE_NOT_FOUND,
            ResultCode.LOCK_FAILED
        )

        if (!notDelayed || !isValidFile) {
            return
        }

        if (uploadResult.code == ResultCode.SYNC_CONFLICT) {
            // NMC: resolving file conflict will trigger normal file upload and shows two upload process
            // one for normal and one for Album upload
            // as customizing conflict can break normal upload
            // so we are removing the upload if it's a conflict
            // Note: this is fallback logic because default policy while uploading is RENAME
            // if in some case code reach here it will remove the upload
            uploadsStorageManager.removeUpload(
                uploadFileOperation.user.accountName,
                uploadFileOperation.remotePath
            )
            return
        }

        notificationManager.run {
            val errorMessage = ErrorMessageAdapter.getErrorCauseMessage(
                uploadResult,
                uploadFileOperation,
                context.resources
            )

            val credentialIntent: PendingIntent? = if (uploadResult.code == ResultCode.UNAUTHORIZED) {
                intents.credentialIntent(uploadFileOperation)
            } else {
                null
            }

            notifyForFailedResult(
                uploadFileOperation,
                uploadResult.code,
                null,
                null,
                credentialIntent,
                errorMessage
            )
        }
    }

    @Suppress("MagicNumber")
    private val minProgressUpdateInterval = 750
    private var lastUpdateTime = 0L

    /**
     * Receives from [com.owncloud.android.operations.UploadFileOperation.normalUpload]
     */
    @Suppress("MagicNumber")
    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        fileAbsoluteName: String
    ) {
        val percent = getPercent(totalTransferredSoFar, totalToTransfer)
        val currentTime = System.currentTimeMillis()

        if (percent != lastPercent && (currentTime - lastUpdateTime) >= minProgressUpdateInterval) {
            notificationManager.run {
                val accountName = currentUploadFileOperation?.user?.accountName
                val remotePath = currentUploadFileOperation?.remotePath

                updateUploadProgress(percent, currentUploadFileOperation)

                if (accountName != null && remotePath != null) {
                    val key: String = FileUploadHelper.buildRemoteName(accountName, remotePath)
                    val boundListener = FileUploadHelper.mBoundListeners[key]
                    val filename = currentUploadFileOperation?.fileName ?: ""

                    boundListener?.onTransferProgress(
                        progressRate,
                        totalTransferredSoFar,
                        totalToTransfer,
                        filename
                    )
                }

                dismissOldErrorNotification(currentUploadFileOperation)
            }
            lastUpdateTime = currentTime
        }

        lastPercent = percent
    }
}
