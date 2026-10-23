/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import android.widget.ImageView
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailGenerator @Inject constructor(
    val fileThumbnailGenerator: FileThumbnailGenerator,
    val folderThumbnailGenerator: FolderThumbnailGenerator
) {
    fun setThumbnail(ocFile: OCFile?, view: ImageView?, arguments: ThumbnailArguments = ThumbnailArguments.none) {
        if (ocFile == null || view == null) {
            return
        }

        when {
            ocFile.isOfflineOperation -> fileThumbnailGenerator.setOfflineOperationThumbnail(ocFile, view)
            ocFile.isFolder -> {
                // NMC Customization
                fileThumbnailGenerator.updateThumbnailViewSize(
                    view,
                    arguments.isGrid,
                    arguments.isMediaGallery,
                    R.dimen.standard_folders_grid_item_size
                )
                // reset the padding as this will change for files and we don't this for folders
                view.setPadding(0, 0, 0, 0)

                folderThumbnailGenerator.setFolderThumbnail(ocFile, view, arguments.shimmer)
            }

            else -> fileThumbnailGenerator.setThumbnail(ocFile, view, arguments)
        }
    }
}
