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
class SettingsResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.prefs_mnemonic_summary to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Displays your 12 word key (passhprase)",
                Locale.GERMAN to "12-Wort-Schlüssel anzeigen (Passphrase)"
            )
        ),
        R.string.prefs_keys_exist_summary to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "End-to-end encryption was already set up on another client. Please enter your mnemonic to allow this client to sync and decrypt the files.",
                Locale.GERMAN to "Die Ende-zu-Ende Verschlüsselung wurde bereits auf einem anderen Gerät eingerichtet. Bitte geben Sie Ihre Passphrase ein, damit die Dateien synchronisiert und entschlüsselt werden."
            )
        ),
        R.string.actionbar_contacts to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Back up contacts",
                Locale.GERMAN to "Kontakte sichern"
            )
        ),
        R.string.actionbar_calendar_contacts_restore to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Restore contacts and calendar",
                Locale.GERMAN to "Kontakte & Kalender wiederherstellen"
            )
        ),
        R.string.prefs_category_account_info to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Account Information",
                Locale.GERMAN to "Kontoinformationen"
            )
        ),
        R.string.prefs_category_info to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Info",
                Locale.GERMAN to "Info"
            )
        ),
        R.string.prefs_category_data_privacy to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Data Privacy",
                Locale.GERMAN to "Datenschutz"
            )
        ),
        R.string.privacy_settings to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Privacy Settings",
                Locale.GERMAN to "Datenschutz-Einstellungen"
            )
        ),
        R.string.privacy_policy to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Privacy Policy",
                Locale.GERMAN to "Datenschutzbestimmungen"
            )
        ),
        R.string.prefs_delete_account to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Delete account permanently",
                Locale.GERMAN to "Konto endgültig löschen"
            )
        ),
        R.string.prefs_open_source to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Used OpenSource Software",
                Locale.GERMAN to "Verwendete OpenSource Software"
            )
        ),
        R.string.prefs_category_service to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Service",
                Locale.GERMAN to "Bedienung"
            )
        ),
        R.string.logs_menu_save to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Save logs",
                Locale.GERMAN to "Protokolle speichern"
            )
        ),
        R.string.logs_export_success to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Logs saved successfully",
                Locale.GERMAN to "Protokolle erfolgreich gespeichert"
            )
        ),
        R.string.logs_export_failed to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Failed to save logs",
                Locale.GERMAN to "Fehler beim Speichern der Protokolle"
            )
        ),
        R.string.url_delete_account to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "https://www.telekom.de/hilfe/vertrag-rechnung/login-daten-passwoerter/telekom-login-loeschen",
            )
        ),
        R.string.url_imprint_nmc to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "https://www.telekom.de/impressum",
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
