/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.events

/**
 * Event for set folder as encrypted/decrypted
 */
class EncryptionEvent(
    @JvmField
    val localId: Long,
    @JvmField
    val remoteId: String,
    @JvmField
    val remotePath: String,
    @JvmField
    val shouldBeEncrypted: Boolean
)
