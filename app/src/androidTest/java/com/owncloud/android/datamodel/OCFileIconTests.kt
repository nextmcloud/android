/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import com.owncloud.android.R
import com.owncloud.android.lib.common.network.WebdavEntry.MountType
import org.junit.After
import org.junit.Before
import org.junit.Test

class OCFileIconTests {

    private val path = "/path/to/a/file.txt"
    private var sut: OCFile? = null

    @Before
    fun setup() {
        sut = OCFile(path)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsAutoUploadFolderShouldReturnFolderOverlayUploadIcon() {
        val fileOverlayIcon = sut?.getFileOverlayIconId(true)
        val expectedDrawable = R.drawable.folder_auto_upload
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsEncryptedShouldReturnFolderOverlayKeyIcon() {
        sut?.isEncrypted = true
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.folder_encrypted
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsGroupFolderShouldReturnFolderOverlayAccountGroupIcon() {
        sut?.mountType = MountType.GROUP
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.folder_shared_users
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsSharedViaLinkShouldReturnFolderOverlayLinkIcon() {
        sut?.isSharedViaLink = true
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.folder_shared_users
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsSharedShouldReturnFolderOverlayShareIcon() {
        sut?.isSharedWithSharee = true
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.folder_shared_users
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsExternalShouldReturnFolderOverlayExternalIcon() {
        sut?.mountType = MountType.EXTERNAL
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.folder
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsLockedShouldReturnFolderOverlayLockIcon() {
        sut?.isLocked = true
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.folder_encrypted
        assert(fileOverlayIcon == expectedDrawable)
    }

    @Test
    fun testGetFileOverlayIconWhenFileIsFolderShouldReturnNull() {
        val fileOverlayIcon = sut?.getFileOverlayIconId(false)
        val expectedDrawable = R.drawable.folder
        assert(fileOverlayIcon == expectedDrawable)
    }

    @After
    fun destroy() {
        sut = null
    }
}
