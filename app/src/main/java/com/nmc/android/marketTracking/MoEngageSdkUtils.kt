/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nmc.android.marketTracking

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import com.moengage.core.DataCenter
import com.moengage.core.MoECoreHelper
import com.moengage.core.MoEngage
import com.moengage.core.Properties
import com.moengage.core.analytics.MoEAnalyticsHelper
import com.moengage.core.config.NotificationConfig
import com.moengage.core.enableAdIdTracking
import com.moengage.core.enableAndroidIdTracking
import com.moengage.core.model.AppStatus
import com.moengage.inapp.MoEInAppHelper
import com.moengage.pushbase.MoEPushHelper
import com.nextcloud.client.account.User
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.extensions.getFormattedStringDate
import com.nmc.android.utils.FileUtils
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.Template
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.Quota
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.PermissionUtil
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

object MoEngageSdkUtils {

    private const val USER_PROPERTIES__STORAGE_CAPACITY = "storage_capacity" // in GB
    private const val USER_PROPERTIES__STORAGE_USED = "storage_used" // % of storage used
    private const val USER_PROPERTIES__CONTACT_BACKUP = "contact_backup_on"
    private const val USER_PROPERTIES__AUTO_UPLOAD = "auto_upload_on"
    private const val USER_PROPERTIES__APP_VERSION = "app_version"

    private const val EVENT__ACTION_BUTTON = "action_button_clicked" // when user clicks on fab + button
    private const val EVENT__UPLOAD_FILE =
        "upload_file" // when user uploads any file (not applicable for folder) from other apps
    private const val EVENT__CREATE_FILE = "create_file" // when user creates any file in app
    private const val EVENT__CREATE_FOLDER = "create_folder"
    private const val EVENT__ADD_FAVORITE = "add_favorite"
    private const val EVENT__SHARE_FILE = "share_file" // when user share any file using link
    private const val EVENT__OFFLINE_AVAILABLE = "offline_available"
    private const val EVENT__PIN_TO_HOME_SCREEN = "pin_to_homescreen"
    private const val EVENT__ONLINE_OFFICE_USED = "online_office_used" // when user opens any office files

    // screen view events when user open specific screen
    private const val SCREEN_EVENT__FAVOURITES = "favorites"
    private const val SCREEN_EVENT__MEDIA = "medien"
    private const val SCREEN_EVENT__OFFLINE_FILES = "offline_files"
    private const val SCREEN_EVENT__SHARED = "shared"
    private const val SCREEN_EVENT__DELETED_FILES = "deleted_files"
    private const val SCREEN_EVENT__NOTIFICATIONS = "notifications"

    // properties attributes key
    private const val PROPERTIES__FILE_TYPE = "file_type"
    private const val PROPERTIES__FOLDER_TYPE = "folder_type"
    private const val PROPERTIES__FILE_SIZE = "file_size" // in MB
    private const val PROPERTIES__CREATION_DATE = "creation_date" // yyyy-MM-dd
    private const val PROPERTIES__UPLOAD_DATE = "upload_date" // // yyyy-MM-dd

    private const val KILOBYTE: Long = 1024
    private const val MEGABYTE = KILOBYTE * 1024
    private const val GIGABYTE = MEGABYTE * 1024

    // app version code for which user attributes need to track
    // this should be the previous version before MoEngage is included
    // Note: will be removed in future once MoEngage feature rolled out to all devices
    private const val OLD_VERSION_CODE = 7_29_00

    private const val DATE_FORMAT = "yyyy-MM-dd"

    // maximum post notification permission retry count
    private const val PUSH_PERMISSION_REQUEST_RETRY_COUNT = 2

    @JvmStatic
    fun initMoEngageSDK(application: Application) {
        val moEngage = MoEngage.Builder(application, BuildConfig.MOENGAGE_APP_ID, DataCenter.DATA_CENTER_2)
            .configureNotificationMetaData(
                NotificationConfig(
                    R.drawable.notification_icon,
                    R.drawable.notification_icon,
                    R.color.primary,
                    false
                )
            )
            .build()
        MoEngage.initialiseDefaultInstance(moEngage)

        updatePostNotificationsPermission(application)

        enableDeviceIdentifierTracking(application)

        // track app version at app launch
        trackAppVersion(application)
    }

