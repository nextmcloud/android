/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.albums

import com.owncloud.android.operations.albums.ReadAlbumsOperation.PhotoAlbumEntry

interface AlbumFragmentInterface {
    fun onItemClick(album: PhotoAlbumEntry)
}