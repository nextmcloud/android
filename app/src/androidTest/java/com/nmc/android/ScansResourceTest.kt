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
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Test class to verify the strings and attrs customized in this branch PR for NMC
 */
@RunWith(AndroidJUnit4::class)
class ScansResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.upload_scan_document to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Scan Document",
                Locale.GERMAN to "Dokument scannen"
            )
        ),
        R.string.result_scan_doc_dont_move to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Do not move",
                Locale.GERMAN to "Nicht bewegen"
            )
        ),
        R.string.result_scan_doc_move_closer to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Move closer",
                Locale.GERMAN to "Näher heranbewegen"
            )
        ),
        R.string.result_scan_doc_perspective to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Perspective",
                Locale.GERMAN to "Perspektive"
            )
        ),
        R.string.result_scan_doc_no_doc to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "No document",
                Locale.GERMAN to "Kein Dokument"
            )
        ),
        R.string.result_scan_doc_bg_noisy to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Background too noisy",
                Locale.GERMAN to "Hintergrund zu unruhig"
            )
        ),
        R.string.result_scan_doc_aspect_ratio to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Wrong aspect ratio.\nRotate your device.",
                Locale.GERMAN to "Falsches Bildformat.\nDrehen Sie Ihr Gerät."
            )
        ),
        R.string.result_scan_doc_poor_light to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Poor light",
                Locale.GERMAN to "Schwaches Licht"
            )
        ),
        R.string.scanned_doc_count to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "%d of %d",
                Locale.GERMAN to "%d von %d"
            )
        ),
        R.string.title_edit_scan to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Edit Scan",
                Locale.GERMAN to "Scan bearbeiten"
            )
        ),
        R.string.title_crop_scan to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Crop Scan",
                Locale.GERMAN to "Scan beschneiden"
            )
        ),
        R.string.crop_btn_reset_crop_text to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Reset Crop",
                Locale.GERMAN to "Rahmen zurücksetzen"
            )
        ),
        R.string.crop_btn_detect_doc_text to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Detect Document",
                Locale.GERMAN to "Dokument erkennen"
            )
        ),
        R.string.edit_scan_filter_dialog_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Apply Filter",
                Locale.GERMAN to "Filter anwenden"
            )
        ),
        R.string.edit_scan_filter_none to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "No Filter",
                Locale.GERMAN to "Kein Filter"
            )
        ),
        R.string.edit_scan_filter_pure_binarized to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Whiteboard",
                Locale.GERMAN to "Whiteboard"
            )
        ),
        R.string.edit_scan_filter_color_enhanced to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Photo Filter",
                Locale.GERMAN to "Foto Filter"
            )
        ),
        R.string.edit_scan_filter_b_n_w to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Black & White",
                Locale.GERMAN to "Schwarz-Weiß"
            )
        ),
        R.string.edit_scan_filter_color_document to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Document Filter",
                Locale.GERMAN to "Dokument Filter"
            )
        ),
        R.string.edit_scan_filter_grey to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Grayscale",
                Locale.GERMAN to "Grau"
            )
        ),
        R.string.automatic to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Automatic",
                Locale.GERMAN to "Automatisch"
            )
        ),
        R.string.flash to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Flash",
                Locale.GERMAN to "Blitz"
            )
        ),
        R.string.title_save_as to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Save as",
                Locale.GERMAN to "Speichern unter"
            )
        ),
        R.string.scan_save_filename to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Filename",
                Locale.GERMAN to "Dateiname"
            )
        ),
        R.string.scan_save_location to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Location",
                Locale.GERMAN to "Speicherort"
            )
        ),
        R.string.scan_save_location_root to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "/Root folder",
                Locale.GERMAN to "/Hauptverzeichnis"
            )
        ),
        R.string.scan_save_file_type to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "File type",
                Locale.GERMAN to "Dateityp"
            )
        ),
        R.string.scan_save_without_text_recognition to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Save without text recognition",
                Locale.GERMAN to "Speichern ohne Texterkennung"
            )
        ),
        R.string.scan_save_with_text_recognition to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Save with text recognition",
                Locale.GERMAN to "Speichern mit Texterkennung"
            )
        ),
        R.string.scan_save_file_type_txt to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Textfile (txt)",
                Locale.GERMAN to "Textdokument (txt)"
            )
        ),
        R.string.scan_save_pdf_password to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "PDF-Password",
                Locale.GERMAN to "PDF-Passwort"
            )
        ),
        R.string.scan_save_set_password_hint to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Set password",
                Locale.GERMAN to "Passwort setzen"
            )
        ),
        R.string.scan_save_no_file_select_toast to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Please select at least one filetype",
                Locale.GERMAN to "Bitten wählen sie mindestens einen Dateityp zum Speichern aus."
            )
        ),
        R.string.save_scan_empty_pdf_password to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Please enter a password for the PDF you want to create or disable the function.",
                Locale.GERMAN to "Bitte geben Sie ein Passwort für das zu erstellende PDF ein oder deaktivieren Sie die Funktion."
            )
        ),
        R.string.scan_save_file_type_text to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "You can save the file with or without text recognition. Multiple selection is allowed.",
                Locale.GERMAN to "Sie können die gescannten Dokumente mit oder ohne Texterkennung abspeichern. Sie können auch mehrere Dateiformate auswählen."
            )
        ),
        R.string.camera_permission_denied to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "You cannot scan document without camera permission.",
                Locale.GERMAN to "Sie können keine Dokumente scannen ohne die Erlaubnis die Kamera zu verwenden."
            )
        ),
        R.string.description_add_more_scan to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Add more document",
                Locale.GERMAN to "Weiteres Dokument hinzufügen"
            )
        ),
        R.string.description_crop_scan to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Crop scanned document",
                Locale.GERMAN to "Gescanntes Dokument zuschneiden"
            )
        ),
        R.string.description_filter_scan to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Filter scanned document",
                Locale.GERMAN to "Gescanntes Dokument filtern"
            )
        ),
        R.string.description_rotate_scan to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Rotate scanned document",
                Locale.GERMAN to "Gescanntes Dokument drehen"
            )
        ),
        R.string.description_delete_scan to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Delete scanned document",
                Locale.GERMAN to "Gescanntes Dokument löschen"
            )
        ),
        R.string.description_edit_filename to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Edit scan filename",
                Locale.GERMAN to "Scan-Dateinamen bearbeiten"
            )
        ),
        R.string.description_edit_location to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Edit scan location",
                Locale.GERMAN to "Scan-Speicherort bearbeiten"
            )
        ),
        R.string.dialog_ok to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Ok",
                Locale.GERMAN to "Ok"
            )
        ),
        R.string.dialog_save_scan_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Saving will take some time, especially if you have selected several pages and file formats.",
                Locale.GERMAN to "Das Speichern kann einige Minuten in Anspruch nehmen, insbesondere wenn Sie mehrere Seiten und Dateiformate ausgewählt haben."
            )
        ),
        R.string.scan_save_file_type_pdf to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "PDF",
                Locale.GERMAN to "PDF"
            )
        ),
        R.string.scan_save_file_type_jpg to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "JPG",
                Locale.GERMAN to "JPG"
            )
        ),
        R.string.scan_save_file_type_png to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "PNG",
                Locale.GERMAN to "PNG"
            )
        ),
        R.string.scan_save_file_type_pdf_ocr to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "PDF (OCR)",
                Locale.GERMAN to "PDF (OCR)"
            )
        ),
        R.string.foreground_service_save to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Saving files…",
                Locale.GERMAN to "Dateien werden gespeichert…"
            )
        ),
        R.string.notification_channel_image_save to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Image save notification channel",
                Locale.GERMAN to "Benachrichtigungskanal zum Speichern von Bildern"
            )
        ),
        R.string.notification_channel_image_save_description to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Shows image save progress",
                Locale.GERMAN to "Zeigt den Fortschritt der Bildspeicherung an"
            )
        ),
        R.string.choose_location to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Choose location",
                Locale.GERMAN to "Ort wählen"
            )
        ),
        R.string.common_select to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Select",
                Locale.GERMAN to "Auswählen"
            )
        )
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

    private val localizedStringArrayMap = mapOf(
        R.array.edit_scan_filter_values to ExpectedLocalizedStringArray(
            translations = mapOf(
                Locale.ENGLISH to arrayOf(
                    "No Filter",
                    "Photo Filter",
                    "Document Filter",
                    "Grayscale",
                    "Black & White",
                    "Whiteboard"
                ),
                Locale.GERMAN to arrayOf(
                    "Kein Filter",
                    "Foto Filter",
                    "Dokument Filter",
                    "Grau",
                    "Schwarz-Weiß",
                    "Whiteboard"
                )
            )
        ),
    )

    @Test
    fun verifyLocalizedStringArray() {
        localizedStringArrayMap.forEach { (arrayRes, expected) ->
            expected.translations.forEach { (locale, expectedArray) ->

                val config = Configuration(baseContext.resources.configuration)
                config.setLocale(locale)

                val localizedContext = baseContext.createConfigurationContext(config)
                val actualArray = localizedContext.resources.getStringArray(arrayRes)

                assertArrayEquals(
                    "Mismatch for ${baseContext.resources.getResourceEntryName(arrayRes)} in $locale",
                    expectedArray,
                    actualArray
                )
            }
        }
    }

    data class ExpectedLocalizedStringArray(val translations: Map<Locale, Array<String>>)
}
