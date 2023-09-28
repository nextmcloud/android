/*
 * Nextcloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Chris Narkiewicz
 * Copyright (C) 2015 Bartosz Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2016 Nextcloud.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.onboarding;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;

import com.nextcloud.android.common.ui.theme.utils.ColorRole;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.appinfo.AppInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.nmc.android.helper.OnBoardingUtils;
import com.nmc.android.helper.OnBoardingPagerAdapter;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.databinding.FirstRunActivityBinding;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

/**
 * Activity displaying general feature after a fresh install.
 */
public class FirstRunActivity extends BaseActivity implements ViewPager.OnPageChangeListener, Injectable {

    public static final String EXTRA_ALLOW_CLOSE = "ALLOW_CLOSE";
    public static final String EXTRA_EXIT = "EXIT";
    public static final int FIRST_RUN_RESULT_CODE = 199;

    @Inject UserAccountManager userAccountManager;
    @Inject AppPreferences preferences;
    @Inject AppInfo appInfo;
    @Inject OnboardingService onboarding;

    @Inject ViewThemeUtils.Factory viewThemeUtilsFactory;

    private FirstRunActivityBinding binding;
    private ViewThemeUtils defaultViewThemeUtils;
    private int selectedPosition = 0;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        enableAccountHandling = false;

        super.onCreate(savedInstanceState);
        defaultViewThemeUtils = viewThemeUtilsFactory.withPrimaryAsBackground();
        defaultViewThemeUtils.platform.themeStatusBar(this, ColorRole.PRIMARY);

        //if device is not tablet then we have to lock it to Portrait mode
        //as we don't have images for that
        if (!com.nmc.android.utils.DisplayUtils.isTablet()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        this.binding = FirstRunActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        defaultViewThemeUtils.material.colorMaterialButtonFilledOnPrimary(binding.login);
        binding.login.setOnClickListener(v -> {
            if (getIntent().getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                Intent authenticatorActivityIntent = new Intent(this, AuthenticatorActivity.class);
                authenticatorActivityIntent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, false);
                startActivityForResult(authenticatorActivityIntent, FIRST_RUN_RESULT_CODE);
            } else {
                preferences.setOnBoardingComplete(true);
                finish();
            }
        });

        // Sometimes, accounts are not deleted when you uninstall the application so we'll do it now
        if (onboarding.isFirstRun()) {
            userAccountManager.removeAllAccounts();
        }

        updateLoginButtonMargin();
        updateOnBoardingPager(selectedPosition);

        binding.contentPanel.addOnPageChangeListener(this);
    }

    private void updateOnBoardingPager(int selectedPosition) {
        OnBoardingPagerAdapter featuresViewAdapter = new OnBoardingPagerAdapter(this, OnBoardingUtils.Companion.getOnBoardingItems());
        binding.progressIndicator.setNumberOfSteps(featuresViewAdapter.getCount());
        binding.contentPanel.setAdapter(featuresViewAdapter);
        binding.contentPanel.setCurrentItem(selectedPosition);
    }

    private void updateLoginButtonMargin() {
        if (com.nmc.android.utils.DisplayUtils.isLandscapeOrientation()) {
            if (binding.login.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) binding.login.getLayoutParams()).setMargins(
                    0, 0, 0, getResources().getDimensionPixelOffset(
                        R.dimen.login_btn_bottom_margin_land));
                binding.login.requestLayout();
            }
        } else {
            if (binding.login.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) binding.login.getLayoutParams()).setMargins(
                    0, 0, 0, getResources().getDimensionPixelOffset(
                        R.dimen.login_btn_bottom_margin));
                binding.login.requestLayout();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLoginButtonMargin();
        updateOnBoardingPager(selectedPosition);
    }

    @Override
    public void onBackPressed() {
        onFinish();

        if (getIntent().getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
            super.onBackPressed();
        } else {
            finishAffinity();
        }
    }

    private void onFinish() {
        preferences.setLastSeenVersionCode(BuildConfig.VERSION_CODE);
    }

    @Override
    protected void onStop() {
        onFinish();

        super.onStop();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // unused but to be implemented due to abstract parent
    }

    @Override
    public void onPageSelected(int position) {
        //-1 to position because this position doesn't start from 0
        selectedPosition = position - 1;

        //pass directly the position here because this position will doesn't start from 0
        binding.progressIndicator.animateToStep(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // unused but to be implemented due to abstract parent
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FIRST_RUN_RESULT_CODE == requestCode && RESULT_OK == resultCode) {

            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            Account account = userAccountManager.getAccountByName(accountName);


            if (account == null) {
                DisplayUtils.showSnackMessage(this, R.string.account_creation_failed);
                return;
            }

            userAccountManager.setCurrentOwnCloudAccount(account.name);

            Intent i = new Intent(this, FileDisplayActivity.class);
            i.setAction(FileDisplayActivity.RESTART);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);

            finish();
        }
    }
}