    // for NMC the default privacy tracking consent is always taken from users
    // so the tracking will always be enabled for MoEngage
    private fun enableDeviceIdentifierTracking(context: Context) {
        enableAndroidIdTracking(context)
        enableAdIdTracking(context)
    }

    private fun trackAppVersion(context: Context) {
        MoEAnalyticsHelper.setUserAttribute(context, USER_PROPERTIES__APP_VERSION, BuildConfig.VERSION_NAME)
    }

    /**
     * method to check if a user updated the app from older version where MoEngage was not included
     * if user app version is old and is logged in then we have to auto capture the user attributes to map the events
     * Note: Will be removed when MoEngage will be rolled out to all versions
     */
    @JvmStatic
    fun captureUserAttrsForOldAppVersion(
        context: Context,
        lastSeenVersionCode: Int,
        user: User
    ) {
        if (lastSeenVersionCode in 1..OLD_VERSION_CODE && !user.isAnonymous) {
            fetchUserInfo(context, user)
        }

        // if user is not logged in for older app versions then nothing to do
        // as the events will be captured after successful login
    }

    @JvmStatic
    fun trackAppInstallOrUpdate(context: Context, lastSeenVersionCode: Int) {
        if (lastSeenVersionCode <= 0) {
            trackAppInstall(context)
        } else if (lastSeenVersionCode < BuildConfig.VERSION_CODE) {
            trackAppUpdate(context)
        }
        // For same version code no event has to send
    }

    private fun trackAppInstall(context: Context) {
        // For Fresh Install of App
        MoEAnalyticsHelper.setAppStatus(context, AppStatus.INSTALL)
    }

    private fun trackAppUpdate(context: Context) {
        // For Existing user who has updated the app
        MoEAnalyticsHelper.setAppStatus(context, AppStatus.UPDATE)
    }

    @JvmStatic
    fun trackUserLogin(context: Context, userInfo: UserInfo) {
        userInfo.id?.let {
            MoEAnalyticsHelper.setUniqueId(context, it)
        }
        userInfo.displayName?.let {
            MoEAnalyticsHelper.setUserName(context, it)
        }
        userInfo.email?.let {
            MoEAnalyticsHelper.setEmailId(context, it)
        }
        trackQuotaStorage(context, userInfo.quota)
    }

    @JvmStatic
    fun trackQuotaStorage(context: Context, quota: Quota?) {
        quota?.let {
            val totalQuota = if (it.quota > 0) {
                bytesToGB(it.total).toString()
            } else {
                it.total.toString()
            }
            // capture storage capacity
            MoEAnalyticsHelper.setUserAttribute(context, USER_PROPERTIES__STORAGE_CAPACITY, totalQuota)

            val usedSpace = ceil(it.relative).toInt()
            // capture storage used
            MoEAnalyticsHelper.setUserAttribute(context, USER_PROPERTIES__STORAGE_USED, usedSpace)
        }
    }

    @JvmStatic
    fun trackContactBackup(context: Context, isEnabled: Boolean) {
        MoEAnalyticsHelper.setUserAttribute(context, USER_PROPERTIES__CONTACT_BACKUP, isEnabled)
    }

    @JvmStatic
    fun trackAutoUpload(context: Context, syncedFoldersCount: Int) {
        // since multiple folders can be enabled for auto upload
        // user can add or remove a folder anytime, and we don't have single flag to check if auto upload is enabled
        // so we have to check the count and if there are folders more than 0 i.e. auto upload is enabled
        MoEAnalyticsHelper.setUserAttribute(context, USER_PROPERTIES__AUTO_UPLOAD, syncedFoldersCount > 0)
    }

    @JvmStatic
    fun trackActionButtonEvent(context: Context) {
        MoEAnalyticsHelper.trackEvent(context, EVENT__ACTION_BUTTON, Properties())
    }

    @JvmStatic
    fun trackUploadFileEvent(context: Context, file: OCFile, originalStoragePath: String) {
        if (file.isFolder) return

        MoEAnalyticsHelper.trackEvent(
            context, EVENT__UPLOAD_FILE, getCommonProperties(
                file,
                FileUtils.isScannedFiles(context, originalStoragePath)
            )
        )
    }

