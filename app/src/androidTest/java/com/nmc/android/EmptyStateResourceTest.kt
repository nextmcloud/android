/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
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
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Test class to verify the strings and dimens customized in this branch PR for NMC
 */
@RunWith(AndroidJUnit4::class)
class EmptyStateResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.file_list_empty to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Upload some content or sync with your devices.",
                Locale.GERMAN to "Laden Sie Inhalte hoch oder synchronisieren Sie mit Ihren Geräten."
            )
        ),
        R.string.file_list_empty_headline to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "No files here",
                Locale.GERMAN to "Keine Dateien vorhanden"
            )
        ),
        R.string.search_result_empty to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Search for a file (at least 2 characters)",
                Locale.GERMAN to "Eine Datei suchen (mindestens 2 Zeichen)"
            )
        ),
        R.string.gallery_list_empty to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "No photos or videos uploaded yet",
                Locale.GERMAN to "Noch keine hochgeladene Fotos oder Videos vorhanden"
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
        R.dimen.empty_list_icon_size to ExpectedDimen(
            default = 120f,
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
}
