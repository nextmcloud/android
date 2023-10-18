package com.nmc.android.appupdate

import android.app.Activity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.nmc.android.remoteconfig.RemoteConfigInit.Companion.APP_VERSION_KEY
import com.nmc.android.remoteconfig.RemoteConfigInit.Companion.FORCE_UPDATE_KEY
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.DisplayUtils

class InAppUpdateHelperImpl(private val activity: AppCompatActivity) : InAppUpdateHelper, InstallStateUpdatedListener {

    companion object {
        private val TAG = InAppUpdateHelperImpl::class.java.simpleName
    }

    private val remoteConfig = Firebase.remoteConfig
    private val isForceUpdate = remoteConfig.getBoolean(FORCE_UPDATE_KEY)
    private val appVersionCode = remoteConfig.getLong(APP_VERSION_KEY)

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    @AppUpdateType
    private var updateType = if (isForceUpdate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE

    init {
        Log_OC.d(TAG, "App Update Remote Config Values : Force Update- $isForceUpdate -- Version Code- $appVersionCode")

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                Log_OC.d(TAG, "App update is available.")

                // if app version in remote config is not equal to the latest app version code in play store
                // then do the flexible update instead of reading the value from remote config
                if (appUpdateInfo.availableVersionCode() != appVersionCode.toInt()) {
                    Log_OC.d(
                        TAG,
                        "Available app version code mismatch with remote config. Setting update type to optional."
                    )
                    updateType = AppUpdateType.FLEXIBLE
                }

                if (appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                    // Request the update.
                    startAppUpdate(
                        appUpdateInfo,
                        updateType
                    )
                }
            } else {
                Log_OC.d(TAG, "No app update available.")
            }
        }
    }

    private fun startAppUpdate(
        appUpdateInfo: AppUpdateInfo,
        @AppUpdateType updateType: Int
    ) {

        if (updateType == AppUpdateType.FLEXIBLE) {
            // Before starting an update, register a listener for updates.
            appUpdateManager.registerListener(this)
        }

        Log_OC.d(TAG, "App update dialog showing to the user.")

        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            appUpdateResultLauncher,
            AppUpdateOptions.newBuilder(updateType).build()
        )
    }

    private val appUpdateResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    Log_OC.d(TAG, "The user has accepted to download the update or the download finished.")
                }

                Activity.RESULT_CANCELED -> {
                    Log_OC.e(TAG, "Update flow failed: The user has denied or canceled the update.")
                }

                RESULT_IN_APP_UPDATE_FAILED -> {
                    Log_OC.e(
                        TAG,
                        "Update flow failed: Some other error prevented either the user from providing consent or the update from proceeding."
                    )
                }
            }

        }

    private fun flexibleUpdateDownloadCompleted() {
        DisplayUtils.createSnackbar(
            activity.findViewById(android.R.id.content),
            R.string.app_update_downloaded,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(R.string.common_restart) { appUpdateManager.completeUpdate() }
            show()
        }
    }

    override fun onResume() {
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                // for AppUpdateType.IMMEDIATE only, already executing updater
                if (updateType == AppUpdateType.IMMEDIATE) {
                    if (appUpdateInfo.updateAvailability()
                        == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        Log_OC.d(TAG, "Resume the Immediate update if in-app update is already running.")
                        // If an in-app update is already running, resume the update.
                        startAppUpdate(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE
                        )
                    }
                } else if (updateType == AppUpdateType.FLEXIBLE) {
                    // If the update is downloaded but not installed,
                    // notify the user to complete the update.
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        Log_OC.d(TAG, "Resume: Flexible update is downloaded but not installed. User is notified.")
                        flexibleUpdateDownloadCompleted()
                    }
                }
            }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(this)
    }

    override fun onStateUpdate(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Log_OC.d(TAG, "Flexible update is downloaded. User is notified to restart the app.")

            // After the update is downloaded, notifying user via snackbar
            // and request user confirmation to restart the app.
            flexibleUpdateDownloadCompleted()
        }
    }
}