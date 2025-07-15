/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewpager.widget.ViewPager
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.mdm.MDMConfig
import com.nmc.android.helper.OnBoardingPagerAdapter
import com.nmc.android.helper.OnBoardingUtils.Companion.getOnBoardingItems
import com.nmc.android.utils.DisplayUtils.isLandscapeOrientation
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.FirstRunActivityBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Activity displaying general feature after a fresh install.
 */
class FirstRunActivity : BaseActivity(), ViewPager.OnPageChangeListener, Injectable {

    @JvmField
    @Inject
    var userAccountManager: UserAccountManager? = null

    @JvmField
    @Inject
    var preferences: AppPreferences? = null

    @JvmField
    @Inject
    var appInfo: AppInfo? = null

    @JvmField
    @Inject
    var onboarding: OnboardingService? = null

    @JvmField
    @Inject
    var viewThemeUtilsFactory: ViewThemeUtils.Factory? = null

    private var activityResult: ActivityResultLauncher<Intent>? = null

    private lateinit var binding: FirstRunActivityBinding
    private var defaultViewThemeUtils: ViewThemeUtils? = null

    private var selectedPosition = 0

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableAccountHandling = false

        super.onCreate(savedInstanceState)

        applyDefaultTheme()

        // NMC Customization
        // if device is not tablet then we have to lock it to Portrait mode
        // as we don't have images for that
        if (!com.nmc.android.utils.DisplayUtils.isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        binding = FirstRunActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerActivityResult()
        setupLoginButton()
        deleteAccountAtFirstLaunch()
        updateLoginButtonMargin()
        updateOnBoardingPager(selectedPosition)
        handleOnBackPressed()
    }

    private fun applyDefaultTheme() {
        defaultViewThemeUtils = viewThemeUtilsFactory?.withPrimaryAsBackground()
        defaultViewThemeUtils?.platform?.colorStatusBar(this, resources.getColor(R.color.primary))
    }

    private fun registerActivityResult() {
        activityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (RESULT_OK == result.resultCode) {
                    val data = result.data
                    val accountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    val account = userAccountManager?.getAccountByName(accountName)
                    if (account == null) {
                        DisplayUtils.showSnackMessage(this, R.string.account_creation_failed)
                        return@registerForActivityResult
                    }

                    userAccountManager?.setCurrentOwnCloudAccount(account.name)

                    val i = Intent(this, FileDisplayActivity::class.java)
                    i.action = FileDisplayActivity.RESTART
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                    finish()
                }
            }
    }

    private fun setupLoginButton() {
        defaultViewThemeUtils?.material?.colorMaterialButtonFilledOnPrimary(binding.login)
        binding.login.setOnClickListener {
            if (intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                val authenticatorActivityIntent = getAuthenticatorActivityIntent(false)
                activityResult?.launch(authenticatorActivityIntent)
            } else {
                preferences?.onBoardingComplete = true
                finish()
            }
        }
    }

    private fun getAuthenticatorActivityIntent(extraUseProviderAsWebLogin: Boolean): Intent {
        val intent = Intent(this, AuthenticatorActivity::class.java)
        intent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, extraUseProviderAsWebLogin)
        return intent
    }

    // Sometimes, accounts are not deleted when you uninstall the application so we'll do it now
    private fun deleteAccountAtFirstLaunch() {
        if (onboarding?.isFirstRun == true) {
            userAccountManager?.removeAllAccounts()
        }
    }

    private fun updateLoginButtonMargin() {
        if (isLandscapeOrientation()) {
            if (binding.login.layoutParams is MarginLayoutParams) {
                (binding.login.layoutParams as MarginLayoutParams).setMargins(
                    0, 0, 0, resources.getDimensionPixelOffset(
                        R.dimen.login_btn_bottom_margin_land
                    )
                )
                binding.login.requestLayout()
            }
        } else {
            if (binding.login.layoutParams is MarginLayoutParams) {
                (binding.login.layoutParams as MarginLayoutParams).setMargins(
                    0, 0, 0, resources.getDimensionPixelOffset(
                        R.dimen.login_btn_bottom_margin
                    )
                )
                binding.login.requestLayout()
            }
        }
    }

    private fun updateOnBoardingPager(selectedPosition: Int) {
        val featuresViewAdapter = OnBoardingPagerAdapter(this, getOnBoardingItems())
        binding.progressIndicator.setNumberOfSteps(featuresViewAdapter.count)
        binding.contentPanel.adapter = featuresViewAdapter
        binding.contentPanel.currentItem = selectedPosition
        binding.contentPanel.addOnPageChangeListener(this)
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val isFromAddAccount = intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)

                    // NMC Customization -> Modified the condition for readability
                    if (isFromAddAccount) {
                        val destination = Intent(applicationContext, FileDisplayActivity::class.java)
                        destination.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(destination)
                        finish()
                    } else {
                        // NMC Customization -> No redirection to AuthenticatorActivity is required
                        // just close the app
                        finishAffinity()
                    }

                }
            }
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLoginButtonMargin()
        updateOnBoardingPager(selectedPosition)
    }

    private fun onFinish() {
        preferences?.lastSeenVersionCode = BuildConfig.VERSION_CODE
    }

    override fun onStop() {
        onFinish()
        super.onStop()
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // unused but to be implemented due to abstract parent
    }

    override fun onPageSelected(position: Int) {
        //-1 to position because this position doesn't start from 0
        selectedPosition = position - 1

        //pass directly the position here because this position will doesn't start from 0
        binding.progressIndicator.animateToStep(position)
    }

    override fun onPageScrollStateChanged(state: Int) {
        // unused but to be implemented due to abstract parent
    }

    companion object {
        const val EXTRA_ALLOW_CLOSE = "ALLOW_CLOSE"
        const val EXTRA_EXIT = "EXIT"
    }
}
