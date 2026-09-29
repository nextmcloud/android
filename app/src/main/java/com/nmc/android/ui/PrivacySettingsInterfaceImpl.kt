package com.nmc.android.ui

import android.content.Context

/**
 * interface impl to launch PrivacySettings Activity
 * this class will have the implementation for it since it has the PrivacySettingsActivity in place
 * calling of this method will be done from nmc/1921-settings
 */
class PrivacySettingsInterfaceImpl : PrivacySettingsInterface {
    override fun openPrivacySettingsActivity(context: Context) {
        PrivacySettingsActivity.openPrivacySettingsActivity(context, false)
    }
}