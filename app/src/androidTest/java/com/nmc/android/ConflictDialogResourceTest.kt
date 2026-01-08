/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nmc.android

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.R
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Test class to verify the strings customized in this branch PR for NMC
 */
@RunWith(AndroidJUnit4::class)
class ConflictDialogResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.conflict_replace to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Replace",
                Locale.GERMAN to "Ersetzen"
            )
        ),
        R.string.conflict_replace_all to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Replace all",
                Locale.GERMAN to "Alle ersetzen"
            )
        ),
        R.string.conflict_keep_both to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Keep both",
                Locale.GERMAN to "Beide behalten"
            )
        ),
        R.string.conflict_keep_both_all to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Keep both for all",
                Locale.GERMAN to "Beide Versionen für alle behalten"
            )
        ),
        R.string.conflict_more_details to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "More details",
                Locale.GERMAN to "Mehr Details"
            )
        ),
        R.string.conflict_cancel_keep_existing to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Cancel and keep existing",
                Locale.GERMAN to "Abbrechen und bestehende Datei behalten"
            )
        ),
        R.string.conflict_dialog_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "File conflict",
                Locale.GERMAN to "Dateikonflikt"
            )
        ),
        R.string.conflict_dialog_title_multiple to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "%d File conflicts",
                Locale.GERMAN to "%d Dateikonflikte"
            )
        ),
        R.string.conflict_dialog_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "%1\$s already exists in this location. Do you want to replace it with the file you are moving?",
                Locale.GERMAN to "%1\$s ist im Zielordner bereits vorhanden. Möchten Sie die bestehende Datei behalten oder überschreiben?"
            )
        ),
        R.string.conflict_dialog_message_multiple to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "The files already exist in this location. Do you want to replace them with the files you are moving?",
                Locale.GERMAN to "Die Dateien sind im Zielordner bereits vorhanden. Möchten Sie die bestehenden Dateien behalten oder überschreiben?"
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
}