/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nmc.android.marketTracking

import android.app.Application
import android.content.Context
import com.moengage.core.DataCenter
import com.moengage.core.MoEngage
import com.moengage.core.analytics.MoEAnalyticsHelper
import com.moengage.core.enableAdIdTracking
import com.moengage.core.enableAndroidIdTracking
import com.moengage.core.model.AppStatus
import com.owncloud.android.BuildConfig

object MoEngageSdkUtils {

    @JvmStatic
    fun initMoEngageSDK(application: Application) {
        val moEngage = MoEngage.Builder(application, BuildConfig.MOENGAGE_APP_ID, DataCenter.DATA_CENTER_2)
            .build()
        MoEngage.initialiseDefaultInstance(moEngage)
        enableDeviceIdentifierTracking(application)
    }

    // for NMC the default privacy tracking consent is always taken from users
    // so the tracking will always be enabled for MoEngage
    @JvmStatic
    internal fun enableDeviceIdentifierTracking(context: Context) {
        enableAndroidIdTracking(context)
        enableAdIdTracking(context)
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
}