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
import com.moengage.core.MoECoreHelper
import com.moengage.core.MoEngage
import com.moengage.core.analytics.MoEAnalyticsHelper
import com.moengage.core.enableAdIdTracking
import com.moengage.core.enableAndroidIdTracking
import com.moengage.core.model.AppStatus
import com.owncloud.android.BuildConfig
import com.owncloud.android.lib.common.Quota
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.utils.DisplayUtils
import kotlin.math.ceil

object MoEngageSdkUtils {

    private const val USER_PROPERTIES__STORAGE_CAPACITY = "storage_capacity" // in GB
    private const val USER_PROPERTIES__STORAGE_USED = "storage_used" // % of storage used
    private const val USER_PROPERTIES__CONTACT_BACKUP = "contact_backup_on"
    private const val USER_PROPERTIES__AUTO_UPLOAD = "auto_upload_on"
    private const val USER_PROPERTIES__APP_VERSION = "app_version"

    @JvmStatic
    fun initMoEngageSDK(application: Application) {
        val moEngage = MoEngage.Builder(application, BuildConfig.MOENGAGE_APP_ID, DataCenter.DATA_CENTER_2)
            .build()
        MoEngage.initialiseDefaultInstance(moEngage)
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
        userInfo.displayName?.let{
            MoEAnalyticsHelper.setUserName(context, it)
        }
        userInfo.email?.let{
            MoEAnalyticsHelper.setEmailId(context, it)
        }
        trackQuotaStorage(context, userInfo.quota)
    }

    @JvmStatic
    fun trackQuotaStorage(context: Context, quota: Quota?) {
        quota?.let {
            val totalQuota = if (it.quota > 0) {
                DisplayUtils.bytesToHumanReadable(it.total)
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
    fun trackUserLogout(context: Context) {
        MoECoreHelper.logoutUser(context)
    }
}