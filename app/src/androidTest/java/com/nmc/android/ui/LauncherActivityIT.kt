/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nmc.android.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherActivityIT : AbstractIT() {

    @get:Rule
    val activityRule = ActivityScenarioRule(LauncherActivity::class.java)

    @Test
    fun verifyUIElements() {
        onView(withId(R.id.ivSplash)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.splashScreenBold)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.splashScreenNormal)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.splashScreenBold)).check(matches(withText("Magenta")))
        onView(withId(R.id.splashScreenNormal)).check(matches(withText("CLOUD")))
        shortSleep()
    }
}
