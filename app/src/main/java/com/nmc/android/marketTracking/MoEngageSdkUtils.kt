package com.nmc.android.marketTracking

import android.app.Application
import com.moengage.core.MoEngage
import com.owncloud.android.BuildConfig
import com.owncloud.android.R

object MoEngageSdkUtils {

    //enable/disable moengage as we are not using it right now due to no proper firebase api key
    private const val MOENGAGE_ENABLED = false

    @JvmStatic
    fun initMoEngageSDK(application: Application) {
        if (MOENGAGE_ENABLED) {
            val moEngage = MoEngage.Builder(application, BuildConfig.MOENGAGE_APP_ID)
                .build()
            MoEngage.initialise(moEngage)
        }
    }
}