/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nmc.android.ui.conflict

import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nmc.android.ui.conflict.ConflictsResolveConsentDialog.Companion.newInstance
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener
import com.owncloud.android.utils.FileStorageUtils
import junit.framework.TestCase
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ConflictsResolveConsentDialogIT : AbstractIT() {
    @get:Rule
    val activityRule = IntentsTestRule(ConflictsResolveActivity::class.java, true, false)

    private var returnCode = false

    @Test
    fun replaceWithNewFile() {
        returnCode = false

        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )

        val existingFile = OCFile("/newFile.txt")
        existingFile.fileLength = 1024000
        existingFile.modificationTimestamp = 1582019340
        existingFile.remoteId = "00000123abc"

        val newFile = OCFile("/newFile.txt")
        newFile.fileLength = 56000
        newFile.modificationTimestamp = 1522019340
        newFile.storagePath = FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt"

        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)

        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(FileActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        intent.putExtra(ConflictsResolveActivity.EXTRA_LAUNCHED_FROM_TEST, true)

        val sut = activityRule.launchActivity(intent)

        val dialog = newInstance(
            targetContext,
            existingFile,
            newFile,
            UserAccountManagerImpl
                .fromContext(targetContext)
                .user
        )
        dialog.showDialog(sut)

        sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
            Assert.assertEquals(decision, Decision.KEEP_LOCAL)
            returnCode = true
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        Espresso.onView(ViewMatchers.withId(R.id.replace_btn)).perform(ViewActions.click())

        TestCase.assertTrue(returnCode)
    }

    @Test
    fun keepBothFiles() {
        returnCode = false

        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )

        val existingFile = OCFile("/newFile.txt")
        existingFile.fileLength = 1024000
        existingFile.modificationTimestamp = 1582019340

        val newFile = OCFile("/newFile.txt")
        newFile.fileLength = 56000
        newFile.modificationTimestamp = 1522019340
        newFile.storagePath = FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt"

        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)

        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(FileActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        intent.putExtra(FileActivity.EXTRA_USER, user)
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        intent.putExtra(ConflictsResolveActivity.EXTRA_LAUNCHED_FROM_TEST, true)

        val sut = activityRule.launchActivity(intent)

        val dialog = newInstance(
            targetContext,
            existingFile,
            newFile,
            UserAccountManagerImpl
                .fromContext(targetContext)
                .user
        )
        dialog.showDialog(sut)

        sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
            Assert.assertEquals(decision, Decision.KEEP_BOTH)
            returnCode = true
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        Espresso.onView(ViewMatchers.withId(R.id.keep_both_btn)).perform(ViewActions.click())

        TestCase.assertTrue(returnCode)
    }

    @Test
    fun keepExistingFile() {
        returnCode = false

        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )

        val existingFile = OCFile("/newFile.txt")
        existingFile.fileLength = 1024000
        existingFile.modificationTimestamp = 1582019340

        val newFile = OCFile("/newFile.txt")
        newFile.fileLength = 56000
        newFile.modificationTimestamp = 1522019340
        newFile.storagePath = FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt"

        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)

        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(FileActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        intent.putExtra(FileActivity.EXTRA_USER, user)
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        intent.putExtra(ConflictsResolveActivity.EXTRA_LAUNCHED_FROM_TEST, true)

        val sut = activityRule.launchActivity(intent)

        val dialog = newInstance(
            targetContext,
            existingFile,
            newFile,
            UserAccountManagerImpl
                .fromContext(targetContext)
                .user
        )
        dialog.showDialog(sut)

        sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
            Assert.assertEquals(decision, Decision.KEEP_SERVER)
            returnCode = true
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        Espresso.onView(ViewMatchers.withId(R.id.cancel_keep_existing_btn)).perform(ViewActions.click())

        TestCase.assertTrue(returnCode)
    }

    @After
    override fun after() {
        storageManager.deleteAllFiles()
    }
}