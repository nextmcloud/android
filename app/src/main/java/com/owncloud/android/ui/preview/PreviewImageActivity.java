/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2024 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.editimage.EditImageActivity;
import com.nextcloud.client.jobs.download.FileDownloadHelper;
import com.nextcloud.client.jobs.download.FileDownloadWorker;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.model.WorkerState;
import com.nextcloud.model.WorkerStateLiveData;
import com.nextcloud.utils.extensions.IntentExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.Serializable;
import java.util.Optional;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 *  Holds a swiping gallery where image files contained in an Nextcloud directory are shown.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PreviewImageActivity extends FileActivity implements
    FileFragment.ContainerActivity,
    OnRemoteOperationListener,
    Injectable {

    public static final String TAG = PreviewImageActivity.class.getSimpleName();
    public static final String EXTRA_VIRTUAL_TYPE = "EXTRA_VIRTUAL_TYPE";
    private static final String KEY_WAITING_FOR_BINDER = "WAITING_FOR_BINDER";
    private static final String KEY_SYSTEM_VISIBLE = "TRUE";

    private OCFile livePhotoFile;
    private ViewPager2 viewPager;
    private PreviewImagePagerAdapter previewImagePagerAdapter;
    private int savedPosition;
    private boolean hasSavedPosition;
    private boolean requestWaitingForBinder;
    private DownloadFinishReceiver downloadFinishReceiver;
    private boolean isDownloadWorkStarted = false;

    @Inject AppPreferences preferences;
    @Inject LocalBroadcastManager localBroadcastManager;

    private ActionBar actionBar;

    public static Intent previewFileIntent(Context context, User user, OCFile file) {
        final Intent intent = new Intent(context, PreviewImageActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, file);
        intent.putExtra(FileActivity.EXTRA_USER, user);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        actionBar = getSupportActionBar();

        if (savedInstanceState != null && !savedInstanceState.getBoolean(KEY_SYSTEM_VISIBLE, true) &&
            actionBar != null) {
            actionBar.hide();
        }

        setContentView(R.layout.preview_image_activity);
        setupToolbar();

        livePhotoFile = IntentExtensionsKt.getParcelableArgument(getIntent(), EXTRA_LIVE_PHOTO_FILE, OCFile.class);

        // Navigation Drawer
        setupDrawer();

        // ActionBar
        OCFile chosenFile = IntentExtensionsKt.getParcelableArgument(getIntent(), FileActivity.EXTRA_FILE, OCFile.class);
        updateActionBarTitleAndHomeButton(chosenFile);

        if (actionBar != null) {
            viewThemeUtils.files.setWhiteBackButton(this, actionBar);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // to keep our UI controls visibility in line with system bars visibility
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        if (savedInstanceState != null) {
            requestWaitingForBinder = savedInstanceState.getBoolean(KEY_WAITING_FOR_BINDER);
        } else {
            requestWaitingForBinder = false;
        }

        observeWorkerState();
    }

    public void toggleActionBarVisibility(boolean hide) {
        if (actionBar == null) {
            return;
        }

        if (hide) {
            actionBar.hide();
        } else {
            actionBar.show();
        }
    }

    private void initViewPager(User user) {
        // virtual folder
        final Serializable virtualFolderType = IntentExtensionsKt.getSerializableArgument(getIntent(), EXTRA_VIRTUAL_TYPE, Serializable.class);
        if (virtualFolderType != null && virtualFolderType != VirtualFolderType.NONE) {
            VirtualFolderType type = (VirtualFolderType) virtualFolderType;

            previewImagePagerAdapter = new PreviewImagePagerAdapter(this,
                                                                    type,
                                                                    user,
                                                                    getStorageManager());
        } else {
            // get parent from path
            OCFile parentFolder = getStorageManager().getFileById(getFile().getParentId());

            if (parentFolder == null) {
                // should not be necessary
                parentFolder = getStorageManager().getFileByEncryptedRemotePath(OCFile.ROOT_PATH);
            }

            previewImagePagerAdapter = new PreviewImagePagerAdapter(
                this,
                livePhotoFile,
                parentFolder,
                user,
                getStorageManager(),
                MainApp.isOnlyOnDevice(),
                preferences
            );
        }

        viewPager = findViewById(R.id.fragmentPager);

        int position = hasSavedPosition ? savedPosition : previewImagePagerAdapter.getFilePosition(getFile());
        position = Math.max(position, 0);

        viewPager.setAdapter(previewImagePagerAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectPage(position);
            }
        });
        viewPager.setCurrentItem(position, false);

        if (position == 0 && !getFile().isDown()) {
            // this is necessary because mViewPager.setCurrentItem(0) just after setting the
            // adapter does not result in a call to #onPageSelected(0)
            requestWaitingForBinder = true;
        }
    }

    @Override
    public void onBackPressed() {
        sendRefreshSearchEventBroadcast();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            sendRefreshSearchEventBroadcast();

            if (isDrawerOpen()) {
                closeDrawer();
            } else {
                backToDisplayActivity();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void sendRefreshSearchEventBroadcast() {
        Intent intent = new Intent(GalleryFragment.REFRESH_SEARCH_EVENT_RECEIVER);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        Optional<User> optionalUser = getUser();
        if (optionalUser.isPresent()) {
            OCFile file = getFile();
            /// Validate handled file (first image to preview)
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (!MimeTypeUtil.isImage(file)) {
                throw new IllegalArgumentException("Non-image file passed as argument");
            }

            // Update file according to DB file, if it is possible
            if (file.getFileId() > FileDataStorageManager.ROOT_PARENT_ID) {
                file = getStorageManager().getFileById(file.getFileId());
            }

            if (file != null) {
                /// Refresh the activity according to the Account and OCFile set
                setFile(file);  // reset after getting it fresh from storageManager
                // NMC Customization
                updateActionBarTitleAndHomeButton(getFile());
                //if (!stateWasRecovered) {
                initViewPager(optionalUser.get());
                //}

            } else {
                // handled file not in the current Account
                finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WAITING_FOR_BINDER, requestWaitingForBinder);
        outState.putBoolean(KEY_SYSTEM_VISIBLE, isSystemUIVisible());
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (operation instanceof RemoveFileOperation) {
            int deletePosition = viewPager.getCurrentItem();
            int nextPosition = deletePosition > 0 ? deletePosition - 1 : 0;

            if (previewImagePagerAdapter.getItemCount() <= 1) {
                finish();
                return;
            }

            viewPager.setCurrentItem(nextPosition, true);
            previewImagePagerAdapter.delete(deletePosition);
        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish(result);
        }
    }

    private void onSynchronizeFileOperationFinish(RemoteOperationResult result) {
        if (result.isSuccess()) {
            supportInvalidateOptionsMenu();
        }
    }

    private void observeWorkerState() {
        WorkerStateLiveData.Companion.instance().observe(this, state -> {
            if (state instanceof WorkerState.Download) {
                Log_OC.d(TAG, "Download worker started");
                isDownloadWorkStarted = true;

                if (requestWaitingForBinder) {
                    requestWaitingForBinder = false;
                    Log_OC.d(TAG, "Simulating reselection of current page after connection " +
                        "of download binder");
                    selectPage(viewPager.getCurrentItem());
                }
            } else {
                Log_OC.d(TAG, "Download worker stopped");
                isDownloadWorkStarted = false;
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        downloadFinishReceiver = new DownloadFinishReceiver();
        IntentFilter downloadIntentFilter = new IntentFilter(FileDownloadWorker.Companion.getDownloadFinishMessage());
        localBroadcastManager.registerReceiver(downloadFinishReceiver, downloadIntentFilter);

        UploadFinishReceiver uploadFinishReceiver = new UploadFinishReceiver();
        IntentFilter uploadIntentFilter = new IntentFilter(FileUploadWorker.Companion.getUploadFinishMessage());
        localBroadcastManager.registerReceiver(uploadFinishReceiver, uploadIntentFilter);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    public void onPause() {
        if (downloadFinishReceiver != null){
            localBroadcastManager.unregisterReceiver(downloadFinishReceiver);
            downloadFinishReceiver = null;
        }

        super.onPause();
    }


    private void backToDisplayActivity() {
        finish();
    }

    @SuppressFBWarnings("DLS")
    @Override
    public void showDetails(OCFile file) {
        final Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        showDetailsIntent.setAction(FileDisplayActivity.ACTION_DETAILS);
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, file);
        showDetailsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(showDetailsIntent);
        finish();
    }

    @Override
    public void showDetails(OCFile file, int activeTab) {
        showDetails(file);
    }

    public void requestForDownload(OCFile file) {
        requestForDownload(file, null);
    }

    public void requestForDownload(OCFile file, String downloadBehaviour) {
        final User user = getUser().orElseThrow(RuntimeException::new);
        FileDownloadHelper.Companion.instance().downloadFileIfNotStartedBefore(user, file);
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily
     * complete.
     *
     *  @param  position        Position index of the new selected page
     */
    public void selectPage(int position) {
        savedPosition = position;
        hasSavedPosition = true;

        OCFile currentFile = previewImagePagerAdapter.getFileAt(position);

        if (!isDownloadWorkStarted) {
            requestWaitingForBinder = true;
        } else {
            if (currentFile != null) {
                if (currentFile.isEncrypted() && !currentFile.isDown() &&
                    !previewImagePagerAdapter.pendingErrorAt(position)) {
                    requestForDownload(currentFile);
                }

                // Call to reset image zoom to initial state
                // ((PreviewImagePagerAdapter) mViewPager.getAdapter()).resetZoom();
            }
        }

        // Update ActionBar title
        if (currentFile != null) {
            // NMC Customization
            updateActionBarTitleAndHomeButton(currentFile);
            setDrawerIndicatorEnabled(false);
        }
    }

    /**
     * Class waiting for broadcast events from the {@link FileDownloadWorker} service.
     * <p>
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * folder displayed in the gallery.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            previewNewImage(intent);
        }
    }

    private class UploadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            previewNewImage(intent);
        }
    }

    private void previewNewImage(Intent intent) {
        String accountName = intent.getStringExtra(FileDownloadWorker.EXTRA_ACCOUNT_NAME);
        String downloadedRemotePath = intent.getStringExtra(FileDownloadWorker.EXTRA_REMOTE_PATH);
        String downloadBehaviour = intent.getStringExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR);

        if (getAccount().name.equals(accountName) && downloadedRemotePath != null) {
            OCFile file = getStorageManager().getFileByEncryptedRemotePath(downloadedRemotePath);
            boolean downloadWasFine = intent.getBooleanExtra(FileDownloadWorker.EXTRA_DOWNLOAD_RESULT, false);

            if (EditImageActivity.OPEN_IMAGE_EDITOR.equals(downloadBehaviour)) {
                startImageEditor(file);
            } else {
                int position = previewImagePagerAdapter.getFilePosition(file);
                if (position >= 0) {
                    if (downloadWasFine) {
                        previewImagePagerAdapter.updateFile(position, file);
                    } else {
                        previewImagePagerAdapter.updateWithDownloadError(position);
                    }
                    previewImagePagerAdapter.notifyItemChanged(position);
                } else if (downloadWasFine) {
                    Optional<User> user = getUser();

                    if (user.isPresent()) {
                        initViewPager(user.get());
                        int newPosition = previewImagePagerAdapter.getFilePosition(file);
                        if (newPosition >= 0) {
                            viewPager.setCurrentItem(newPosition);
                        }
                    }
                }
            }
        }
    }

    public boolean isSystemUIVisible() {
        return getSupportActionBar() == null || getSupportActionBar().isShowing();
    }

    public void toggleFullScreen() {
        // do nothing for NMC
    }

    public void startImageEditor(OCFile file) {
        if (file.isDown()) {
            Intent editImageIntent = new Intent(this, EditImageActivity.class);
            editImageIntent.putExtra(EditImageActivity.EXTRA_FILE, file);
            startActivity(editImageIntent);
        } else {
            requestForDownload(file, EditImageActivity.OPEN_IMAGE_EDITOR);
        }
    }

    @Override
    public void onBrowsedDownTo(OCFile folder) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading) {
        // TODO Auto-generated method stub

    }
}
