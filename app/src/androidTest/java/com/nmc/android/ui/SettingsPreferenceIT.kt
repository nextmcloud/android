package com.nmc.android.ui

import android.preference.ListPreference
import android.preference.Preference
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.PreferenceMatchers
import androidx.test.espresso.matcher.PreferenceMatchers.withKey
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.ui.AppVersionPreference
import com.owncloud.android.ui.PreferenceCustomCategory
import com.owncloud.android.ui.ThemeableSwitchPreference
import com.owncloud.android.ui.activity.SettingsActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsPreferenceIT : AbstractIT() {

    @get:Rule
    val activityRule = ActivityScenarioRule(SettingsActivity::class.java)

    @Test
    fun verifyPreferenceSectionCustomClass() {
        activityRule.scenario.onActivity {
            val preferenceAccountInfo = it.findPreference("account_info")
            val preferenceGeneral = it.findPreference("general")
            val preferenceDetails = it.findPreference("details")
            val preferenceMore = it.findPreference("more")
            val preferenceDataProtection = it.findPreference("data_protection")
            val preferenceInfo = it.findPreference("info")

            val preferenceCategoryList = listOf(
                preferenceAccountInfo,
                preferenceGeneral,
                preferenceDetails,
                preferenceMore,
                preferenceDataProtection,
                preferenceInfo
            )

            for (preference in preferenceCategoryList) {
                assertEquals(PreferenceCustomCategory::class.java, preference.javaClass)
            }
        }
    }

    @Test
    fun verifySwitchPreferenceCustomClass() {
        activityRule.scenario.onActivity {
            val preferenceShowHiddenFiles = it.findPreference("show_hidden_files")
            assertEquals(ThemeableSwitchPreference::class.java, preferenceShowHiddenFiles.javaClass)
        }
    }

    @Test
    fun verifyAppVersionPreferenceCustomClass() {
        activityRule.scenario.onActivity {
            val preferenceAboutApp = it.findPreference("about_app")
            assertEquals(AppVersionPreference::class.java, preferenceAboutApp.javaClass)
        }
    }

    @Test
    fun verifyPreferenceChildCustomLayout() {
        activityRule.scenario.onActivity {
            val userName = it.findPreference("user_name")
            val storagePath = it.findPreference("storage_path")
            val lock = it.findPreference("lock")
            val showHiddenFiles = it.findPreference("show_hidden_files")
            val syncedFolders = it.findPreference("syncedFolders")
            val backup = it.findPreference("backup")
            val mnemonic = it.findPreference("mnemonic")
            val privacySettings = it.findPreference("privacy_settings")
            val privacyPolicy = it.findPreference("privacy_policy")
            val sourceCode = it.findPreference("sourcecode")
            val help = it.findPreference("help")
            val imprint = it.findPreference("imprint")

            val preferenceList = listOf(
                userName,
                storagePath,
                lock,
                showHiddenFiles,
                syncedFolders,
                backup,
                mnemonic,
                privacySettings,
                privacyPolicy,
                sourceCode,
                help,
                imprint
            )

            for (preference in preferenceList) {
                assertEquals(R.layout.custom_preference_layout, preference.layoutResource)
            }

            val aboutApp = it.findPreference("about_app")
            assertEquals(R.layout.custom_app_preference_layout, aboutApp.layoutResource)

        }
    }

    @Test
    fun verifyPreferencesTitleText() {
        onData(allOf(`is`(instanceOf(PreferenceCustomCategory::class.java)), withKey("account_info"),
                PreferenceMatchers.withTitleText("Account Information")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("user_name"),
            PreferenceMatchers.withTitleText("test")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(PreferenceCustomCategory::class.java)), withKey("general"),
            PreferenceMatchers.withTitleText("General")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(ListPreference::class.java)), withKey("storage_path"),
            PreferenceMatchers.withTitleText("Data storage folder")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(PreferenceCustomCategory::class.java)), withKey("details"),
            PreferenceMatchers.withTitleText("Details")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(ListPreference::class.java)), withKey("lock"),
            PreferenceMatchers.withTitleText("App passcode")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(ThemeableSwitchPreference::class.java)), withKey("show_hidden_files"),
            PreferenceMatchers.withTitleText("Show hidden files")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(PreferenceCustomCategory::class.java)), withKey("more"),
            PreferenceMatchers.withTitleText("More")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("syncedFolders"),
            PreferenceMatchers.withTitleText("Auto upload")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("backup"),
            PreferenceMatchers.withTitleText("Back up contacts")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("mnemonic"),
            PreferenceMatchers.withTitleText("E2E mnemonic")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("logger"),
            PreferenceMatchers.withTitleText("Logs")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(PreferenceCustomCategory::class.java)), withKey("data_protection"),
            PreferenceMatchers.withTitleText("Data Privacy")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("privacy_settings"),
            PreferenceMatchers.withTitleText("Privacy Settings")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("privacy_policy"),
            PreferenceMatchers.withTitleText("Privacy Policy")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("sourcecode"),
            PreferenceMatchers.withTitleText("Used OpenSource Software")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(PreferenceCustomCategory::class.java)), withKey("service"),
            PreferenceMatchers.withTitleText("Service")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("help"),
            PreferenceMatchers.withTitleText("Help")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("imprint"),
            PreferenceMatchers.withTitleText("Imprint")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(PreferenceCustomCategory::class.java)), withKey("info"),
            PreferenceMatchers.withTitleText("Info")))
            .check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun verifyPreferencesSummaryText() {
        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("lock"),
            PreferenceMatchers.withSummaryText("None")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("syncedFolders"),
            PreferenceMatchers.withSummaryText("Manage folders for auto upload")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("backup"),
            PreferenceMatchers.withSummaryText("Daily backup of your calendar & contacts")))
            .check(matches(isCompletelyDisplayed()))

        onData(allOf(`is`(instanceOf(Preference::class.java)), withKey("mnemonic"),
            PreferenceMatchers.withSummaryText("To show mnemonic please enable device credentials.")))
            .check(matches(isCompletelyDisplayed()))
    }
}