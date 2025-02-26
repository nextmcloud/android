/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.albums

interface AlbumSharingBottomSheetActions {
    fun createShare()

    fun removeShare()

    fun copyShareLink()

    fun shareAlbumLink()
}