    @JvmStatic
    fun trackCreateFileEvent(context: Context, file: OCFile, type: Template.Type? = null) {
        if (file.isFolder) return

        val properties = Properties()
        properties.addAttribute(PROPERTIES__FILE_TYPE, getOfficeFileType(type) { getFileType(file) }.fileType)
        properties.addAttribute(PROPERTIES__FILE_SIZE, bytesToMBInDecimal(file.fileLength).toString())
        properties.addAttribute(
            PROPERTIES__CREATION_DATE,
            // using modification timestamp as this will always have value
            file.modificationTimestamp.getFormattedStringDate(DATE_FORMAT)
        )

        MoEAnalyticsHelper.trackEvent(context, EVENT__CREATE_FILE, properties)
    }

    @JvmStatic
    fun trackCreateFolderEvent(context: Context, file: OCFile) {
        if (!file.isFolder) return

        val properties = Properties()
        properties.addAttribute(PROPERTIES__FOLDER_TYPE, getFolderType(file).folderType)
        properties.addAttribute(
            PROPERTIES__CREATION_DATE,
            // using modification timestamp because for folder creationTimeStamp is always 0
            file.modificationTimestamp.getFormattedStringDate(DATE_FORMAT)
        )

        MoEAnalyticsHelper.trackEvent(context, EVENT__CREATE_FOLDER, properties)
    }

    @JvmStatic
    fun trackAddFavoriteEvent(context: Context, file: OCFile) {
        if (file.isFolder) return

        MoEAnalyticsHelper.trackEvent(context, EVENT__ADD_FAVORITE, getCommonProperties(file))
    }

    @JvmStatic
    fun trackShareFileEvent(context: Context, file: OCFile) {
        if (file.isFolder) return

        MoEAnalyticsHelper.trackEvent(context, EVENT__SHARE_FILE, getCommonProperties(file))
    }

    @JvmStatic
    fun trackOfflineAvailableEvent(context: Context, file: OCFile) {
        if (file.isFolder) return

        MoEAnalyticsHelper.trackEvent(context, EVENT__OFFLINE_AVAILABLE, getCommonProperties(file))
    }

    @JvmStatic
    fun trackPinHomeScreenEvent(context: Context, file: OCFile) {
        if (file.isFolder) return

        MoEAnalyticsHelper.trackEvent(context, EVENT__PIN_TO_HOME_SCREEN, getCommonProperties(file))
    }

    @JvmStatic
    fun trackOnlineOfficeUsedEvent(context: Context, file: OCFile) {
        if (file.isFolder) return

        MoEAnalyticsHelper.trackEvent(context, EVENT__ONLINE_OFFICE_USED, Properties())
    }

    @JvmStatic
    fun trackFavouriteScreenEvent(context: Context) {
        MoEAnalyticsHelper.trackEvent(context, SCREEN_EVENT__FAVOURITES, Properties())
    }

    @JvmStatic
    fun trackMediaScreenEvent(context: Context) {
        MoEAnalyticsHelper.trackEvent(context, SCREEN_EVENT__MEDIA, Properties())
    }

    @JvmStatic
    fun trackOfflineFilesScreenEvent(context: Context) {
        MoEAnalyticsHelper.trackEvent(context, SCREEN_EVENT__OFFLINE_FILES, Properties())
    }

    @JvmStatic
    fun trackSharedScreenEvent(context: Context) {
        MoEAnalyticsHelper.trackEvent(context, SCREEN_EVENT__SHARED, Properties())
    }

    @JvmStatic
    fun trackDeletedFilesScreenEvent(context: Context) {
        MoEAnalyticsHelper.trackEvent(context, SCREEN_EVENT__DELETED_FILES, Properties())
    }

    @JvmStatic
    fun trackNotificationsScreenEvent(context: Context) {
        MoEAnalyticsHelper.trackEvent(context, SCREEN_EVENT__NOTIFICATIONS, Properties())
    }

    @JvmStatic
    fun trackUserLogout(context: Context) {
        MoECoreHelper.logoutUser(context)
    }

