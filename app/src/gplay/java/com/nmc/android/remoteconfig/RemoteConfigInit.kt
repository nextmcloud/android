package com.nmc.android.remoteconfig

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import java.util.concurrent.TimeUnit

/**
 * class to fetch and activate remote config for the app update feature
 */
class RemoteConfigInit(context: Application) {

    companion object {
        private val TAG = RemoteConfigInit::class.java.simpleName

        const val FORCE_UPDATE_KEY = "android_force_update"
        const val APP_VERSION_KEY = "android_app_version"

        private const val INTERVAL_FOR_DEVELOPMENT = 0L //0 sec for immediate update

        // by default the sync value is 12 hours which is not required in our case
        // as we will be only using this for app update and since the app updates are done in few months
        // so fetching the data in 1 day
        private val INTERVAL_FOR_PROD = TimeUnit.DAYS.toSeconds(1) //1 day

        private fun getMinimumTimeToFetchConfigs(): Long {
            return if (BuildConfig.DEBUG) INTERVAL_FOR_DEVELOPMENT else INTERVAL_FOR_PROD
        }
    }

    private var remoteConfig: FirebaseRemoteConfig

    init {
        // init firebase
        // fix: NMC-3449
        // fix: NMC-3848
        // Initialize Firebase (only if not already initialized)
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        remoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = getMinimumTimeToFetchConfigs()
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        fetchAndActivateConfigs()
    }

    private fun fetchAndActivateConfigs() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log_OC.d(TAG, "Config params updated: $updated\nFetch and activate succeeded.")
                } else {
                    Log_OC.e(TAG, "Fetch failed.")
                }
            }
    }
}