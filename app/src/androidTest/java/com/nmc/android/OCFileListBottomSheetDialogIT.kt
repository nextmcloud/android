package com.nmc.android

import android.os.Looper
import androidx.activity.result.contract.ActivityResultContract
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.documentscan.AppScanOptionalFeature
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.Creator
import com.owncloud.android.lib.common.DirectEditing
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.OCFileListBottomSheetActions
import com.owncloud.android.ui.fragment.OCFileListBottomSheetDialog
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.CapabilityUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class OCFileListBottomSheetDialogIT : AbstractIT() {

    @Mock
    private lateinit var actions: OCFileListBottomSheetActions

    @get:Rule
    val activityRule = IntentsTestRule(FileDisplayActivity::class.java, true, true)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun validateCreateTextDocumentMenuOption() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val info = DeviceInfo()
        val ocFile = OCFile("/test.md")

        // add direct editing info
        val creatorMap = mutableMapOf<String, Creator>()
        creatorMap["1"] = Creator(
            "1",
            "md",
            "markdown file",
            ".md",
            MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN,
            false
        )

        val directEditing = DirectEditing(
            creators = creatorMap
        )

        val json = Gson().toJson(directEditing)
        ArbitraryDataProviderImpl(targetContext).storeOrUpdateKeyValue(
            user.accountName,
            ArbitraryDataProvider.DIRECT_EDITING,
            json
        )

        val capability = activityRule.activity.capabilities
        capability.richDocuments = CapabilityBooleanType.TRUE
        capability.richDocumentsDirectEditing = CapabilityBooleanType.TRUE
        capability.richDocumentsTemplatesAvailable = CapabilityBooleanType.TRUE
        capability.accountName = user.accountName
        CapabilityUtils.updateCapability(capability)

        val appScanOptionalFeature: AppScanOptionalFeature = object : AppScanOptionalFeature() {
            override fun getScanContract(): ActivityResultContract<Unit, String?> {
                throw UnsupportedOperationException("Document scan is not available")
            }
        }

        val editorUtils = EditorUtils(ArbitraryDataProviderImpl(targetContext))
        val sut = OCFileListBottomSheetDialog(
            activityRule.activity,
            actions,
            info,
            user,
            ocFile,
            activityRule.activity.themeUtils,
            activityRule.activity.viewThemeUtils,
            editorUtils,
            appScanOptionalFeature
        )

        activityRule.activity.runOnUiThread { sut.show() }

        waitForIdleSync()

        sut.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        shortSleep()

        onView(withText("Create text document")).check(matches(isCompletelyDisplayed()))
    }
}