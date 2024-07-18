package com.nmc.android.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacySettingsActivityIT : AbstractIT() {

    private fun getIntent(showSettingsButton: Boolean): Intent =
        Intent(targetContext, PrivacySettingsActivity::class.java)
            .putExtra("show_settings_button", showSettingsButton)

    lateinit var activityRule: ActivityScenario<PrivacySettingsActivity>

    @Before
    fun setUp() {
        activityRule = launchActivity(getIntent(false))
    }

    @Test
    fun verifyUIElements() {
        onView(withId(R.id.tv_privacy_intro_text)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.switch_data_collection)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.switch_data_collection)).check(matches(not(isEnabled())))
        onView(withId(R.id.switch_data_collection)).check(matches(isChecked()))

        onView(withId(R.id.switch_data_analysis)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.switch_data_analysis)).check(matches(isEnabled()))
        //by-default the analysis switch will be checked as per #AppPreferences.isDataAnalysisEnabled will return true
        onView(withId(R.id.switch_data_analysis)).check(matches(isChecked()))
        onView(withId(R.id.switch_data_analysis)).check(matches(isClickable()))

        onView(withId(R.id.privacy_save_settings_btn)).check(matches(not(isDisplayed())))
    }

    @Test
    fun verifyDataCollectionSwitchToggle() {
        //since this button is disabled performing click operation should do nothing
        //and switch will be in checked state only
        onView(withId(R.id.switch_data_collection)).perform(click())
        onView(withId(R.id.switch_data_collection)).check(matches(isChecked()))

        onView(withId(R.id.switch_data_collection)).perform(click())
        onView(withId(R.id.switch_data_collection)).check(matches(isChecked()))
    }

    @Test
    fun verifyDataAnalysisSwitchToggle() {
        onView(withId(R.id.switch_data_analysis)).perform(click())
        onView(withId(R.id.switch_data_analysis)).check(matches(not(isChecked())))

        onView(withId(R.id.switch_data_analysis)).perform(click())
        onView(withId(R.id.switch_data_analysis)).check(matches(isChecked()))
    }

    @Test
    fun verifySaveSettingsButton() {
        //button not shown on the basis of extras passed to intent
        onView(withId(R.id.privacy_save_settings_btn)).check(matches(not(isDisplayed())))
        //close the activity already open
        activityRule.close()

        //launch activity with extras as true
        activityRule = launchActivity(getIntent(true))
        //button will be shown if extras is true
        onView(withId(R.id.privacy_save_settings_btn)).check(matches(isDisplayed()))
    }

    @After
    fun tearDown() {
        activityRule.close()
    }
}