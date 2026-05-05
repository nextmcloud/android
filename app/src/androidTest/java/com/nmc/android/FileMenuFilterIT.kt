/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nmc.android

import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.test.TestActivity
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.services.OperationsService
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.utils.MimeType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class FileMenuFilterIT : AbstractIT() {

    @MockK
    private lateinit var mockComponentsGetter: ComponentsGetter

    @MockK
    private lateinit var mockStorageManager: FileDataStorageManager

    @MockK
    private lateinit var mockFileUploaderBinder: FileUploadHelper

    @MockK
    private lateinit var mockOperationsServiceBinder: OperationsService.OperationsServiceBinder

    @MockK
    private lateinit var mockFileDownloadProgressListener: FileDownloadWorker.FileDownloadProgressListener

    @MockK
    private lateinit var mockArbitraryDataProvider: ArbitraryDataProvider

    private lateinit var editorUtils: EditorUtils

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { mockFileUploaderBinder.isUploading(any(), any()) } returns false
        every { mockComponentsGetter.fileUploaderHelper } returns mockFileUploaderBinder
        every { mockFileDownloadProgressListener.isDownloading(any(), any()) } returns false
        every { mockComponentsGetter.fileDownloadProgressListener } returns mockFileDownloadProgressListener
        every { mockOperationsServiceBinder.isSynchronizing(any(), any()) } returns false
        every { mockComponentsGetter.operationsServiceBinder } returns mockOperationsServiceBinder
        every { mockStorageManager.getFileById(any()) } returns OCFile("/")
        every { mockStorageManager.getFolderContent(any(), any()) } returns ArrayList<OCFile>()
        every { mockArbitraryDataProvider.getValue(any<User>(), any()) } returns ""
        editorUtils = EditorUtils(mockArbitraryDataProvider)
    }

    @Test
    fun hide_shareAndFavouriteMenu_encryptedFolder() {
        val capability = OCCapability().apply {
            endToEndEncryption = CapabilityBooleanType.TRUE
        }

        val encryptedFolder = OCFile("/encryptedFolder/").apply {
            isEncrypted = true
            mimeType = MimeType.DIRECTORY
            fileLength = SecureRandom().nextLong()
        }

        configureCapability(capability)

        launchActivity<TestActivity>().use {
            it.onActivity { activity ->
                val filterFactory =
                    FileMenuFilter.Factory(mockStorageManager, activity, editorUtils)

                val sut = filterFactory.newInstance(encryptedFolder, mockComponentsGetter, true, user)
                val toHide = sut.getToHide(false)

                // encrypted folder
                assertTrue(toHide.contains(R.id.action_see_details))
                assertTrue(toHide.contains(R.id.action_favorite))
                assertTrue(toHide.contains(R.id.action_unset_favorite))
            }
        }
    }

    @Test
    fun show_shareAndFavouriteMenu_normalFolder() {
        val capability = OCCapability().apply {
            endToEndEncryption = CapabilityBooleanType.TRUE
        }

        val normalFolder = OCFile("/folder/").apply {
            mimeType = MimeType.DIRECTORY
            fileLength = SecureRandom().nextLong()
        }

        configureCapability(capability)

        launchActivity<TestActivity>().use {
            it.onActivity { activity ->
                val filterFactory =
                    FileMenuFilter.Factory(mockStorageManager, activity, editorUtils)

                val sut = filterFactory.newInstance(normalFolder, mockComponentsGetter, true, user)
                val toHide = sut.getToHide(false)

                // normal folder
                assertFalse(toHide.contains(R.id.action_see_details))
                assertFalse(toHide.contains(R.id.action_favorite))
                assertTrue(toHide.contains(R.id.action_unset_favorite))
            }
        }
    }

    private fun configureCapability(capability: OCCapability) {
        every { mockStorageManager.getCapability(any<User>()) } returns capability
        every { mockStorageManager.getCapability(any<String>()) } returns capability
    }
}
