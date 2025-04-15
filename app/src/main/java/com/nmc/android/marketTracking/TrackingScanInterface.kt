package com.nmc.android.marketTracking

import com.nextcloud.client.preferences.AppPreferences

/**
 * interface to track the scanning events from nmc/1867-scanbot branch
 * for implementation look nmc/1925-market_tracking branch
 * this class will have the declaration for it since it has the tracking SDK's in place
 * since we don't have scanning functionality in this branch so to handle the event we have used interface
 */
interface TrackingScanInterface {

    fun sendScanEvent(appPreferences: AppPreferences)
}