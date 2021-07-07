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
class PrivacySettingsResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.privacy_settings_intro_text to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "To optimize your app, we collect anonymous data. For this we use software solutions of different partners. We would like to give you full transparency and decision-making power over the processing and collection of your anonymized usage data. You can also change your settings at any time later in the app settings under data protection. Please note, however, that data collection makes a considerable contribution to the optimization of this app and you prevent this optimization by preventing data transmission.",
                Locale.GERMAN to "Zur Optimierung unserer App erfassen wir anonymisierte Daten. Hierzu nutzen wir Software Lösungen verschiedener Partner. Wir möchten Ihnen volle Transparenz und Entscheidungsgewalt über die Verarbeitung und Erfassung Ihrer anonymisierten Nutzungsdaten geben. Ihre Einstellungen können Sie auch später jederzeit in den Einstellungen unter Datenschutz ändern. Bitte beachten Sie jedoch, dass die Datenerfassungen einen erheblichen Beitrag zur Optimierung dieser App leisten und Sie diese Optimierungen durch die Unterbindung der Datenübermittlung verhindern."
            )
        ),
        R.string.required_data_collection to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Required data collection",
                Locale.GERMAN to "Erforderliche Datenerfassung"
            )
        ),
        R.string.data_collection_info to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "The collection of this data is necessary to be able to use essential functions of the app.",
                Locale.GERMAN to "Die Erfassung dieser Daten ist notwendig, um wesentliche Funktionen der App nutzen zu können."
            )
        ),
        R.string.data_analysis to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Analysis-data acquisition for the design",
                Locale.GERMAN to "Analyse-Datenerfassung zur bedarfsgerechten Gestaltung"
            )
        ),
        R.string.data_analysis_info to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "This data helps us to optimize the app usage for you and to identify system crashes and errors more quickly.",
                Locale.GERMAN to "Diese Daten helfen uns, die App Nutzung für Sie zu optimieren und Systemabstürze und Fehler schneller zu identifizieren."
            )
        ),
        R.string.login_privacy_settings_intro_text to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "This app uses Cookies and similar technologies (tools). By " +
                    "clicking Accept, you accept the processing and also the Transfer of your data to third parties. The data will " +
                    "be used for Analysis, retargeting and to Display personalized Content and Advertising on sites and " +
                    "third-party sites. You can find further information, including Information on data processing by third-party " +
                    "Providers, in the Settings and in our %s. You can %s the use of the Tools or customize them at any time in the " +
                    "%s.",
                Locale.GERMAN to "Diese App verwendet Cookies und ähnliche Technologien (Tools). " +
                    "Mit einem Klick auf Zustimmen akzeptieren Sie die Verarbeitung und auch die Weitergabe Ihrer Daten an " +
                    "Drittanbieter. Die Daten werden für Analysen, Retargeting und zur Ausspielung von personalisierten Inhalten " +
                    "und Werbung auf Seiten der Telekom, sowie auf Drittanbieterseiten genutzt. Weitere Informationen, auch zur " +
                    "Datenverarbeitung durch Drittanbieter, finden Sie in den Einstellungen sowie in unseren %s. Sie können die " +
                    "Verwendung der Tools %s oder jederzeit über ihre %s anpassen."
            )
        ),
        R.string.login_privacy_settings_header to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Privacy Settings",
                Locale.GERMAN to "Datenschutz-Einstellungen"
            )
        ),
        R.string.common_accept to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Accept",
                Locale.GERMAN to "Akzeptieren"
            )
        ),
        R.string.save_settings to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Save Settings",
                Locale.GERMAN to "Einstellungen speichern"
            )
        ),
        R.string.login_privacy_policy to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Privacy Policy",
                Locale.GERMAN to "Datenschutzhinweise"
            )
        ),
        R.string.login_privacy_reject to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "reject",
                Locale.GERMAN to "ablehnen"
            )
        ),
        R.string.login_privacy_settings to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Settings",
                Locale.GERMAN to "Einstellungen"
            )
        ),
        R.string.sourcecode_url to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "https://static.magentacloud.de/licences/android.html",
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