package com.nmc.android.ui

import android.content.Context

/**
 * interface to open privacy settings activity from nmc/1921-settings branch
 * for implementation look nmc/1878-privacy branch
 * this class will have the declaration for it since it has the PrivacySettingsActivity.java in place
 * since we don't have privacy settings functionality in this branch so to handle the redirection we have used interface
 */
interface PrivacySettingsInterface {
    fun openPrivacySettingsActivity(context: Context)
}