    private fun getCommonProperties(file: OCFile, isScan: Boolean = false): Properties {
        val properties = Properties()
        properties.addAttribute(PROPERTIES__FILE_TYPE, getFileType(file, isScan).fileType)
        properties.addAttribute(PROPERTIES__FILE_SIZE, bytesToMBInDecimal(file.fileLength).toString())
        properties.addAttribute(
            PROPERTIES__CREATION_DATE,
            // using modification timestamp as this will always have value
            file.modificationTimestamp.getFormattedStringDate(DATE_FORMAT)
        )
        properties.addAttribute(PROPERTIES__UPLOAD_DATE, file.uploadTimestamp.getFormattedStringDate(DATE_FORMAT))
        return properties
    }

    private fun bytesToGB(bytes: Long): Int {
        return floor((bytes / GIGABYTE).toDouble()).toInt()
    }

     private fun bytesToMBInDecimal(bytes: Long): Double {
         val mb = bytes.toDouble() / MEGABYTE
         return round((mb * 10)) / 10 // Round down to 1 decimal place
    }

    private fun getFileType(file: OCFile, isScan: Boolean = false): EventFileType {
        // if upload is happening through scan then no need to check mime type
        // just set SCAN as type and send event
        if (isScan) return EventFileType.SCAN

        return when {
            MimeTypeUtil.isImage(file) -> {
                EventFileType.PHOTO
            }

            MimeTypeUtil.isVideo(file) -> {
                EventFileType.VIDEO
            }

            MimeTypeUtil.isAudio(file) -> {
                EventFileType.AUDIO
            }

            MimeTypeUtil.isPDF(file) -> {
                EventFileType.PDF
            }

            MimeTypeUtil.isText(file) -> {
                EventFileType.TEXT
            }

            else -> {
                EventFileType.OTHER
            }
        }
    }

    private fun getOfficeFileType(
        type: Template.Type?,
        getFileType: () -> EventFileType
    ): EventFileType {
        return when (type) {
            Template.Type.DOCUMENT -> {
                EventFileType.DOCUMENT
            }

            Template.Type.SPREADSHEET -> {
                EventFileType.SPREADSHEET
            }

            Template.Type.PRESENTATION -> {
                EventFileType.PRESENTATION
            }

            else -> {
                getFileType()
            }
        }
    }

    private fun getFolderType(file: OCFile): EventFolderType {
        return if (file.isEncrypted) {
            EventFolderType.ENCRYPTED
        } else {
            EventFolderType.NOT_ENCRYPTED
        }
    }

    private fun fetchUserInfo(context: Context, user: User) {
        val t = Thread(Runnable {
            val nextcloudClient: NextcloudClient
            try {
                nextcloudClient = OwnCloudClientFactory.createNextcloudClient(
                    user,
                    context
                )
            } catch (e: AccountUtils.AccountNotFoundException) {
                Log_OC.e(this, "Error retrieving user info", e)
                return@Runnable
            } catch (e: SecurityException) {
                Log_OC.e(this, "Error retrieving user info", e)
                return@Runnable
            }

            val result = GetUserInfoRemoteOperation().execute(nextcloudClient)
            if (result.isSuccess && result.resultData != null) {
                val userInfo = result.resultData

                trackUserLogin(context, userInfo)
            } else {
                Log_OC.d(this, result.logMessage)
            }
        })

        t.start()
    }

    @JvmStatic
    fun updatePostNotificationsPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = PermissionUtil.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)

            MoEPushHelper.getInstance().pushPermissionResponse(context, isGranted)

            if (!isGranted) {
                MoEPushHelper.getInstance()
                    .updatePushPermissionRequestCount(context, PUSH_PERMISSION_REQUEST_RETRY_COUNT)
            }
        } else {
            MoEPushHelper.getInstance().setUpNotificationChannels(context)
        }
    }

    /**
     * function should be called from onStart() of Activity
     * or onResume() of Fragment
     */
    @JvmStatic
    fun displayInAppNotification(context: Context) {
        MoEInAppHelper.getInstance().showInApp(context)
    }

    /**
     * To show In-App in both Portrait and Landscape mode properly
     * when Activity is handling Config changes by itself
     * call this function from onConfigurationChanged()
     */
    @JvmStatic
    fun handleConfigChangesForInAppNotification() {
        MoEInAppHelper.getInstance().onConfigurationChanged()
    }
}