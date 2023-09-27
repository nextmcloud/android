package com.nmc.android

import android.content.Intent
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContract
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.documentscan.AppScanOptionalFeature
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.OCFileListBottomSheetActions
import com.owncloud.android.ui.fragment.OCFileListBottomSheetDialog
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class ScanbotIT : AbstractIT() {

    @Mock
    private lateinit var actions: OCFileListBottomSheetActions

    @get:Rule
    var activityRule = IntentsTestRule(FileDisplayActivity::class.java, true, false)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun validateScanButton() {
        //Looper to avoid android.util.AndroidRuntimeException: Animators may only be run on Looper threads
        //during running test
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val intent = Intent(targetContext, FileDisplayActivity::class.java)
        val fda = activityRule.launchActivity(intent)
        val info = DeviceInfo()
        val ocFile = OCFile("/test.md")
        val appScanOptionalFeature: AppScanOptionalFeature = object : AppScanOptionalFeature() {
            override fun getScanContract(): ActivityResultContract<Unit, String?> {
                throw UnsupportedOperationException("Document scan is not available")
            }
        }

        val editorUtils = EditorUtils(ArbitraryDataProviderImpl(targetContext))
        val sut = OCFileListBottomSheetDialog(
            fda,
            actions,
            info,
            user,
            ocFile,
            fda.themeUtils,
            activityRule.activity.viewThemeUtils,
            editorUtils,
            appScanOptionalFeature
        )

        fda.runOnUiThread { sut.show() }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        shortSleep()

        sut.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        //validate nmc scan button visibility & clickable
        onView(withId(R.id.menu_scan_document)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.menu_scan_document)).check(matches(isClickable()))

        //validate nc scan button hidden
        onView(withId(R.id.menu_scan_doc_upload)).check(matches(not(isDisplayed())))

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        shortSleep()
    }
}