package com.nmc.android.scans

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
/*
 * Scan test to test the max number of possible scans till device throws exception or unexpected error occurs
 */
class ScanActivityMultipleTest : AbstractIT() {
    @get:Rule
    val activityRule = ActivityScenarioRule(ScanActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private var docScanCount = 0

    @Test
    fun runAllScanTests() {
        captureAndVerifyDocScan()
        for (i in 0 until MAX_NUMBER_OF_SCAN) {
            println("Scan no: $docScanCount")
            verifyScanMoreDocument()
        }
    }

    private fun captureAndVerifyDocScan() {
        Espresso.onView(ViewMatchers.withId(R.id.shutterButton)).perform(ViewActions.click())
        shortSleep()
        shortSleep()
        shortSleep()
        shortSleep()
        docScanCount++
        TestCase.assertEquals(docScanCount, ScanActivity.originalScannedImages.size)
    }

    private fun verifyScanMoreDocument() {
        Espresso.onView(ViewMatchers.withId(R.id.scanMoreButton)).perform(ViewActions.click())
        captureAndVerifyDocScan()
    }

    companion object {
        /**
         * variable to define max number of scans to test
         */
        private const val MAX_NUMBER_OF_SCAN = 40
    }
}
