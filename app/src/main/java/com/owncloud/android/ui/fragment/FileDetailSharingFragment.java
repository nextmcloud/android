/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * @author TSI-mc
 *
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2023 TSI-mc
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.fragment;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.database.entity.FileEntity;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.utils.EditorUtils;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.nmc.android.utils.SearchViewThemeUtils;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsSharingFragmentBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.NextcloudVersion;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.providers.UsersAndGroupsSearchConfig;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.ShareeListAdapter;
import com.owncloud.android.ui.adapter.ShareeListAdapterListener;
import com.owncloud.android.ui.asynctasks.RetrieveHoverCardAsyncTask;
import com.nextcloud.client.preferences.AppPreferences;
import com.nmc.android.marketTracking.AdjustSdkUtils;
import com.nmc.android.marketTracking.TealiumSdkUtils;
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment;
import com.owncloud.android.ui.fragment.share.RemoteShareRepository;
import com.owncloud.android.ui.fragment.share.ShareRepository;
import com.owncloud.android.ui.events.ShareSearchViewFocusEvent;
import com.owncloud.android.ui.fragment.util.FileDetailSharingFragmentHelper;
import com.owncloud.android.ui.fragment.util.SharePermissionManager;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.ClipboardUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kotlin.Unit;

public class FileDetailSharingFragment extends Fragment implements ShareeListAdapterListener,
    DisplayUtils.AvatarGenerationListener,
    Injectable, FileDetailsSharingMenuBottomSheetActions, QuickSharingPermissionsBottomSheetDialog.QuickPermissionSharingBottomSheetActions {

    private static final String TAG = "FileDetailSharingFragment";
    private static final String ARG_FILE = "FILE";
    private static final String ARG_USER = "USER";

    private OCFile file;
    private User user;
    private OCCapability capabilities;

    private FileOperationsHelper fileOperationsHelper;
    private FileActivity fileActivity;
    private FileDataStorageManager fileDataStorageManager;

    private FileDetailsSharingFragmentBinding binding;

    private OnEditShareListener onEditShareListener;

    private ShareeListAdapter linkShareeListAdapter;

    private ShareeListAdapter emailShareeListAdapter;

    private boolean isSearchViewFocused;

    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    @Inject EditorUtils editorUtils;
    @Inject ViewThemeUtils viewThemeUtils;
    @Inject UsersAndGroupsSearchConfig searchConfig;
    @Inject AppPreferences appPreferences;

    public static FileDetailSharingFragment newInstance(OCFile file, User user) {
        FileDetailSharingFragment fragment = new FileDetailSharingFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            file = BundleExtensionsKt.getParcelableArgument(savedInstanceState, ARG_FILE, OCFile.class);
            user = BundleExtensionsKt.getParcelableArgument(savedInstanceState, ARG_USER, User.class);
        } else {
            Bundle arguments = getArguments();

            if (arguments != null) {
                file = BundleExtensionsKt.getParcelableArgument(arguments, ARG_FILE, OCFile.class);
                user = BundleExtensionsKt.getParcelableArgument(arguments, ARG_USER, User.class);
            }
        }

        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }

        if (user == null) {
            throw new IllegalArgumentException("Account may not be null");
        }

        fileActivity = (FileActivity) getActivity();

        if (fileActivity == null) {
            throw new IllegalArgumentException("FileActivity may not be null");
        }

        fileDataStorageManager = fileActivity.getStorageManager();
    }

    private void fetchSharees() {
        ShareRepository shareRepository = new RemoteShareRepository(fileActivity.getClientRepository(), fileActivity, fileDataStorageManager);
        shareRepository.fetchSharees(file.getRemotePath(), () -> {
            refreshCapabilitiesFromDB();
            refreshSharesFromDB();
            showShareContainer();
            return Unit.INSTANCE;
        }, () -> {
            showShareContainer();
            DisplayUtils.showSnackMessage(getView(), R.string.error_fetching_sharees);
            return Unit.INSTANCE;
        });
    }

    private void showShareContainer() {
        if (binding == null) {
            return;
        }

        final LinearLayout shimmerLayout = binding.shimmerLayout.getRoot();
        shimmerLayout.clearAnimation();
        shimmerLayout.setVisibility(View.GONE);

        binding.shareContainer.setVisibility(View.VISIBLE);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FileDetailsSharingFragmentBinding.inflate(inflater, container, false);

        // NMC: call fetch shares here because we are replacing the fragments
        // instead of adding them where we have to fetch shares again.
        fetchSharees();
        final Animation blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink);
        binding.shimmerLayout.getRoot().startAnimation(blinkAnimation);

        fileOperationsHelper = fileActivity.getFileOperationsHelper();

        AccountManager accountManager = AccountManager.get(requireContext());
        String userId = accountManager.getUserData(user.toPlatformAccount(),
                                                   com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

        linkShareeListAdapter = new ShareeListAdapter(fileActivity,
                                                      new ArrayList<>(),
                                                      this,
                                                      userId,
                                                      user,
                                                      viewThemeUtils,
                                                      file.isEncrypted());
        linkShareeListAdapter.setHasStableIds(true);

        binding.linkSharesList.setAdapter(linkShareeListAdapter);

        binding.linkSharesList.setLayoutManager(new LinearLayoutManager(requireContext()));

        emailShareeListAdapter = new ShareeListAdapter(fileActivity,
                                                       new ArrayList<>(),
                                                       this,
                                                       userId,
                                                       user,
                                                       viewThemeUtils,
                                                       file.isEncrypted());
        emailShareeListAdapter.setHasStableIds(true);

        binding.sharesList.setAdapter(emailShareeListAdapter);

        binding.sharesList.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.pickContactEmailBtn.setOnClickListener(v -> checkContactPermission());

        binding.shareCreateNewLink.setOnClickListener(v -> createPublicShareLink());

        //remove focus from search view on click of root view
        binding.shareContainer.setOnClickListener(v -> binding.searchView.clearFocus());

        //enable-disable scrollview scrolling
        binding.fileDetailsNestedScrollView.setOnTouchListener((view1, motionEvent) -> {
            //true means disable the scrolling and false means enable the scrolling
            return com.nmc.android.utils.DisplayUtils.isLandscapeOrientation() && isSearchViewFocused;
        });

        setupView();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof FileActivity)) {
            throw new IllegalArgumentException("Calling activity must be of type FileActivity");
        }

        try {
            onEditShareListener = (OnEditShareListener) context;
        } catch (Exception e) {
            throw new IllegalArgumentException("Calling activity must implement the interface" + e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        searchConfig.setSearchOnlyUsers(file.isEncrypted());
    }

    @Override
    public void onStop() {
        super.onStop();
        searchConfig.reset();
    }

    private void setupView() {
        setShareWithYou();
    }

    private void setUpSearchView() {
        FileDetailSharingFragmentHelper.setupSearchView(
            (SearchManager) fileActivity.getSystemService(Context.SEARCH_SERVICE),
            binding.searchView,
            fileActivity.getComponentName());

        SearchViewThemeUtils.INSTANCE.themeSearchView(requireContext(), binding.searchView);

        binding.searchView.setQueryHint(getResources().getString(R.string.share_search));
        binding.searchView.setVisibility(View.VISIBLE);
        binding.pickContactEmailBtn.setVisibility(View.VISIBLE);

        binding.searchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            isSearchViewFocused = hasFocus;
            scrollToSearchViewPosition(false);
        });

    }

    /**
     * @param isDeviceRotated true when user rotated the device and false when user is already in landscape mode
     */
    private void scrollToSearchViewPosition(boolean isDeviceRotated) {
        if (com.nmc.android.utils.DisplayUtils.isLandscapeOrientation()) {
            if (isSearchViewFocused) {
                binding.fileDetailsNestedScrollView.post(() -> {
                    //ignore the warning because there can be case that the scrollview can be null
                    if (binding.fileDetailsNestedScrollView == null) {
                        return;
                    }

                    //need to hide app bar to have more space in landscape mode while search view is focused
                    hideAppBar();

                    //send the event to hide the share top view to have more space
                    //need to use this here else white view will be visible for sometime
                    EventBus.getDefault().post(new ShareSearchViewFocusEvent(isSearchViewFocused));

                    if (isDeviceRotated) {
                        //during the rotation we need to use getTop() method for proper alignment of search view
                        //-25 just to avoid blank space at top
                        binding.fileDetailsNestedScrollView.smoothScrollTo(0, binding.searchView.getTop() - 20);
                    } else {
                        //when user is already in landscape mode and search view gets focus
                        //we need to user getBottom() method for proper alignment of search view
                        //-100 just to avoid blank space at top
                        binding.fileDetailsNestedScrollView.smoothScrollTo(0, binding.searchView.getBottom() - 100);
                    }
                });
            } else {
                //send the event to show the share top view again
                EventBus.getDefault().post(new ShareSearchViewFocusEvent(isSearchViewFocused));
            }
        } else {
            //in portrait mode we need to see the layout everytime
            //send the event to show the share top view
            EventBus.getDefault().post(new ShareSearchViewFocusEvent(false));
        }
    }

    private void hideAppBar() {
        if (requireActivity() instanceof FileDisplayActivity) {
            AppBarLayout appBarLayout = requireActivity().findViewById(R.id.appbar);

            if (appBarLayout != null) {
                appBarLayout.setExpanded(false, true);
            }
        }
    }

    private void disableSearchView(View view) {
        view.setEnabled(false);

        if (view instanceof ViewGroup viewGroup) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableSearchView(viewGroup.getChildAt(i));
            }
        }
    }

    /**
     * will be called from FileActivity when user is sharing from PreviewImageFragment
     *
     * @param shareeName
     * @param shareType
     */
    public void initiateSharingProcess(String shareeName, ShareType shareType, boolean secureShare) {
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.share_fragment_container,
                                                                                 FileDetailsSharingProcessFragment.newInstance(file,
                                                                                                                               shareeName,
                                                                                                                               shareType,
                                                                                                                               secureShare,
                                                                                                                               SharePermissionManager.canEditFile(user, capabilities, file, editorUtils)),
                                                                                 FileDetailsSharingProcessFragment.TAG)
            .addToBackStack(null)
            .commit();
    }

    /**
     * open the new sharing screen process to modify the created share this will be called from PreviewImageFragment
     *
     * @param share
     * @param screenTypePermission
     * @param isReshareShown
     * @param isExpiryDateShown
     */
    public void editExistingShare(OCShare share, int screenTypePermission, boolean isReshareShown,
                                  boolean isExpiryDateShown) {
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.share_fragment_container,
                                                                                 FileDetailsSharingProcessFragment.newInstance(share, screenTypePermission, isReshareShown,
                                                                                                                               isExpiryDateShown, SharePermissionManager.canEditFile(user, capabilities, file, editorUtils)),
                                                                                 FileDetailsSharingProcessFragment.TAG)
            .addToBackStack(null)
            .commit();
    }

    private void setShareWithYou() {
        setUpSearchView();
        if (accountManager.userOwnsFile(file, user)) {
            binding.tvResharingInfo.setVisibility(View.GONE);
            binding.tvResharingStatus.setVisibility(View.GONE);
        } else {
            binding.tvResharingInfo.setText(
                DisplayUtils.createTextWithSpan(
                    String.format(getString(R.string.resharing_user_info), file.getOwnerDisplayName()),
                    file.getOwnerDisplayName(),
                    new StyleSpan(Typeface.BOLD)));
          /*  DisplayUtils.setAvatar(user,
                                   file.getOwnerId(),
                                   this,
                                   getResources().getDimension(
                                       R.dimen.file_list_item_avatar_icon_radius),
                                   getResources(),
                                   binding.sharedWithYouAvatar,
                                   getContext());
            binding.sharedWithYouAvatar.setVisibility(View.VISIBLE);*/

            if (file.canReshare()) {
                binding.tvResharingStatus.setText(getResources().getString(R.string.reshare_allowed));
            } else {
                binding.orSectionLayout.setVisibility(View.GONE);
                binding.linkShareSectionHeading.setVisibility(View.GONE);
                binding.linkSharesList.setVisibility(View.GONE);
                binding.shareCreateNewLink.setVisibility(View.GONE);

                binding.sharedWithDivider.setVisibility(View.GONE);
                binding.tvYourShares.setVisibility(View.GONE);
                binding.sharesList.setVisibility(View.GONE);
                binding.tvEmptyShares.setVisibility(View.GONE);

                binding.tvResharingStatus.setText(getResources().getString(R.string.reshare_not_allowed));

                disableSearchView(binding.searchContainer);
            }
            binding.tvResharingStatus.setVisibility(View.VISIBLE);
            binding.tvResharingInfo.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void copyInternalLink() {
        OwnCloudAccount account = accountManager.getCurrentOwnCloudAccount();

        if (account == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.could_not_retrieve_url));
            return;
        }

        FileDisplayActivity.showShareLinkDialog(fileActivity, file, createInternalLink(account, file));
    }

    private String createInternalLink(OwnCloudAccount account, OCFile file) {
        return account.getBaseUri() + "/index.php/f/" + file.getLocalId();
    }

    @Override
    public void createPublicShareLink() {
        if (capabilities != null && (capabilities.getFilesSharingPublicPasswordEnforced().isTrue() ||
            capabilities.getFilesSharingPublicAskForOptionalPassword().isTrue())) {
            // password enforced by server, request to the user before trying to create
            requestPasswordForShareViaLink(true,
                                           capabilities.getFilesSharingPublicAskForOptionalPassword().isTrue());

        } else {
            // create without password if not enforced by server or we don't know if enforced;
            fileOperationsHelper.shareFileViaPublicShare(file, null);
        }

        //track event on creating share link
        AdjustSdkUtils.trackEvent(AdjustSdkUtils.EVENT_TOKEN_CREATE_SHARING_LINK, appPreferences);
        TealiumSdkUtils.trackEvent(TealiumSdkUtils.EVENT_CREATE_SHARING_LINK, appPreferences);
    }

    @Override
    public void createSecureFileDrop() {
        fileOperationsHelper.shareFolderViaSecureFileDrop(file);
    }

    private void showSendLinkTo(OCShare publicShare) {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(publicShare.getShareLink())) {
                fileOperationsHelper.getFileWithLink(file, viewThemeUtils);
            } else {
                FileDisplayActivity.showShareLinkDialog(fileActivity, file, publicShare.getShareLink());
            }
        }
    }

    public void copyLink(OCShare share) {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(share.getShareLink())) {
                fileOperationsHelper.getFileWithLink(file, viewThemeUtils);
            } else {
                ClipboardUtil.copyToClipboard(requireActivity(), share.getShareLink());
            }
            // NMC: send link after copying it to clipboard
            sendLink(share);
        }
    }

    /**
     * show share action bottom sheet
     *
     * @param share
     */
    @Override
    @VisibleForTesting
    public void showSharingMenuActionSheet(OCShare share) {
        if (fileActivity != null && !fileActivity.isFinishing()) {
            new FileDetailSharingMenuBottomSheetDialog(fileActivity, this, share, viewThemeUtils, file.isEncrypted()).show();
        }
    }

    /**
     * show quick sharing permission dialog
     *
     * @param share
     */
    @Override
    public void showPermissionsDialog(OCShare share) {
        new QuickSharingPermissionsBottomSheetDialog(fileActivity, this, share, viewThemeUtils, file.isEncrypted()).show();
    }

    /**
     * Updates the UI after the result of an update operation on the edited {@link OCFile}.
     *
     * @param result {@link RemoteOperationResult} of an update on the edited {@link OCFile} sharing information.
     * @param file   the edited {@link OCFile}
     * @see #onUpdateShareInformation(RemoteOperationResult)
     */
    public void onUpdateShareInformation(RemoteOperationResult result, OCFile file) {
        this.file = file;

        onUpdateShareInformation(result);
    }

    /**
     * Updates the UI after the result of an update operation on the edited {@link OCFile}. Keeps the current {@link
     * OCFile held by this fragment}.
     *
     * @param result {@link RemoteOperationResult} of an update on the edited {@link OCFile} sharing information.
     * @see #onUpdateShareInformation(RemoteOperationResult, OCFile)
     */
    public void onUpdateShareInformation(RemoteOperationResult result) {
        if (result.isSuccess()) {
            refreshUiFromDB();
        } else {
            setupView();
        }
    }

    /**
     * Get {@link OCShare} instance from DB and updates the UI.
     */
    private void refreshUiFromDB() {
        refreshSharesFromDB();
        // Updates UI with new state
        setupView();
    }

    private void unShareWith(OCShare share) {
        fileOperationsHelper.unShareShare(file, share);
    }

    /**
     * Starts a dialog that requests a password to the user to protect a share link.
     *
     * @param createShare    When 'true', the request for password will be followed by the creation of a new public
     *                       link; when 'false', a public share is assumed to exist, and the password is bound to it.
     * @param askForPassword if true, password is optional
     */
    public void requestPasswordForShareViaLink(boolean createShare, boolean askForPassword) {
        SharePasswordDialogFragment dialog = SharePasswordDialogFragment.newInstance(file,
                                                                                     createShare,
                                                                                     askForPassword);
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT);
    }

    @Override
    public void requestPasswordForShare(OCShare share, boolean askForPassword) {
        SharePasswordDialogFragment dialog = SharePasswordDialogFragment.newInstance(share, askForPassword);
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT);
    }

    @Override
    public void showProfileBottomSheet(User user, String shareWith) {
        if (user.getServer().getVersion().isNewerOrEqual(NextcloudVersion.nextcloud_23)) {
            new RetrieveHoverCardAsyncTask(user,
                                           shareWith,
                                           fileActivity,
                                           clientFactory,
                                           viewThemeUtils).execute();
        }
    }

    /**
     * Get known server capabilities from DB
     */
    public void refreshCapabilitiesFromDB() {
        capabilities = fileDataStorageManager.getCapability(user.getAccountName());
    }

    /**
     * Get public link from the DB to fill in the "Share link" section in the UI. Takes into account server capabilities
     * before reading database.
     */
    @SuppressFBWarnings("PSC")
    public void refreshSharesFromDB() {
        // NMC-4582 NPE fix
        if (binding == null) {
            return;
        }

        OCFile newFile = fileDataStorageManager.getFileById(file.getFileId());
        if (newFile != null) {
            file = newFile;
        }

        if (emailShareeListAdapter == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.could_not_retrieve_shares));
            return;
        }
        emailShareeListAdapter.removeAll();

        /*//update flag in adapter
        adapter.setTextFile(SharePermissionManager.canEditFile(user,
                                                          capabilities, file, editorUtils));*/

        // to show share with users/groups info
        List<OCShare> shares = fileDataStorageManager.getSharesWithForAFile(file.getRemotePath(),
                                                                            user.getAccountName());

        emailShareeListAdapter.addShares(shares);

        showHideEmailShareView(shares.isEmpty());

        if (FileDetailSharingFragmentHelper.isPublicShareDisabled(capabilities) || !file.canReshare()) {
            return;
        }

        if (linkShareeListAdapter == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.could_not_retrieve_shares));
            return;
        }
        linkShareeListAdapter.removeAll();

        /*//update flag in adapter
        linkAdapter.setTextFile(SharePermissionManager.canEditFile(user,
                                                                   capabilities, file, editorUtils));*/

        // Get public share
        List<OCShare> publicShares = fileDataStorageManager.getSharesByPathAndType(file.getRemotePath(),
                                                                                   ShareType.PUBLIC_LINK,
                                                                                   "");

        linkShareeListAdapter.addShares(publicShares);

        showHideLinkShareView(publicShares == null || publicShares.isEmpty());
    }

    private void showHideLinkShareView(boolean isEmptyList) {
        binding.linkSharesList.setVisibility(isEmptyList ? View.GONE : View.VISIBLE);
    }

    private void showHideEmailShareView(boolean isEmptyList) {
        binding.sharesList.setVisibility(isEmptyList ? View.GONE : View.VISIBLE);
        // additional check to hide the empty shares if file cannot be shared
        if (!file.canReshare()) {
            binding.tvEmptyShares.setVisibility(View.GONE);
            return;
        }
        binding.tvEmptyShares.setVisibility(isEmptyList ? View.VISIBLE : View.GONE);
    }

    private void checkContactPermission() {
        if (PermissionUtil.checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS)) {
            pickContactEmail();
        } else {
            requestContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void pickContactEmail() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI);

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            onContactSelectionResultLauncher.launch(intent);
        } else {
            DisplayUtils.showSnackMessage(requireActivity(), getString(R.string.file_detail_sharing_fragment_no_contact_app_message));
        }
    }

    private void handleContactResult(@NonNull Uri contactUri) {
        // Define the projection to get all email addresses.
        String[] projection = {ContactsContract.CommonDataKinds.Email.ADDRESS};

        Cursor cursor = fileActivity.getContentResolver().query(contactUri, projection, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // The contact has only one email address, use it.
                int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                if (columnIndex != -1) {
                    // Use the email address as needed.
                    // email variable contains the selected contact's email address.
                    String email = cursor.getString(columnIndex);
                    binding.searchView.post(() -> {
                        binding.searchView.setQuery(email, false);
                        binding.searchView.requestFocus();
                    });
                } else {
                    DisplayUtils.showSnackMessage(binding.getRoot(), R.string.email_pick_failed);
                    Log_OC.e(FileDetailSharingFragment.class.getSimpleName(), "Failed to pick email address.");
                }
            } else {
                DisplayUtils.showSnackMessage(binding.getRoot(), R.string.email_pick_failed);
                Log_OC.e(FileDetailSharingFragment.class.getSimpleName(), "Failed to pick email address as no Email found.");
            }
            cursor.close();
        } else {
            DisplayUtils.showSnackMessage(binding.getRoot(), R.string.email_pick_failed);
            Log_OC.e(FileDetailSharingFragment.class.getSimpleName(), "Failed to pick email address as Cursor is null.");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        FileExtensionsKt.logFileSize(file, TAG);
        outState.putParcelable(ARG_FILE, file);
        outState.putParcelable(ARG_USER, user);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        // NMC: not required
        // binding.sharedWithYouAvatar.setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return false;
    }

    private boolean isReshareForbidden(OCShare share) {
        return ShareType.FEDERATED == share.getShareType() ||
            capabilities != null && capabilities.getFilesSharingResharing().isFalse();
    }

    @VisibleForTesting
    public void search(String query) {
        SearchView searchView = requireView().findViewById(R.id.searchView);
        searchView.setQuery(query, true);
    }

    @Override
    public void openIn(OCShare share) {
        fileOperationsHelper.sendShareFile(file, true);
    }

    @Override
    public void advancedPermissions(OCShare share) {
        modifyExistingShare(share, FileDetailsSharingProcessFragment.SCREEN_TYPE_PERMISSION);
    }

    @Override
    public void sendNewEmail(OCShare share) {
        modifyExistingShare(share, FileDetailsSharingProcessFragment.SCREEN_TYPE_NOTE);
    }

    @Override
    public void unShare(OCShare share) {
        unShareWith(share);

        FileEntity entity = fileDataStorageManager.getFileEntity(file);

        if (binding.linkSharesList.getAdapter() instanceof ShareeListAdapter adapter) {
            adapter.remove(share);
            if (entity != null && adapter.isAdapterEmpty()) {
                entity.setSharedViaLink(0);
                fileDataStorageManager.updateFileEntity(entity);
            }
        } else if (binding.sharesList.getAdapter() instanceof ShareeListAdapter adapter) {
            adapter.remove(share);
            if (entity != null && adapter.isAdapterEmpty()) {
                entity.setSharedWithSharee(0);
                fileDataStorageManager.updateFileEntity(entity);
            }
        } else {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.failed_update_ui));
        }
    }

    @Override
    public void sendLink(OCShare share) {
        if (file.isSharedViaLink() && !TextUtils.isEmpty(share.getShareLink())) {
            FileDisplayActivity.showShareLinkDialog(fileActivity, file, share.getShareLink());
        } else {
            showSendLinkTo(share);
        }
    }

    private void modifyExistingShare(OCShare share, int screenTypePermission) {
        onEditShareListener.editExistingShare(share, screenTypePermission, !isReshareForbidden(share),
                                              capabilities.getVersion().isNewerOrEqual(OwnCloudVersion.nextcloud_18));
    }

    @Override
    public void onQuickPermissionChanged(OCShare share, int permission) {
        fileOperationsHelper.setPermissionsToShare(share, permission);
    }

    //launcher for contact permission
    private final ActivityResultLauncher<String> requestContactPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                pickContactEmail();
            } else {
                DisplayUtils.showSnackMessage(binding.getRoot(), R.string.contact_no_permission);
            }
        });

    //launcher to handle contact selection
    private final ActivityResultLauncher<Intent> onContactSelectionResultLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                                  result -> {
                                      if (result.getResultCode() == Activity.RESULT_OK) {
                                          Intent intent = result.getData();
                                          if (intent == null) {
                                              DisplayUtils.showSnackMessage(binding.getRoot(), R.string.email_pick_failed);
                                              return;
                                          }

                                          Uri contactUri = intent.getData();
                                          if (contactUri == null) {
                                              DisplayUtils.showSnackMessage(binding.getRoot(), R.string.email_pick_failed);
                                              return;
                                          }

                                          handleContactResult(contactUri);

                                      }
                                  });

    public interface OnEditShareListener {
        void editExistingShare(OCShare share, int screenTypePermission, boolean isReshareShown,
                               boolean isExpiryDateShown);

        void onShareProcessClosed();

        void onLinkShareDownloadLimitFetched(long downloadLimit, long downloadCount);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //when user is in portrait mode and search view is focused and keyboard is open
        //so when user rotate the device we have to fix the search view properly in landscape mode
        scrollToSearchViewPosition(true);
    }
}
