/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023-2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.ActivitySplashBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import javax.inject.Inject

class LauncherActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var appPreferences: AppPreferences

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        // Fix of NMC-2464 & NMC-3183
        if (userAccountManager.user.isAnonymous) {
            startActivity(Intent(this, AuthenticatorActivity::class.java))
        }
        // if user is logged in but did not accepted the privacy policy then take him there
        // show him the privacy policy screen again
        // check if app has been updated, if yes then also we have to show the privacy policy screen
        else if (!userAccountManager.user.isAnonymous && (appPreferences.privacyPolicyAction == PrivacyUserAction.NO_ACTION
                || appPreferences.lastSeenVersionCode < BuildConfig.VERSION_CODE)
        ) {
            LoginPrivacySettingsActivity.openPrivacySettingsActivity(this)
        } else if (MDMConfig.enforceProtection(this) && appPreferences.lockPreference == SettingsActivity.LOCK_NONE) {
            startActivity(Intent(this, SettingsActivity::class.java))
        } else {
            startActivity(Intent(this, FileDisplayActivity::class.java))
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Mandatory to call this before super method to show system launch screen for api level 31+
        installSplashScreen()

        //Fix of NMC-2464
        //this is mandatory to call before super() function
        //setting false to show launcher screen properly if user is not logged in
        enableAccountHandling = false

        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)

        setContentView(binding.root)
        updateTitleVisibility()
    }

    override fun onResume() {
        super.onResume()
        scheduleSplashScreen()
    }

    @VisibleForTesting
    fun setSplashTitles(boldText: String, normalText: String) {
        binding.splashScreenBold.visibility = View.VISIBLE
        binding.splashScreenNormal.visibility = View.VISIBLE

        binding.splashScreenBold.text = boldText
        binding.splashScreenNormal.text = normalText
    }

    private fun updateTitleVisibility() {
        if (TextUtils.isEmpty(resources.getString(R.string.splashScreenBold))) {
            binding.splashScreenBold.visibility = View.GONE
        }
        if (TextUtils.isEmpty(resources.getString(R.string.splashScreenNormal))) {
            binding.splashScreenNormal.visibility = View.GONE
        }
    }

    private fun scheduleSplashScreen() {
        handler.postDelayed(
            runnable,
            SPLASH_DURATION
        )
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    companion object {
        const val SPLASH_DURATION = 1500L
    }
}