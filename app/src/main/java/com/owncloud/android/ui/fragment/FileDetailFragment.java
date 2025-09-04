/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2011 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.download.FileDownloadHelper;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.model.WorkerState;
import com.nextcloud.model.WorkerStateLiveData;
import com.nextcloud.ui.fileactions.FileAction;
import com.nextcloud.utils.EditorUtils;
import com.nextcloud.ui.fileactions.FileActionsBottomSheet;
import com.nextcloud.utils.MenuUtils;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsFragmentBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ToggleFavoriteRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.tags.Tag;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.adapter.progressListener.DownloadProgressListener;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.dialog.SendShareDialog;
import com.owncloud.android.ui.events.FavoriteEvent;
import com.owncloud.android.ui.events.ShareSearchViewFocusEvent;
import com.owncloud.android.ui.fragment.util.SharePermissionManager;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Objects;

import javax.inject.Inject;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;

/**
 * This Fragment is used to display the details about a file.
 */
public class FileDetailFragment extends FileFragment implements OnClickListener, Injectable {
    private static final String TAG = FileDetailFragment.class.getSimpleName();
    private static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";
    static final String FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT";
    private static final String FTAG_SHARING = "SHARING_DETAILS_FRAGMENT";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";
    private static final String ARG_USER = "USER";
    private static final String ARG_ACTIVE_TAB = "TAB";
    private View view;
    private User user;
    private OCFile parentFolder;
    private boolean previewLoaded;

    private FileDetailsFragmentBinding binding;
    private DownloadProgressListener progressListener;
    private ToolbarActivity toolbarActivity;
    private int activeTab;

    @Inject AppPreferences preferences;
    @Inject ConnectivityService connectivityService;
    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    @Inject FileDataStorageManager storageManager;
    @Inject ViewThemeUtils viewThemeUtils;
    @Inject BackgroundJobManager backgroundJobManager;
    @Inject EditorUtils editorUtils;
    @Inject SyncedFolderProvider syncedFolderProvider;

    /**
     * Public factory method to create new FileDetailFragment instances.
     * <p>
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     *
     * @param fileToDetail An {@link OCFile} to show in the fragment
     * @param user         Currently active user
     * @return New fragment with arguments set
     */
    public static FileDetailFragment newInstance(OCFile fileToDetail, OCFile parentFolder, User user) {
        FileDetailFragment frag = new FileDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToDetail);
        args.putParcelable(ARG_PARENT_FOLDER, parentFolder);
        args.putParcelable(ARG_USER, user);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Public factory method to create new FileDetailFragment instances.
     * <p>
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     *
     * @param fileToDetail An {@link OCFile} to show in the fragment
     * @param user         Currently active user
     * @param activeTab    to be active tab
     * @return New fragment with arguments set
     */
    public static FileDetailFragment newInstance(OCFile fileToDetail, User user, int activeTab) {
        FileDetailFragment frag = new FileDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToDetail);
        args.putParcelable(ARG_USER, user);
        args.putInt(ARG_ACTIVE_TAB, activeTab);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates an empty details fragment.
     *
     * It's necessary to keep a public constructor without parameters; the system uses it when tries
     * to reinstate a fragment automatically.
     */
    public FileDetailFragment() {
        super();
        user = null;
        progressListener = null;
    }

    /**
     * return the reference to the file detail sharing fragment to communicate with it.
     *
     * @return reference to the {@link FileDetailSharingFragment}
     */
    public FileDetailSharingFragment getFileDetailSharingFragment() {
        if (binding == null) {
            return null;
        }
        return (FileDetailSharingFragment)requireActivity().getSupportFragmentManager().findFragmentByTag(FTAG_SHARING);
    }

    /**
     * return the reference to the file detail activity fragment to communicate with it.
     *
     * @return reference to the {@link FileDetailActivitiesFragment}
     */
    public FileDetailActivitiesFragment getFileDetailActivitiesFragment() {
        return null;
    }

