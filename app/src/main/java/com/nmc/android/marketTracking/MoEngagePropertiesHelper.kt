/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nmc.android.marketTracking

enum class EventFileType(val fileType: String) {
    PHOTO("foto"),
    SCAN("scan"),
    VIDEO("video"),
    AUDIO("audio"),
    TEXT("text"),
    PDF("pdf"),
    DOCUMENT("docx"),
    SPREADSHEET("xlsx"),
    PRESENTATION("pptx"),
    OTHER("other"), // default
}

enum class EventFolderType(val folderType: String) {
    ENCRYPTED("encrypted"),
    NOT_ENCRYPTED("not encrypted")
}