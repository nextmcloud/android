/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nmc.android

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.R
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class to verify the colors customized in this branch PR for NMC
 */
@RunWith(AndroidJUnit4::class)
class AppColorTest {
    private val expectedColorMap = mapOf(
        R.color.list_item_lastmod_and_filesize_text to ExpectedColor(
            light = "#666666",
            dark = "#B2B2B2"
        ),
        R.color.text_color to ExpectedColor(
            light = "#191919",
            dark = "#E3E3E3"
        ),
        R.color.light_grey to ExpectedColor(
            light = "#F5F5F5",
            dark = "#F5F5F5" // same for light and dark
        ),
        R.color.secondary_text_color to ExpectedColor(
            light = "#B2B2B2",
            dark = "#A5A5A5"
        ),
        R.color.action_mode_background to ExpectedColor(
            light = "#FFFFFF",
            dark = "#121212"
        ),
        R.color.selected_item_background to ExpectedColor(
            light = "#F2F2F2",
            dark = "#4C4C4C"
        ),
        R.color.drawer_active_item_background to ExpectedColor(
            light = "#919191",
            dark = "#FFFFFF"
        ),
        R.color.fontAppbar to ExpectedColor(
            light = "#191919",
            dark = "#FFFFFF"
        ),
        R.color.icon_color to ExpectedColor(
            light = "#191919",
            dark = "#FFFFFF"
        ),
        R.color.sort_text_color to ExpectedColor(
            light = "#E20074", // primary magenta color
            dark = "#B2B2B2"
        ),
        R.color.list_icon_color to ExpectedColor(
            light = "#191919",
            dark = "#B2B2B2"
        ),
        R.color.warning_icon_color to ExpectedColor(
            light = "#191919",
            dark = "#CCCCCC"
        ),
        R.color.divider_color to ExpectedColor(
            light = "#B2B2B2",
            dark = "#4C4C4C"
        ),
        R.color.spinner_bg_color to ExpectedColor(
            light = "#FFFFFF",
            dark = "#333333"
        ),
        R.color.refresh_layout_bg_color to ExpectedColor(
            light = "#FFFFFF",
            dark = "#2D2D2D"
        ),
        R.color.primary_button_disabled_color to ExpectedColor(
            light = "#F2F2F2",
            dark = "#4C4C4C"
        ),
        R.color.toolbar_divider_color to ExpectedColor(
            light = "#CCCCCC",
            dark = "#4C4C4C"
        ),
        R.color.et_highlight_color to ExpectedColor(
            light = "#77c4ff",
            dark = "#77c4ff" // same for light and dark
        ),
        R.color.white_trans_70 to ExpectedColor(
            light = "#B3FFFFFF",
            dark = "#B3FFFFFF" // same for light and dark
        ),
        R.color.progress_bar_background to ExpectedColor(
            light = "#E5E5E5",
            dark = "#E5E5E5" // same for light and dark
        ),
        R.color.dark_grey to ExpectedColor(
            light = "#101010",
            dark = "#101010" // same for light and dark
        ),
        R.color.grey_0 to ExpectedColor(
            light = "#F2F2F2",
            dark = "#F2F2F2" // same for light and dark
        ),
        R.color.grey_10 to ExpectedColor(
            light = "#E5E5E5",
            dark = "#E5E5E5" // same for light and dark
        ),
        R.color.grey_30 to ExpectedColor(
            light = "#B2B2B2",
            dark = "#B2B2B2" // same for light and dark
        ),
        R.color.grey_60 to ExpectedColor(
            light = "#666666",
            dark = "#666666" // same for light and dark
        ),
        R.color.grey_70 to ExpectedColor(
            light = "#4C4C4C",
            dark = "#4C4C4C" // same for light and dark
        ),
        R.color.grey_80 to ExpectedColor(
            light = "#333333",
            dark = "#333333" // same for light and dark
        ),
        R.color.snackbar_bg_color to ExpectedColor(
            light = "#323232",
            dark = "#333333"
        ),
        R.color.snackbar_txt_color to ExpectedColor(
            light = "#FFFFFF",
            dark = "#F2F2F2"
        ),
        R.color.alert_bg_color to ExpectedColor(
            light = "#FFFFFF",
            dark = "#333333"
        ),
        R.color.alert_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.nav_selected_bg_color to ExpectedColor(
            light = "#F2F2F2",
            dark = "#666666"
        ),
        R.color.nav_txt_unselected_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.nav_txt_selected_color to ExpectedColor(
            light = "#E20074", // primary magenta color
            dark = "#F2F2F2"
        ),
        R.color.nav_icon_unselected_color to ExpectedColor(
            light = "#191919",
            dark = "#B2B2B2"
        ),
        R.color.nav_icon_selected_color to ExpectedColor(
            light = "#E20074", // primary magenta color
            dark = "#FFFFFF"
        ),
        R.color.nav_divider_color to ExpectedColor(
            light = "#B2B2B2",
            dark = "#B2B2B2" // same for light and dark
        ),
        R.color.nav_bg_color to ExpectedColor(
            light = "#FFFFFF",
            dark = "#333333"
        ),
        R.color.drawer_quota_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#FFFFFF"
        ),
        R.color.bottom_sheet_bg_color to ExpectedColor(
            light = "#FFFFFF",
            dark = "#333333"
        ),
        R.color.bottom_sheet_icon_color to ExpectedColor(
            light = "#191919",
            dark = "#B2B2B2"
        ),
        R.color.bottom_sheet_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.popup_menu_bg to ExpectedColor(
            light = "#FFFFFF",
            dark = "#333333"
        ),
        R.color.popup_menu_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.overflow_bg_color to ExpectedColor(
            light = "#FFFFFF",
            dark = "#333333"
        ),
        R.color.switch_thumb_checked_enabled to ExpectedColor(
            light = "#E20074", // primary magenta color
            dark = "#E20074", // same for light and dark
        ),
        R.color.switch_track_checked_enabled to ExpectedColor(
            light = "#F399C7",
            dark = "#F399C7" // same for light and dark
        ),
        R.color.switch_thumb_unchecked_enabled to ExpectedColor(
            light = "#FFFFFF",
            dark = "#FFFFFF" // same for light and dark
        ),
        R.color.switch_track_unchecked_enabled to ExpectedColor(
            light = "#B2B2B2",
            dark = "#B2B2B2" // same for light and dark
        ),
        R.color.switch_thumb_disabled to ExpectedColor(
            light = "#E5E5E5",
            dark = "#4C4C4C"
        ),
        R.color.switch_track_disabled to ExpectedColor(
            light = "#F2F2F2",
            dark = "#666666"
        ),
        R.color.checkbox_checked_enabled to ExpectedColor(
            light = "#E20074", // primary magenta color
            dark = "#E20074", // same for light and dark
        ),
        R.color.checkbox_unchecked_enabled to ExpectedColor(
            light = "#B2B2B2",
            dark = "#B2B2B2" // same for light and dark
        ),
        R.color.checkbox_checked_disabled to ExpectedColor(
            light = "#B2B2B2",
            dark = "#4C4C4C"
        ),
        R.color.checkbox_unchecked_disabled to ExpectedColor(
            light = "#CCCCCC",
            dark = "#4C4C4C"
        ),
        R.color.share_title_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#FFFFFF"
        ),
        R.color.share_subtitle_txt_color to ExpectedColor(
            light = "#B2B2B2",
            dark = "#B2B2B2"
        ),
        R.color.share_info_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.share_search_border_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.share_btn_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.share_list_item_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.share_disabled_txt_color to ExpectedColor(
            light = "#B2B2B2",
            dark = "#666666"
        ),
        R.color.share_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.share_et_divider to ExpectedColor(
            light = "#000000",
            dark = "#FFFFFF"
        ),
        R.color.share_warning_txt_color to ExpectedColor(
            light = "#191919",
            dark = "#191919" // same for light and dark
        ),
        R.color.sharing_warning_bg_color to ExpectedColor(
            light = "#F6E5EB",
            dark = "#F6E5EB" // same for light and dark
        ),
        R.color.sharing_warning_border_color to ExpectedColor(
            light = "#C16F81",
            dark = "#C16F81" // same for light and dark
        ),
        R.color.share_color to ExpectedColor(
            light = "#0D39DF",
            dark = "#0D39DF" // same for light and dark
        ),
        R.color.shared_with_me_color to ExpectedColor(
            light = "#0099ff",
            dark = "#0099ff" // same for light and dark
        ),
        R.color.share_blue_color to ExpectedColor(
            light = "#2238df",
            dark = "#7d94f9"
        ),
        R.color.scan_doc_bg_color to ExpectedColor(
            light = "#F2F2F2",
            dark = "#121212"
        ),
        R.color.scan_text_color to ExpectedColor(
            light = "#191919",
            dark = "#F2F2F2"
        ),
        R.color.scan_edit_bottom_color to ExpectedColor(
            light = "#F2F2F2",
            dark = "#333333"
        ),
        R.color.scan_count_bg_color to ExpectedColor(
            light = "#B2B2B2",
            dark = "#333333"
        ),
        R.color.neptune to ExpectedColor(
            light = "#77b6bb",
            dark = "#77b6bb" // same for light and dark
        ),
        R.color.neptune_50 to ExpectedColor(
            light = "#5077b6bb",
            dark = "#5077b6bb" // same for light and dark
        ),
    )

    @Test
    fun validateLightModeColors() {
        validateColors(Configuration.UI_MODE_NIGHT_NO) { Color.parseColor(it.light) }
    }

    @Test
    fun validateDarkModeColors() {
        validateColors(Configuration.UI_MODE_NIGHT_YES) { Color.parseColor(it.dark) }
    }

    private fun validateColors(
        nightMode: Int,
        expectedSelector: (ExpectedColor) -> Int
    ) {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val conf = Configuration(appContext.resources.configuration)
        conf.uiMode = (conf.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        val ctx = appContext.createConfigurationContext(conf)

        expectedColorMap.forEach { (colorRes, expectedColor) ->
            val expected = expectedSelector(expectedColor)
            val actual = ContextCompat.getColor(ctx, colorRes)
            assertEquals(
                "Color mismatch: ${ctx.resources.getResourceName(colorRes)}",
                expected, actual
            )
        }
    }

    data class ExpectedColor(val light: String, val dark: String)
}
