/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nmc.android

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.R
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Test class to verify the strings, dimens and bool customized in this branch PR for NMC
 */
@RunWith(AndroidJUnit4::class)
class GeneralThemingResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.camera_permission_rationale to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Please navigate to App info in settings and give permission manually.",
                Locale.GERMAN to "Bitte geben Sie unter Apps & Benachrichtigungen in den Einstellungen manuell die Erlaubnis."
            )
        ),
    )

    @Test
    fun verifyLocalizedStrings() {
        localizedStringMap.forEach { (stringRes, expected) ->
            expected.translations.forEach { (locale, expectedText) ->

                val config = Configuration(baseContext.resources.configuration)
                config.setLocale(locale)

                val localizedContext = baseContext.createConfigurationContext(config)
                val actualText = localizedContext.getString(stringRes)

                assertEquals(
                    "Mismatch for ${baseContext.resources.getResourceEntryName(stringRes)} in $locale",
                    expectedText,
                    actualText
                )
            }
        }
    }

    data class ExpectedLocalizedString(val translations: Map<Locale, String>)

    private val expectedDimenMap = mapOf(
        R.dimen.grid_recyclerview_padding to ExpectedDimen(
            default = 4f,
            unit = DimenUnit.DP
        ),
        R.dimen.list_item_icons_size to ExpectedDimen(
            default = 16f,
            unit = DimenUnit.DP
        ),
        R.dimen.grid_item_icons_size to ExpectedDimen(
            default = 24f,
            unit = DimenUnit.DP
        ),
        R.dimen.grid_item_shared_icon_layout_top_margin to ExpectedDimen(
            default = 24f,
            unit = DimenUnit.DP
        ),
        R.dimen.media_grid_item_rv_spacing to ExpectedDimen(
            default = 6f,
            unit = DimenUnit.DP
        ),
        R.dimen.txt_size_18sp to ExpectedDimen(
            default = 18f,
            unit = DimenUnit.SP
        ),
        R.dimen.txt_size_15sp to ExpectedDimen(
            default = 15f,
            unit = DimenUnit.SP
        ),
        R.dimen.crop_corner_size to ExpectedDimen(
            default = 15f,
            unit = DimenUnit.DP
        ),
        R.dimen.edit_scan_bottom_bar_height to ExpectedDimen(
            default = 56f,
            unit = DimenUnit.DP
        ),
        R.dimen.standard_folders_grid_item_size to ExpectedDimen(
            default = 86f,
            unit = DimenUnit.DP
        ),
        R.dimen.standard_files_grid_item_size to ExpectedDimen(
            default = 80f,
            unit = DimenUnit.DP
        ),
        R.dimen.txt_size_11sp to ExpectedDimen(
            default = 11f,
            unit = DimenUnit.SP
        ),
        R.dimen.share_row_icon_size to ExpectedDimen(
            default = 30f,
            unit = DimenUnit.DP
        ),
        R.dimen.create_link_button_height to ExpectedDimen(
            default = 55f,
            unit = DimenUnit.DP
        ),
        R.dimen.note_et_height to ExpectedDimen(
            default = 258f,
            unit = DimenUnit.DP
        ),
        R.dimen.txt_size_17sp to ExpectedDimen(
            default = 17f,
            unit = DimenUnit.SP
        ),
        R.dimen.share_exp_date_divider_margin to ExpectedDimen(
            default = 20f,
            unit = DimenUnit.DP
        ),
        R.dimen.privacy_btn_width to ExpectedDimen(
            default = 160f,
            unit = DimenUnit.DP
        ),
        R.dimen.privacy_icon_size to ExpectedDimen(
            default = 50f,
            unit = DimenUnit.DP
        ),
        R.dimen.login_btn_width to ExpectedDimen(
            default = 150f,
            unit = DimenUnit.DP
        ),
        R.dimen.login_btn_height to ExpectedDimen(
            default = 55f,
            unit = DimenUnit.DP
        ),
        R.dimen.login_btn_bottom_margin to ExpectedDimen(
            default = 48f,
            unit = DimenUnit.DP
        ),
        R.dimen.login_btn_bottom_margin_land to ExpectedDimen(
            default = 48f,
            unit = DimenUnit.DP
        ),
        R.dimen.login_btn_bottom_margin_small_screen to ExpectedDimen(
            default = 24f,
            unit = DimenUnit.DP
        ),
        R.dimen.shared_with_me_icon_size to ExpectedDimen(
            default = 26f,
            unit = DimenUnit.DP
        ),
        R.dimen.txt_size_20sp to ExpectedDimen(
            default = 20f,
            unit = DimenUnit.SP
        ),
        R.dimen.notification_row_item_height to ExpectedDimen(
            default = 145f,
            unit = DimenUnit.DP
        ),
        R.dimen.button_stroke_width to ExpectedDimen(
            default = 1f,
            unit = DimenUnit.DP
        ),
        R.dimen.txt_size_13sp to ExpectedDimen(
            default = 13f,
            unit = DimenUnit.SP
        ),
        R.dimen.file_icon_rounded_corner_radius_for_grid_mode to ExpectedDimen(
            default = 4f,
            unit = DimenUnit.DP,
            alt = 16f
        ),
        R.dimen.file_icon_rounded_corner_radius to ExpectedDimen(
            default = 8f,
            unit = DimenUnit.DP,
            alt = 32f
        ),
        R.dimen.grid_item_text_size to ExpectedDimen(
            default = 14f,
            unit = DimenUnit.SP
        ),
        R.dimen.grid_item_local_file_indicator_layout_width to ExpectedDimen(
            default = 24f,
            unit = DimenUnit.DP
        ),
        R.dimen.grid_item_local_file_indicator_layout_height to ExpectedDimen(
            default = 24f,
            unit = DimenUnit.DP
        ),
        R.dimen.list_item_local_file_indicator_layout_width to ExpectedDimen(
            default = 24f,
            unit = DimenUnit.DP
        ),
        R.dimen.list_item_local_file_indicator_layout_height to ExpectedDimen(
            default = 24f,
            unit = DimenUnit.DP
        ),
    )

    @Test
    fun validateDefaultDimens() {
        validateDimens(
            configModifier = { it }, // no change → default values
        ) { it.default to it.unit }
    }

    @Test
    fun validate_sw600dp_Dimens() {
        validateDimens(configModifier = { config ->
            config.smallestScreenWidthDp = 600
            config
        }) { it.alt to it.unit }
    }

    private fun validateDimens(
        configModifier: (Configuration) -> Configuration,
        selector: (ExpectedDimen) -> Pair<Float?, DimenUnit>
    ) {
        val baseConfig = Configuration(baseContext.resources.configuration)
        val testConfig = configModifier(baseConfig)
        val testContext = baseContext.createConfigurationContext(testConfig)
        val dm = testContext.resources.displayMetrics
        val config = testContext.resources.configuration
        expectedDimenMap.forEach { (resId, entry) ->
            val (value, unit) = selector(entry)
            val actualPx = testContext.resources.getDimension(resId)
            value?.let {
                val expectedPx = convertToPx(value, unit, dm, config)
                assertEquals(
                    "Mismatch for ${testContext.resources.getResourceEntryName(resId)} ($unit)",
                    expectedPx,
                    actualPx,
                    0.01f
                )
            }
        }
    }

    private fun convertToPx(
        value: Float,
        unit: DimenUnit,
        dm: DisplayMetrics,
        config: Configuration
    ): Float {
        return when (unit) {
            DimenUnit.DP -> value * dm.density
            DimenUnit.SP -> value * dm.density * config.fontScale
            DimenUnit.PX -> value
        }
    }

    data class ExpectedDimen(
        val default: Float,
        val alt: Float? = null,
        val unit: DimenUnit,
    )

    enum class DimenUnit { DP, SP, PX }

    @Test
    fun assertShowCalendarBackupBooleanFalse() {
        val actualValue = baseContext.resources.getBoolean(R.bool.show_calendar_backup)
        assertTrue(!actualValue)
    }
}