    public void goBackToOCFileListFragment() {
        requireActivity().onBackPressed();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle arguments = getArguments();

        if (arguments == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }

        if (getActivity() instanceof DrawerActivity drawerActivity) {
            drawerActivity.showBottomNavigationBar(false);
        }

        setFile(BundleExtensionsKt.getParcelableArgument(arguments, ARG_FILE, OCFile.class));
        parentFolder = BundleExtensionsKt.getParcelableArgument(arguments, ARG_PARENT_FOLDER, OCFile.class);
        user = BundleExtensionsKt.getParcelableArgument(arguments, ARG_USER, User.class);
        activeTab = arguments.getInt(ARG_ACTIVE_TAB, 0);

        if (savedInstanceState != null) {
            setFile(BundleExtensionsKt.getParcelableArgument(savedInstanceState, ARG_FILE, OCFile.class));
            user = BundleExtensionsKt.getParcelableArgument(savedInstanceState, ARG_USER, User.class);
        }

        binding = FileDetailsFragmentBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        if (getFile() == null || user == null) {
            showEmptyContent();
        } else {
            binding.emptyList.emptyListView.setVisibility(View.GONE);
        }

        Context context = getContext();
        if (context == null) {
            return null;
        }

        if (getFile().getTags().isEmpty()) {
            binding.tagsGroup.setVisibility(View.GONE);
        } else {
            for (Tag tag : getFile().getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag.getName());
                chip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.bg_default,
                                                                                           context.getTheme())));
                chip.setShapeAppearanceModel(chip.getShapeAppearanceModel().toBuilder().setAllCornerSizes((100.0f))
                                                 .build());
                chip.setEnsureMinTouchTargetSize(false);
                chip.setClickable(false);
                viewThemeUtils.material.themeChipSuggestion(chip);

                if (tag.getColor() != null) {
                    int color = Color.parseColor(tag.getColor());
                    chip.setChipStrokeColor(ColorStateList.valueOf(color));
                    chip.setTextColor(color);
                }

                binding.tagsGroup.addView(chip);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getFile() != null && user != null) {
            viewThemeUtils.platform.themeHorizontalProgressBar(binding.progressBar);
            progressListener = new DownloadProgressListener(binding.progressBar);
            binding.cancelBtn.setOnClickListener(this);
            binding.favorite.setOnClickListener(this);
            binding.overflowMenu.setOnClickListener(this);
            binding.lastModificationTimestamp.setOnClickListener(this);
            binding.folderSyncButton.setOnClickListener(this);

            updateFileDetails(false, false);
        }
    }

    private void replaceSharingFragment() {
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.sharing_frame_container,
                     FileDetailSharingFragment.newInstance(getFile(), user),
                     FTAG_SHARING).commit();
    }

    private void onOverflowIconClicked() {
        final OCFile file = getFile();
        final var additionalFilter = FileAction.Companion.getFileDetailActions(file);
        final FragmentManager fragmentManager = getChildFragmentManager();
        FileActionsBottomSheet.newInstance(file, true, additionalFilter)
            .setResultListener(fragmentManager, this, this::optionsItemSelected)
            .show(fragmentManager, "actions");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        FileExtensionsKt.logFileSize(getFile(), TAG);
        outState.putParcelable(ARG_FILE, getFile());
        outState.putParcelable(ARG_USER, user);
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForTransferProgress();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (toolbarActivity != null) {
            if (previewLoaded) {
                toolbarActivity.setPreviewImageVisibility(true);
            }
        }

    }

    @Override
    public void onStop() {
        leaveTransferProgress();

        if (toolbarActivity != null) {
            toolbarActivity.hidePreviewImage();
        }

        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ToolbarActivity) {
            toolbarActivity = (ToolbarActivity) context;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public View getView() {
        return super.getView() == null ? view : super.getView();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuUtils.hideAll(menu);
    }

    private void optionsItemSelected(@IdRes final int itemId) {
        if (itemId == R.id.action_send_file) {
            containerActivity.getFileOperationsHelper().sendShareFile(getFile(), true);
        } else if (itemId == R.id.action_open_file_with) {
            containerActivity.getFileOperationsHelper().openFile(getFile());
        } else if (itemId == R.id.action_remove_file) {
            RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
            dialog.show(getFragmentManager(), FTAG_CONFIRMATION);
        } else if (itemId == R.id.action_rename_file) {
            RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(getFile(), parentFolder);
            dialog.show(getFragmentManager(), FTAG_RENAME_FILE);
        } else if (itemId == R.id.action_cancel_sync) {
            ((FileDisplayActivity) containerActivity).cancelTransference(getFile());
        } else if (itemId == R.id.action_download_file || itemId == R.id.action_sync_file) {
            containerActivity.getFileOperationsHelper().syncFile(getFile());
        } else if (itemId == R.id.action_export_file) {
            ArrayList<OCFile> list = new ArrayList<>();
            list.add(getFile());
            containerActivity.getFileOperationsHelper().exportFiles(list,
                                                                    getContext(),
                                                                    getView(),
                                                                    backgroundJobManager);
        } else if (itemId == R.id.action_set_as_wallpaper) {
            containerActivity.getFileOperationsHelper().setPictureAs(getFile(), getView());
        } else if (itemId == R.id.action_retry) {
            backgroundJobManager.startOfflineOperations();
        } else if (itemId == R.id.action_encrypted) {
            // TODO implement or remove
        } else if (itemId == R.id.action_unset_encrypted) {// TODO implement or remove
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.cancelBtn) {
            ((FileDisplayActivity) containerActivity).cancelTransference(getFile());
        } else if (id == R.id.favorite) {
            containerActivity.getFileOperationsHelper().toggleFavoriteFile(getFile(), !getFile().isFavorite());
            setFavoriteIconStatus(!getFile().isFavorite());
        } else if (id == R.id.overflow_menu) {
            onOverflowIconClicked();
        } else if (id == R.id.last_modification_timestamp) {
            boolean showDetailedTimestamp = !preferences.isShowDetailedTimestampEnabled();
            preferences.setShowDetailedTimestampEnabled(showDetailedTimestamp);
            setFileModificationTimestamp(getFile(), showDetailedTimestamp);
        } else if (id == R.id.folder_sync_button) {
            if (binding.folderSyncButton.isChecked()) {
                getFile().setInternalFolderSyncTimestamp(0L);
            } else {
                getFile().setInternalFolderSyncTimestamp(-1L);
            }

            storageManager.saveFile(getFile());
        } else {
            Log_OC.e(TAG, "Incorrect view clicked!");
        }
    }

    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be
     * replaced.
     *
     * @return True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return getFile() == null || user == null;
    }

    /**
     * Use this method to signal this Activity that it shall update its view.
     *
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, User user) {
        setFile(file);
        this.user = user;
        updateFileDetails(false, false);
    }

    /**
     * Updates the view with all relevant details about that file.
     * <p>
     * TODO Remove parameter when the transferring state of files is kept in database.
     *
     * @param transferring Flag signaling if the file should be considered as downloading or uploading, although
     *                     {@link FileDownloadHelper#isDownloading(User, OCFile)}  and
     *                     {@link FileUploadHelper#isUploading(User, OCFile)} return false.
     * @param refresh      If 'true', try to refresh the whole file from the database
     */
    public void updateFileDetails(boolean transferring, boolean refresh) {
        if (readyToShow()) {
            FileDataStorageManager storageManager = containerActivity.getStorageManager();

            if (storageManager == null) {
                return;
            }

            if (refresh) {
                setFile(storageManager.getFileByPath(getFile().getRemotePath()));
            }
            OCFile file = getFile();

            // set file details
            if (MimeTypeUtil.isImage(file)) {
                binding.filename.setText(file.getFileName());
            } else {
                binding.filename.setVisibility(View.GONE);
            }
            binding.size.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));

            boolean showDetailedTimestamp = preferences.isShowDetailedTimestampEnabled();
            setFileModificationTimestamp(file, showDetailedTimestamp);

            setFilePreview(file);
            setFavoriteIconStatus(file.isFavorite());

            // configure UI for depending upon local state of the file
            if (transferring
                || (FileDownloadHelper.Companion.instance().isDownloading(user, file))
                || (FileUploadHelper.Companion.instance().isUploading(user, file))) {
                setButtonsForTransferring();

            } else if (file.isDown()) {

                setButtonsForDown();

            } else {
                // TODO load default preview image; when the local file is removed, the preview
                // remains there
                setButtonsForRemote();
            }

            FloatingActionButton fabMain = requireActivity().findViewById(R.id.fab_main);
            if (fabMain != null) {
                fabMain.hide();
            }

            // NMC: not required
            /* binding.syncBlock.setVisibility(file.isFolder() ? View.VISIBLE : View.GONE);

            if (file.isInternalFolderSync()) {
                binding.folderSyncButton.setChecked(file.isInternalFolderSync());
            } else {
                if (storageManager.isPartOfInternalTwoWaySync(file)) {
                    binding.folderSyncButton.setChecked(true);
                    binding.folderSyncButton.setEnabled(false);
                }
            }*/
        }

        // TODO: 06/21/23 remove this condition after Comments section included
        if (SendShareDialog.isPeopleShareClicked) {
            replaceSharingFragment();
        }
        final var view = getView();
        if (view != null) {
            view.invalidate();
        }

        observeWorkerState();
    }

    private void observeWorkerState() {
        WorkerStateLiveData.Companion.instance().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof WorkerState.DownloadStarted downloadState) {
                updateProgressBar(downloadState);
            } else if (state instanceof WorkerState.UploadStarted) {
                binding.progressText.setText(R.string.uploader_upload_in_progress_ticker);
            } else {
                binding.progressBlock.setVisibility(View.GONE);
            }
        });
    }

    private void updateProgressBar(WorkerState.DownloadStarted downloadState) {
        if (binding.progressBlock.getVisibility() != View.VISIBLE) {
            binding.progressBlock.setVisibility(View.VISIBLE);
        }

        binding.progressText.setText(R.string.downloader_download_in_progress_ticker);
        binding.progressBar.setProgress(downloadState.getPercent());
        binding.progressBar.invalidate();
    }

    private void setFileModificationTimestamp(OCFile file, boolean showDetailedTimestamp) {
        if (showDetailedTimestamp) {
            binding.lastModificationTimestamp.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
        } else {
            binding.lastModificationTimestamp.setText(DisplayUtils.getRelativeTimestamp(getContext(),
                                                                                        file.getModificationTimestamp()));
        }
    }

    private void setFavoriteIconStatus(boolean isFavorite) {
        if (isFavorite) {
            binding.favorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.favorite, null));
            binding.favorite.setContentDescription(getString(R.string.unset_favorite));
        } else {
            binding.favorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                                                                          R.drawable.ic_star_outline,
                                                                          null));
            binding.favorite.setContentDescription(getString(R.string.favorite));

            //NMC Customization
            binding.favorite.getDrawable().mutate().setColorFilter(requireContext()
                                                                       .getResources()
                                                                       .getColor(R.color.list_item_lastmod_and_filesize_text, null),
                                                                   PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * Checks if the fragment is ready to show details of a OCFile
     *
     * @return 'True' when the fragment is ready to show details of a file
     */
    private boolean readyToShow() {
        return getFile() != null && user != null;
    }

    /**
     * Updates the file preview if possible
     *
     * @param file a {@link OCFile} to be previewed
     */
    private void setFilePreview(OCFile file) {
        Bitmap resizedImage;

        if (toolbarActivity != null && MimeTypeUtil.isImage(file)) {
            String tagId = ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + getFile().getRemoteId();
            resizedImage = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId);

            if (resizedImage != null && !file.isUpdateThumbnailNeeded()) {
                toolbarActivity.setPreviewImageBitmap(resizedImage);
                previewLoaded = true;
            } else {
                // show thumbnail while loading resized image
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + getFile().getRemoteId());

                if (thumbnail != null) {
                    toolbarActivity.setPreviewImageBitmap(thumbnail);
                } else {
                    thumbnail = ThumbnailsCacheManager.mDefaultImg;
                }

                // generate new resized image
                if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(getFile(), toolbarActivity.getPreviewImageView()) &&
                    containerActivity.getStorageManager() != null) {
                    final ThumbnailsCacheManager.ResizedImageGenerationTask task =
                        new ThumbnailsCacheManager.ResizedImageGenerationTask(this,
                                                                              toolbarActivity.getPreviewImageView(),
                                                                              toolbarActivity.getPreviewImageContainer(),
                                                                              containerActivity.getStorageManager(),
                                                                              connectivityService,
                                                                              containerActivity.getStorageManager().getUser(),
                                                                              getResources().getColor(R.color.background_color_inverse,
                                                                                                      requireContext().getTheme())
                        );

                    if (resizedImage == null) {
                        resizedImage = thumbnail;
                    }

                    final ThumbnailsCacheManager.AsyncResizedImageDrawable asyncDrawable =
                        new ThumbnailsCacheManager.AsyncResizedImageDrawable(
                            MainApp.getAppContext().getResources(),
                            resizedImage,
                            task
                        );

                    toolbarActivity.setPreviewImageDrawable(asyncDrawable);
                    previewLoaded = true;
                    task.execute(getFile());
                }
            }
        } else {
            previewLoaded = false;
        }
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            // show the progress bar for the transfer
            binding.progressBlock.setVisibility(View.VISIBLE);
            binding.progressText.setVisibility(View.VISIBLE);
            if (FileDownloadHelper.Companion.instance().isDownloading(user, getFile())) {
                binding.progressText.setText(R.string.downloader_download_in_progress_ticker);
            } else {
                if (FileUploadHelper.Companion.instance().isUploading(user, getFile())) {
                    binding.progressText.setText(R.string.uploader_upload_in_progress_ticker);
                }
            }
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private void setButtonsForDown() {
        if (!isEmpty()) {
            // hides the progress bar
            binding.progressBlock.setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            // hides the progress bar
            binding.progressBlock.setVisibility(View.GONE);
        }
    }

    public void listenForTransferProgress() {
        if (progressListener == null) {
            Log_OC.d(TAG, "progressListener == null");
            return;
        }

        if (containerActivity.getFileDownloadProgressListener() != null) {
            containerActivity.getFileDownloadProgressListener().
                addDataTransferProgressListener(progressListener, getFile());
        }

        if (containerActivity.getFileUploaderHelper() != null) {
            OCFile file = getFile();
            if (user == null || file == null) {
                return;
            }

            String targetKey = FileUploadHelper.Companion.buildRemoteName(user.getAccountName(), file.getRemotePath());
            containerActivity.getFileUploaderHelper().addUploadTransferProgressListener(progressListener, targetKey);
        }
    }

    private void leaveTransferProgress() {
        if (progressListener != null) {
            if (containerActivity.getFileDownloadProgressListener() != null) {
                containerActivity.getFileDownloadProgressListener().
                    removeDataTransferProgressListener(progressListener, getFile());
            }
            if (containerActivity.getFileUploaderHelper() != null) {
                OCFile file = getFile();

                if (user == null || file == null) {
                    return;
                }

                String targetKey = FileUploadHelper.Companion.buildRemoteName(user.getAccountName(), file.getRemotePath());
                containerActivity.getFileUploaderHelper().removeUploadTransferProgressListener(progressListener, targetKey);
            }
        }
    }

    private void showEmptyContent() {
        binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
        binding.detailContainer.setVisibility(View.GONE);

        binding.emptyList.emptyListViewHeadline.setText(R.string.file_details_no_content);

        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_list_empty_error);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
    }

    /**
     * open the sharing process fragment for creating new share
     *
     */
    public void initiateSharingProcess(String shareeName,
                                       ShareType shareType,
                                       boolean secureShare) {
        if (getFile() == null) {
            DisplayUtils.showSnackMessage(requireView(), R.string.file_not_found_cannot_share);
            return;
        }

        final var file = getFile();
        if (Objects.equals(file.getOwnerId(), shareeName)) {
            DisplayUtils.showSnackMessage(requireView(), R.string.file_detail_share_already_active);
            return;
        }

        final var fileShareDetailFragment = FileDetailsSharingProcessFragment.newInstance(file, shareeName, shareType, secureShare, SharePermissionManager.canEditFile(user, storageManager.getCapability(user), getFile(), editorUtils));

        requireActivity()
            .getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.sharing_frame_container, fileShareDetailFragment, FileDetailsSharingProcessFragment.TAG)
            .addToBackStack(null)
            .commit();
    }

    /**
     * open the new sharing screen process to modify the created share
     *
     * @param share
     * @param screenTypePermission
     * @param isReshareShown
     * @param isExpiryDateShown
     */
    public void editExistingShare(OCShare share, int screenTypePermission, boolean isReshareShown,
                                  boolean isExpiryDateShown) {
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.sharing_frame_container,
                                                                             FileDetailsSharingProcessFragment.newInstance(share, screenTypePermission, isReshareShown,
                                                                                                                           isExpiryDateShown,
                                                                                                                           SharePermissionManager.canEditFile(user, storageManager.getCapability(user), getFile(), editorUtils)),
                                                                             FileDetailsSharingProcessFragment.TAG)
            .addToBackStack(null)
            .commit();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(FavoriteEvent event) {
        try {
            User user = accountManager.getUser();
            OwnCloudClient client = clientFactory.create(user);

            ToggleFavoriteRemoteOperation toggleFavoriteOperation = new ToggleFavoriteRemoteOperation(
                event.getShouldFavorite(), event.getRemotePath());
            RemoteOperationResult remoteOperationResult = toggleFavoriteOperation.execute(client);

            if (remoteOperationResult.isSuccess()) {
                getFile().setFavorite(event.getShouldFavorite());
                OCFile file = storageManager.getFileByEncryptedRemotePath(event.getRemotePath());
                file.setFavorite(event.getShouldFavorite());
                storageManager.saveFile(file);
            }

        } catch (ClientFactory.CreationException e) {
            Log_OC.e(TAG, "Error processing event", e);
        }
    }

    /**
     * hide the view for landscape mode to have more space for the user to type in search view
     * {@link FileDetailSharingFragment#scrollToSearchViewPosition(boolean)}
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ShareSearchViewFocusEvent event) {
        binding.shareDetailFileContainer.setVisibility(event.getHasFocus() ? View.GONE : View.VISIBLE);
    }
}
