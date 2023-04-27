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
 * Test class to verify the strings, dimens and bool customized in this branch PR for NMC
 */
@RunWith(AndroidJUnit4::class)
class SharingResourceTest {

    private val baseContext = ApplicationProvider.getApplicationContext<Context>()

    private val localizedStringMap = mapOf(
        R.string.unshare to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Unshare",
                Locale.GERMAN to "Freigabe aufheben"
            )
        ),
        R.string.allow_creating to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Allow creating",
                Locale.GERMAN to "Erstellen erlauben"
            )
        ),
        R.string.allow_deleting to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Allow deleting",
                Locale.GERMAN to "Löschen erlauben"
            )
        ),
        R.string.allow_editing to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Allow editing",
                Locale.GERMAN to "Bearbeitung erlauben"
            )
        ),
        R.string.link_share_read_only to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Read only",
                Locale.GERMAN to "Nur Lesen"
            )
        ),
        R.string.share_link_folder to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Link to folder",
                Locale.GERMAN to "Link zum Ordner"
            )
        ),
        R.string.share_link_file to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Link to file",
                Locale.GERMAN to "Link zur Datei"
            )
        ),
        R.string.share_via_link_menu_password_label to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Password protect (%1\$s)",
                Locale.GERMAN to "Passwortschutz (%1\$s)"
            )
        ),
        R.string.share_link_empty_exp_date to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "You must select expiration date.",
                Locale.GERMAN to "Sie müssen das Ablaufdatum auswählen."
            )
        ),
        R.string.share_link_empty_note_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Please enter note.",
                Locale.GERMAN to "Bitte Anmerkung eingeben."
            )
        ),
        R.string.send_email to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Send email",
                Locale.GERMAN to "Email senden"
            )
        ),
        R.string.share_open_in to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Open in…",
                Locale.GERMAN to "Öffnen mit…"
            )
        ),
        R.string.sharing_description to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "You can create links or send shares by mail. If you invite MagentaCLOUD users, you have more opportunities for collaboration.",
                Locale.GERMAN to "Sie können Links erstellen oder Freigaben per Mail versenden. Wenn Sie MagentaCLOUD Nutzer einladen, bieten sich Ihnen mehr Möglichkeiten der Zusammenarbeit."
            )
        ),
        R.string.your_message to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Your Message",
                Locale.GERMAN to "Ihre Nachricht"
            )
        ),
        R.string.personal_share_email to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Personal share by mail",
                Locale.GERMAN to "persönliche Freigabe per E-Mail"
            )
        ),
        R.string.create_link to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Create new link",
                Locale.GERMAN to "Neuen Link erstellen"
            )
        ),
        R.string.your_shares to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Your Shares",
                Locale.GERMAN to "Ihre Freigaben"
            )
        ),
        R.string.link_label to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Link Label",
                Locale.GERMAN to "Linkbezeichnung"
            )
        ),
        R.string.hint_link_label to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Your custom link label",
                Locale.GERMAN to "Ihre Linkbezeichnung"
            )
        ),
        R.string.file_drop_info to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "With File drop, only uploading is allowed. Only you can see files and folders that have been uploaded.",
                Locale.GERMAN to "Bei der Sammelbox ist nur das Hochladen erlaubt. Nur Sie sehen Dateien und Ordner die hochgeladen worden sind."
            )
        ),
        R.string.password_protection to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Password Protection",
                Locale.GERMAN to "Password Protection"
            )
        ),
        R.string.expiration_date to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Expiration Date",
                Locale.GERMAN to "Expiration Date"
            )
        ),
        R.string.empty_shares to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "You have not yet shared your file/folder. Share to give others access.",
                Locale.GERMAN to "Sie haben Ihre Datei / Ihren Ordner noch nicht geteilt. Teilen Sie um anderen Zugriff zu geben."
            )
        ),
        R.string.link_download_limit to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Download Limit",
                Locale.GERMAN to "Download Limit"
            )
        ),
        R.string.download_limit_empty to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Download limit cannot be empty.",
                Locale.GERMAN to "Das Feld für das Download-Limit darf nicht leer sein."
            )
        ),
        R.string.hint_download_limit to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Enter download limit",
                Locale.GERMAN to "Downlimit eingeben"
            )
        ),
        R.string.download_text to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Downloads: %s",
                Locale.GERMAN to "Downloads: %s"
            )
        ),
        R.string.download_limit_zero to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Download limit should be greater than 0.",
                Locale.GERMAN to "Der Wert für das Downloadlimit sollte größer als 0 sein."
            )
        ),
        R.string.allow_resharing_info to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "You are sharing with a MagentaCLOUD user and you can allow her or him to reshare.",
                Locale.GERMAN to "Sie teilen mit einer/einem MagentaCLOUD Nutzer(in). Sie können ihr oder ihm erlauben, den Ordner oder die Dateien weiterzuteilen."
            )
        ),
        R.string.sharing_email_warning to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Password protection has been enabled. You have to provide the password to the recipient.\n\nIf you send a share via MagentaCLOUD and paste the password in this message, it will be transmitted unencrypted in plaintext.",
                Locale.GERMAN to "Der Passwortschutz ist aktiviert. Sie müssen dem Empfänger das Passwort selbst mitteilen.\n\nWenn Sie die Freigabe über die MagentaCLOUD verschicken und das Passwort in den Nachrichtentext eintragen, wird es unverschlüsselt im Klartext übertragen."
            )
        ),
        R.string.placeholder_receivedMessage to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Received",
                Locale.GERMAN to "Empfangen"
            )
        ),
        R.string.placeholder_sharedMessage to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Shared",
                Locale.GERMAN to "Geteilt"
            )
        ),
        R.string.sharing_heading to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Send link by mail",
                Locale.GERMAN to "Link per E-Mail versenden"
            )
        ),
        R.string.reshare_not_allowed to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Resharing is not allowed.",
                Locale.GERMAN to "Weiterteilen wurde nicht erlaubt."
            )
        ),
        R.string.reshare_allowed to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Resharing is allowed.",
                Locale.GERMAN to "Weiterteilen ist erlaubt."
            )
        ),
        R.string.resharing_user_info to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "This file / folder was shared with you by %s",
                Locale.GERMAN to "Dieser Datei / dieser Ordner wurde mit ihnen geteilt von %s"
            )
        ),
        R.string.txt_or to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "or",
                Locale.GERMAN to "oder"
            )
        ),
        R.string.shared_with_heading to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Shared with",
                Locale.GERMAN to "Geteilt mit"
            )
        ),
        R.string.share_details to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Detail",
                Locale.GERMAN to "Details"
            )
        ),
        R.string.share_quick_permission_can_view to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Everyone can only view",
                Locale.GERMAN to "Jeder kann nur anzeigen"
            )
        ),
        R.string.share_quick_permission_can_view_short to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Only view",
                Locale.GERMAN to "Nur anzeigen"
            )
        ),
        R.string.share_quick_permission_can_edit to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Everyone can edit",
                Locale.GERMAN to "Jeder kann bearbeiten"
            )
        ),
        R.string.share_quick_permission_can_edit_short to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Can edit",
                Locale.GERMAN to "Nur bearbeiten"
            )
        ),
        R.string.share_quick_permission_can_upload to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Everyone can just upload",
                Locale.GERMAN to "Jeder kann nur hochladen"
            )
        ),
        R.string.share_quick_permission_can_upload_short to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Just upload",
                Locale.GERMAN to "Nur hochladen"
            )
        ),
        R.string.advanced_settings to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Advanced Permissions",
                Locale.GERMAN to "Erweiterte Berechtigungen"
            )
        ),
        R.string.link_share_allow_upload_and_editing to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Allow upload and editing",
                Locale.GERMAN to "Hochladen & Bearbeiten"
            )
        ),
        R.string.link_share_file_drop to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "File drop (upload only)",
                Locale.GERMAN to "Sammelbox"
            )
        ),
        R.string.allow_resharing to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Allow resharing",
                Locale.GERMAN to "Weiterteilen erlauben"
            )
        ),
        R.string.share_search to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Contact name or email",
                Locale.GERMAN to "Kontaktname oder E-Mail"
            )
        ),
        R.string.share_permission_file_drop to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Filedrop only",
                Locale.GERMAN to "Sammelbox"
            )
        ),
        R.string.share_permission_read_only to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Read only",
                Locale.GERMAN to "Nur lesen"
            )
        ),
        R.string.share_link_with_label to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Link \'%1\$s\'",
                Locale.GERMAN to "Link \"%1\$s\""
            )
        ),
        R.string.share_permissions to ExpectedLocalizedString(
            translations = mapOf(
                Locale.ENGLISH to "Permissions",
                Locale.GERMAN to "Berechtigungen zum Teilen"
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

    @Test
    fun validateIsTabletBoolFalseForSmallDevices() {
        val config = Configuration(baseContext.resources.configuration)

        val ctx = baseContext.createConfigurationContext(config)

        val actual = ctx.resources.getBoolean(R.bool.isTablet)

        assert(!actual)
    }

    @Test
    fun validateIsTabletBoolTrueForTabletDevices() {
        val config = Configuration(baseContext.resources.configuration)

        config.smallestScreenWidthDp = 480  // triggers values-sw480dp
        val ctx = baseContext.createConfigurationContext(config)

        val actual = ctx.resources.getBoolean(R.bool.isTablet)
        assert(actual)
    }
}
