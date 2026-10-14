package com.nmc.android.onboarding

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.onboarding.FirstRunActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnBoardingIT : AbstractIT() {

    @get:Rule
    var activityRule = ActivityScenarioRule(FirstRunActivity::class.java)

    @Test
    fun runAllOnboardingTests() {
        verifyUIElements()

        shortSleep()

        verifyOnBoardingSwipe()
    }

    private fun verifyUIElements() {
        onView(withId(R.id.contentPanel)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.progressIndicator)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.login)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.login)).check(matches(isClickable()))
    }

    private fun verifyOnBoardingSwipe() {
        onView(withId(R.id.contentPanel)).perform(swipeLeft())
        onView(withId(R.id.contentPanel)).perform(swipeLeft())
        onView(withId(R.id.contentPanel)).perform(swipeLeft())

        onView(withId(R.id.contentPanel)).perform(swipeRight())
        onView(withId(R.id.contentPanel)).perform(swipeRight())
    }
}