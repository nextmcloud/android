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
import com.moengage.core.enableAdIdTracking
import com.moengage.core.enableAndroidIdTracking
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
}