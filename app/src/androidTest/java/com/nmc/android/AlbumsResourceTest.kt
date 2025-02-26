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
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Test class to verify the strings and dimens customized in this branch PR for NMC
 */
@RunWith(AndroidJUnit4::class)
class AlbumsResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.drawer_item_album to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Albums",
                Locale.GERMAN to "Alben"
            )
        ),  R.string.create_album to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Create album",
                Locale.GERMAN to "Album erstellen"
            )
        ),  R.string.create_album_dialog_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "New album",
                Locale.GERMAN to "Neues Album"
            )
        ),  R.string.rename_album_dialog_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Rename album",
                Locale.GERMAN to "Album umbenennen"
            )
        ),  R.string.rename_dialog_button to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Rename",
                Locale.GERMAN to "Speichern"
            )
        ),  R.string.create_album_dialog_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Enter your new Album name",
                Locale.GERMAN to "Gib einen Namen für das Album ein"
            )
        ),  R.string.album_name_empty to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Album name cannot be empty",
                Locale.GERMAN to "Der Albumname darf nicht leer sein"
            )
        ),  R.string.hidden_album_name to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Album name cannot start with invalid char",
                Locale.GERMAN to "Der Albumname darf nicht mit einem ungültigen Zeichen beginnen"
            )
        ),  R.string.add_more to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Add more",
                Locale.GERMAN to "Mehr hinzufügen"
            )
        ),  R.string.album_rename to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Rename Album",
                Locale.GERMAN to "Album umbenennen"
            )
        ),  R.string.album_delete to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Delete Album",
                Locale.GERMAN to "Album löschen"
            )
        ),  R.string.album_delete_failed_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Failed to delete few of the files.",
                Locale.GERMAN to "Einige Dateien konnten nicht gelöscht werden."
            )
        ),  R.string.album_already_exists to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Album already exists",
                Locale.GERMAN to "Das Album existiert bereits"
            )
        ),  R.string.album_picker_toolbar_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Pick Album",
                Locale.GERMAN to "Album auswählen"
            )
        ),  R.string.media_picker_toolbar_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Pick Media Files",
                Locale.GERMAN to "Mediendateien auswählen"
            )
        ),  R.string.empty_albums_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Create Albums for your Photos",
                Locale.GERMAN to "Erstelle Alben für deine Fotos"
            )
        ),  R.string.empty_albums_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "You can organize all your photos in as many albums as you like. You haven\'t created an album yet.",
                Locale.GERMAN to "Sie können all Ihre Fotos in beliebig vielen Alben organisieren. Bisher haben Sie noch kein Album erstellt."
            )
        ),  R.string.add_to_album to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Add to Album",
                Locale.GERMAN to "Zum Album hinzufügen"
            )
        ),  R.string.album_file_added_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "File added successfully",
                Locale.GERMAN to "Datei erfolgreich hinzugefügt"
            )
        ),  R.string.empty_album_detailed_view_title to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "All that\'s missing are your photos",
                Locale.GERMAN to "Es fehlen nur noch Ihre Fotos"
            )
        ),  R.string.empty_album_detailed_view_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "You can add as many photos as you like. A photo can also belong to more than one album.",
                Locale.GERMAN to "Sie können so viele Fotos hinzufügen, wie Sie möchten. Ein Foto kann auch mehreren Alben zugeordnet werden."
            )
        ),
        R.string.add_photos to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Add photos",
                Locale.GERMAN to "Fotos hinzufügen"
            )
        ),  R.string.album_items_text to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "%d Items — %s",
                Locale.GERMAN to "%d Elemente — %s"
            )
        ),  R.string.album_unsupported_file to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Unsupported media",
                Locale.GERMAN to "Nicht unterstützte Medien"
            )
        ),  R.string.album_upload_from_camera_roll to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Upload from cameraroll",
                Locale.GERMAN to "Dateien hochladen"
            )
        ),  R.string.album_upload_from_account to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Select images from account",
                Locale.GERMAN to "Dateien auswählen"
            )
        ),  R.string.album_rename_conflict to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "This name is already in use.",
                Locale.GERMAN to "Dieser Name wird bereits verwendet."
            )
        ),  R.string.album_copy_file_conflict to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Already exists.",
                Locale.GERMAN to "Existiert bereits."
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
        R.dimen.album_list_image_width to ExpectedDimen(
            default = 78f,
            unit = DimenUnit.DP
        ),
        R.dimen.album_list_image_height to ExpectedDimen(
            default = 56f,
            unit = DimenUnit.DP
        ),
        R.dimen.album_grid_image_height to ExpectedDimen(
            default = 140f,
            unit = DimenUnit.DP
        ),
        R.dimen.album_grid_image_corner_radius to ExpectedDimen(
            default = 8f,
            unit = DimenUnit.DP
        ),
        R.dimen.album_list_image_corner_radius to ExpectedDimen(
            default = 4f,
            unit = DimenUnit.DP
        ),
        R.dimen.album_grid_spacing to ExpectedDimen(
            default = 4f,
            unit = DimenUnit.DP
        ),
        R.dimen.album_recycler_view_grid_padding to ExpectedDimen(
            default = 8f,
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
