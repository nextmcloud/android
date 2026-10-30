package com.nmc.android.scans

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import junit.framework.TestCase
import org.hamcrest.core.IsNot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
/*
 *Scan test to test the full flow of document scan from Scanning to Save page.
 */
class ScanActivityTest : AbstractIT() {
    @get:Rule
    val activityRule = ActivityScenarioRule(ScanActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private var docScanCount = 0

    /*
     * running all test in one test will create a flow from scanning to saving the scans
     */
    @Test
    fun runAllScanTests() {
        verifyIfToolbarHidden()
        verifyIfScanFragmentReplaced()
        verifyToggleAutomatic()
        verifyToggleFlash()
        captureAndVerifyDocScan()
        verifyScanMoreDocument()
        verifyApplyFilter()
        verifyRotateDocument()
        verifyImageCrop()
        verifyImageDeletion()
        verifySaveScannedDocs()
        verifyPasswordSwitch()
        verifyPdfPasswordSwitchToggle()
    }

    private fun verifyIfToolbarHidden() {
        Espresso.onView(ViewMatchers.withId(R.id.toolbar))
            .check(ViewAssertions.matches(IsNot.not(ViewMatchers.isDisplayed())))
    }

    private fun verifyIfScanFragmentReplaced() {
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_automatic))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_flash))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_cancel))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.shutterButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun verifyToggleAutomatic() {
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_automatic)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_automatic)).check(
            ViewAssertions.matches(
                ViewMatchers.hasTextColor(
                    R.color.grey_60
                )
            )
        )

        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_automatic)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_automatic)).check(
            ViewAssertions.matches(
                ViewMatchers.hasTextColor(
                    R.color.primary
                )
            )
        )
    }

    private fun verifyToggleFlash() {
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_flash)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_flash)).check(
            ViewAssertions.matches(
                ViewMatchers.hasTextColor(
                    R.color.primary
                )
            )
        )

        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_flash)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.scan_doc_btn_flash)).check(
            ViewAssertions.matches(
                ViewMatchers.hasTextColor(
                    R.color.grey_60
                )
            )
        )
    }

    private fun captureAndVerifyDocScan() {
        Espresso.onView(ViewMatchers.withId(R.id.shutterButton)).perform(ViewActions.click())
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

    private fun verifyApplyFilter() {
        Espresso.onView(ViewMatchers.withId(R.id.filterDocButton)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText(R.string.edit_scan_filter_dialog_title))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText(R.string.edit_scan_filter_b_n_w))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.click())

        shortSleep()
        shortSleep()
        shortSleep()
    }

    private fun verifyRotateDocument() {
        Espresso.onView(ViewMatchers.withId(R.id.rotateDocButton)).perform(ViewActions.click())
    }

    private fun verifyImageCrop() {
        Espresso.onView(ViewMatchers.withId(R.id.cropDocButton)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.crop_polygon_view))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.crop_btn_reset_borders))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withId(R.id.action_save)).perform(ViewActions.click())
    }

    private fun verifyImageDeletion() {
        Espresso.onView(ViewMatchers.withId(R.id.deleteDocButton)).perform(ViewActions.click())
        docScanCount--
        TestCase.assertEquals(docScanCount, ScanActivity.originalScannedImages.size)
    }

    private fun verifySaveScannedDocs() {
        Espresso.onView(ViewMatchers.withId(R.id.action_save)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.scan_save_filename_input))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_location_input))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_nested_scroll_view)).perform(ViewActions.swipeUp())

        Espresso.onView(ViewMatchers.withId(R.id.scan_save_without_txt_recognition_pdf_checkbox))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_without_txt_recognition_png_checkbox))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_without_txt_recognition_jpg_checkbox))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_with_txt_recognition_pdf_checkbox))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_with_txt_recognition_txt_checkbox))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withId(R.id.scan_save_without_txt_recognition_pdf_checkbox)).check(
            ViewAssertions.matches(
                IsNot.not(ViewMatchers.isChecked())
            )
        )
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_without_txt_recognition_png_checkbox)).check(
            ViewAssertions.matches(
                IsNot.not(ViewMatchers.isChecked())
            )
        )
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_without_txt_recognition_jpg_checkbox)).check(
            ViewAssertions.matches(
                IsNot.not(ViewMatchers.isChecked())
            )
        )
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_with_txt_recognition_pdf_checkbox))
            .check(ViewAssertions.matches(ViewMatchers.isChecked()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_with_txt_recognition_txt_checkbox)).check(
            ViewAssertions.matches(
                IsNot.not(ViewMatchers.isChecked())
            )
        )

        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch))
            .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch))
            .check(ViewAssertions.matches(IsNot.not(ViewMatchers.isChecked())))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_text_input))
            .check(ViewAssertions.matches(IsNot.not(ViewMatchers.isDisplayed())))

        Espresso.onView(ViewMatchers.withId(R.id.save_scan_btn_cancel))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.save_scan_btn_save))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun verifyPasswordSwitch() {
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_with_txt_recognition_pdf_checkbox))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch))
            .check(ViewAssertions.matches(IsNot.not(ViewMatchers.isEnabled())))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch))
            .check(ViewAssertions.matches(IsNot.not(ViewMatchers.isChecked())))

        Espresso.onView(ViewMatchers.withId(R.id.scan_save_without_txt_recognition_pdf_checkbox))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch))
            .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch))
            .check(ViewAssertions.matches(IsNot.not(ViewMatchers.isChecked())))
    }

    private fun verifyPdfPasswordSwitchToggle() {
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_text_input))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_switch)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.scan_save_pdf_password_text_input))
            .check(ViewAssertions.matches(IsNot.not(ViewMatchers.isDisplayed())))
    }
}
