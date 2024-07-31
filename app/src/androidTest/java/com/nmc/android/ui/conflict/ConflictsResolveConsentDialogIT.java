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

package com.nmc.android.ui.conflict;

import android.content.Intent;

import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class ConflictsResolveConsentDialogIT extends AbstractIT {
    @Rule public IntentsTestRule<ConflictsResolveActivity> activityRule =
        new IntentsTestRule<>(ConflictsResolveActivity.class, true, false);
    private boolean returnCode;

    @Test
    public void replaceWithNewFile() {
        returnCode = false;

        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(user.getAccountName()) + "/nonEmpty.txt",
                                          "/newFile.txt",
                                          user.getAccountName());

        OCFile existingFile = new OCFile("/newFile.txt");
        existingFile.setFileLength(1024000);
        existingFile.setModificationTimestamp(1582019340);
        existingFile.setRemoteId("00000123abc");

        OCFile newFile = new OCFile("/newFile.txt");
        newFile.setFileLength(56000);
        newFile.setModificationTimestamp(1522019340);
        newFile.setStoragePath(FileStorageUtils.getSavePath(user.getAccountName()) + "/nonEmpty.txt");

        FileDataStorageManager storageManager = new FileDataStorageManager(user, targetContext.getContentResolver());
        storageManager.saveNewFile(existingFile);

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.getUploadId());
        intent.putExtra(ConflictsResolveActivity.EXTRA_LAUNCHED_FROM_TEST, true);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveConsentDialog dialog = ConflictsResolveConsentDialog.newInstance(existingFile,
                                                                                         newFile,
                                                                                         UserAccountManagerImpl
                                                                                             .fromContext(targetContext)
                                                                                             .getUser()
                                                                                        );
        dialog.showDialog(sut);

        sut.listener = decision -> {
            assertEquals(decision, ConflictsResolveDialog.Decision.KEEP_LOCAL);
            returnCode = true;
        };

        getInstrumentation().waitForIdleSync();

        onView(withId(R.id.replace_btn)).perform(click());

        assertTrue(returnCode);
    }

    @Test
    public void keepBothFiles() {
        returnCode = false;

        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(user.getAccountName()) + "/nonEmpty.txt",
                                          "/newFile.txt",
                                          user.getAccountName());

        OCFile existingFile = new OCFile("/newFile.txt");
        existingFile.setFileLength(1024000);
        existingFile.setModificationTimestamp(1582019340);

        OCFile newFile = new OCFile("/newFile.txt");
        newFile.setFileLength(56000);
        newFile.setModificationTimestamp(1522019340);
        newFile.setStoragePath(FileStorageUtils.getSavePath(user.getAccountName()) + "/nonEmpty.txt");

        FileDataStorageManager storageManager = new FileDataStorageManager(user, targetContext.getContentResolver());
        storageManager.saveNewFile(existingFile);

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.getUploadId());
        intent.putExtra(ConflictsResolveActivity.EXTRA_LAUNCHED_FROM_TEST, true);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveConsentDialog dialog = ConflictsResolveConsentDialog.newInstance(existingFile,
                                                                                         newFile,
                                                                                         UserAccountManagerImpl
                                                                                             .fromContext(targetContext)
                                                                                             .getUser()
                                                                                        );
        dialog.showDialog(sut);

        sut.listener = decision -> {
            assertEquals(decision, ConflictsResolveDialog.Decision.KEEP_BOTH);
            returnCode = true;
        };

        getInstrumentation().waitForIdleSync();

        onView(withId(R.id.keep_both_btn)).perform(click());

        assertTrue(returnCode);
    }

    @Test
    public void keepExistingFile() {
        returnCode = false;

        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(user.getAccountName()) + "/nonEmpty.txt",
                                          "/newFile.txt",
                                          user.getAccountName());

        OCFile existingFile = new OCFile("/newFile.txt");
        existingFile.setFileLength(1024000);
        existingFile.setModificationTimestamp(1582019340);

        OCFile newFile = new OCFile("/newFile.txt");
        newFile.setFileLength(56000);
        newFile.setModificationTimestamp(1522019340);
        newFile.setStoragePath(FileStorageUtils.getSavePath(user.getAccountName()) + "/nonEmpty.txt");

        FileDataStorageManager storageManager = new FileDataStorageManager(user, targetContext.getContentResolver());
        storageManager.saveNewFile(existingFile);

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.getUploadId());
        intent.putExtra(ConflictsResolveActivity.EXTRA_LAUNCHED_FROM_TEST, true);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveConsentDialog dialog = ConflictsResolveConsentDialog.newInstance(existingFile,
                                                                                         newFile,
                                                                                         UserAccountManagerImpl
                                                                                             .fromContext(targetContext)
                                                                                             .getUser()
                                                                                        );
        dialog.showDialog(sut);

        sut.listener = decision -> {
            assertEquals(decision, ConflictsResolveDialog.Decision.KEEP_SERVER);
            returnCode = true;
        };

        getInstrumentation().waitForIdleSync();

        onView(withId(R.id.cancel_keep_existing_btn)).perform(click());

        assertTrue(returnCode);
    }

    @After
    public void after() {
        getStorageManager().deleteAllFiles();
    }
}