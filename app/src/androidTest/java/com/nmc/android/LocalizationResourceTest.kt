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
class LocalizationResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.about_version to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to $$"Version %1$s",
                Locale.GERMAN to $$"Version %1$s"
            )
        ),
        R.string.actionbar_see_details to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Share",
                Locale.GERMAN to "Teilen"
            )
        ),
        R.string.drawer_item_on_device to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Offline files",
                Locale.GERMAN to "Offline verfügbare Dateien"
            )
        ),
        R.string.prefs_lock to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Security",
                Locale.GERMAN to "Sicherheit"
            )
        ),
        R.string.filedetails_download to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Offline availability",
                Locale.GERMAN to "Offline Verfügbarkeit"
            )
        ),
        R.string.filedetails_sync_file to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Offline availability",
                Locale.GERMAN to "Offline Verfügbarkeit"
            )
        ),
        R.string.action_send_share to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Sharing",
                Locale.GERMAN to "Teilen"
            )
        ),
        R.string.common_cancel_sync to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Cancel",
                Locale.GERMAN to "Abbrechen"
            )
        ),
        R.string.instant_upload_path to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "/Camera-Media",
                Locale.GERMAN to "/Kamera-Medien"
            )
        ),
        R.string.prefs_synced_folders_remote_path_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Target folder in your MagentaCLOUD",
                Locale.GERMAN to "Zielorder in Deiner MagentaCLOUD"
            )
        ),
        R.string.prefs_e2e_mnemonic to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "End-to-end encryption",
                Locale.GERMAN to "Ende-zu-Ende Verschlüsselung"
            )
        ),
        R.string.storage_choose_location to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Choose Source",
                Locale.GERMAN to "Wählen Sie Quelle"
            )
        ),
        R.string.subtitle_photos_videos to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Images and videos",
                Locale.GERMAN to "Fotos und Videos"
            )
        ),
        R.string.show_images to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Show images",
                Locale.GERMAN to "Bilder anzeigen"
            )
        ),
        R.string.subtitle_photos_only to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Images only",
                Locale.GERMAN to "Nur Fotos"
            )
        ),
        R.string.select_media_folder to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Select the \"Media\" folder",
                Locale.GERMAN to "Den Ordner \"Medien\" auswählen",
            )
        ),
        R.string.file_already_exists to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Filename already exists.",
                Locale.GERMAN to "Dateiname bereits vorhanden",
            )
        ),
        R.string.email_pick_failed to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Failed to pick email address.",
                Locale.GERMAN to "Fehler beim Zugriff auf E-Mail Adresse.",
            )
        ),
        R.string.menu_item_sort_by_date_newest_first to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Newest first",
                Locale.GERMAN to "Neueste zuerst",
            )
        ),
        R.string.update_link_file_error to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "An error occurred while trying to update the share.",
                Locale.GERMAN to "Fehler bei der Aktualisierungd der Freigabe aufgetreten.",
            )
        ),
        R.string.file_list_empty_unified_search_start_search_description to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Search for files and folders",
                Locale.GERMAN to "Dateien und Ordner suchen",
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