package com.nmc.android.marketTracking

import com.nextcloud.client.preferences.AppPreferences

/**
 * interface impl to send the scanning events to tealium and adjust
 * this class will have the implementation for it since it has the tracking SDK's in place
 * since we don't have scanning functionality in this branch so to handle the event we have used interface
 * calling of this method will be done from nmc/1867-scanbot
 */
class TrackingScanInterfaceImpl : TrackingScanInterface {

    override fun sendScanEvent(appPreferences: AppPreferences) {
        //track event on Scan Document button click
        AdjustSdkUtils.trackEvent(AdjustSdkUtils.EVENT_TOKEN_FAB_BOTTOM_DOCUMENT_SCAN, appPreferences)
        TealiumSdkUtils.trackEvent(TealiumSdkUtils.EVENT_FAB_BOTTOM_DOCUMENT_SCAN, appPreferences)
    }
}