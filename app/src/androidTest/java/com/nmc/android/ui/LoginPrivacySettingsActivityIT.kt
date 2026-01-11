package com.nmc.android.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nmc.android.ui.ClickableSpanTestHelper.getClickableSpan
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginPrivacySettingsActivityIT : AbstractIT() {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginPrivacySettingsActivity::class.java)

    @Test
    fun verifyNothingHappensOnBackPress() {
        pressBack()
        shortSleep()

        //check any one view to check the activity is not destroyed
        onView(withId(R.id.tv_privacy_setting_title)).check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun verifyUIElements() {
        onView(withId(R.id.ic_privacy)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.tv_privacy_setting_title)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.tv_login_privacy_intro_text)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.privacy_accept_btn)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.privacy_accept_btn)).check(matches(isClickable()))
    }

    @Test
    fun verifyAcceptButtonRedirection() {
        Intents.init()
        onView(withId(R.id.privacy_accept_btn)).perform(click())

        //check if the policy action saved correct  --> 2 for Accept action
        assertEquals(2, AppPreferencesImpl.fromContext(targetContext).privacyPolicyAction)

        intended(hasComponent(FileDisplayActivity::class.java.canonicalName))
        Intents.release()
    }

    @Test
    fun verifySettingsTextClick() {
        Intents.init()
        val settingsClickableSpan = getClickableSpan("Settings", onView(withId(R.id.tv_login_privacy_intro_text)))
        onView(withId(R.id.tv_login_privacy_intro_text)).perform(
            ClickableSpanTestHelper.performClickSpan(
                settingsClickableSpan
            )
        )
        intended(hasComponent(PrivacySettingsActivity::class.java.canonicalName))
        Intents.release()
    }

    @Test
    fun verifyPrivacyPolicyTextClick() {
        Intents.init()
        val privacyPolicyClickableSpan =
            getClickableSpan("Privacy Policy", onView(withId(R.id.tv_login_privacy_intro_text)))
        onView(withId(R.id.tv_login_privacy_intro_text)).perform(
            ClickableSpanTestHelper.performClickSpan(
                privacyPolicyClickableSpan
            )
        )
        intended(hasComponent(ExternalSiteWebView::class.java.canonicalName))
        Intents.release()
    }

    @Test
    fun verifyRejectTextClick() {
        Intents.init()
        val rejectClickableSpan =
            getClickableSpan("reject", onView(withId(R.id.tv_login_privacy_intro_text)))
        onView(withId(R.id.tv_login_privacy_intro_text)).perform(
            ClickableSpanTestHelper.performClickSpan(
                rejectClickableSpan
            )
        )

        //check if the policy action saved correct  --> 1 for Reject action
        assertEquals(1, AppPreferencesImpl.fromContext(targetContext).privacyPolicyAction)

        intended(hasComponent(FileDisplayActivity::class.java.canonicalName))
        Intents.release()
    }
}