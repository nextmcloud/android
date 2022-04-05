/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * @author TSI-mc
 *
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 TSI-mc
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.appbar.AppBarLayout;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.preferences.AppPreferences;
import com.nmc.android.utils.AdjustSdkUtils;
import com.nmc.android.utils.TealiumSdkUtils;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsSharingFragmentBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.NextcloudVersion;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.ShareeListAdapter;
import com.owncloud.android.ui.adapter.ShareeListAdapterListener;
import com.owncloud.android.ui.asynctasks.RetrieveHoverCardAsyncTask;
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment;
import com.owncloud.android.ui.events.ShareSearchViewFocusEvent;
import com.owncloud.android.ui.fragment.util.FileDetailSharingFragmentHelper;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.ClipboardUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;


public class FileDetailSharingFragment extends Fragment implements ShareeListAdapterListener,
    DisplayUtils.AvatarGenerationListener,
    Injectable, FileDetailsSharingMenuBottomSheetActions, QuickSharingPermissionsBottomSheetDialog.QuickPermissionSharingBottomSheetActions {

    private static final String ARG_FILE = "FILE";
    private static final String ARG_USER = "USER";
    public static final int PERMISSION_EDITING_ALLOWED = 17;

    private OCFile file;
    private User user;
    private OCCapability capabilities;

    private FileOperationsHelper fileOperationsHelper;
    private FileActivity fileActivity;
    private FileDataStorageManager fileDataStorageManager;

    private FileDetailsSharingFragmentBinding binding;

    private OnEditShareListener onEditShareListener;

    @Inject UserAccountManager accountManager;
    @Inject AppPreferences appPreferences;

    @Inject ClientFactory clientFactory;

    private boolean isSearchViewFocused;

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
            file = savedInstanceState.getParcelable(ARG_FILE);
            user = savedInstanceState.getParcelable(ARG_USER);
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                file = getArguments().getParcelable(ARG_FILE);
                user = getArguments().getParcelable(ARG_USER);
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        refreshCapabilitiesFromDB();
        refreshSharesFromDB();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FileDetailsSharingFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        fileOperationsHelper = fileActivity.getFileOperationsHelper();
        fileDataStorageManager = fileActivity.getStorageManager();

        AccountManager accountManager = AccountManager.get(getContext());
        String userId = accountManager.getUserData(user.toPlatformAccount(),
                                                   com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

        binding.sharesList.setAdapter(new ShareeListAdapter(fileActivity,
                                                            new ArrayList<>(),
                                                            this,
                                                            userId,
                                                            user, SharingMenuHelper.isFileWithNoTextFile(file)));
        binding.sharesList.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.shareCreateNewLink.setOnClickListener(v -> createPublicShareLink());

        //remove focus from search view on click of root view
        binding.shareContainer.setOnClickListener(v -> binding.searchView.clearFocus());

        //enable-disable scrollview scrolling
        binding.fileDetailsNestedScrollView.setOnTouchListener((view1, motionEvent) -> {
            //true means disable the scrolling and false means enable the scrolling
            return DisplayUtils.isLandscapeOrientation() && isSearchViewFocused;
        });

        setupView();

        return view;
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
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Calling activity must implement the interface", ignored);
        }
    }

    private void setupView() {
        setShareWithYou();
    }

    private void setUpSearchView() {
        FileDetailSharingFragmentHelper.setupSearchView(
            (SearchManager) fileActivity.getSystemService(Context.SEARCH_SERVICE),
            binding.searchView,
            fileActivity.getComponentName());
        ThemeToolbarUtils.themeSearchView(binding.searchView, requireContext());

        //if (file.canReshare()) {
        binding.searchView.setQueryHint(getResources().getString(R.string.share_search));
       /* } else {
            binding.searchView.setQueryHint(getResources().getString(R.string.reshare_not_allowed));
            binding.searchView.setInputType(InputType.TYPE_NULL);
            disableSearchView(binding.searchView);
        }*/
        binding.searchView.setVisibility(View.VISIBLE);
        binding.labelPersonalShare.setVisibility(View.VISIBLE);

        binding.searchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            isSearchViewFocused = hasFocus;
            scrollToSearchViewPosition(false);
        });

    }

    /**
     * @param isDeviceRotated true when user rotated the device and false when user is already in landscape mode
     */
    private void scrollToSearchViewPosition(boolean isDeviceRotated) {
        if (DisplayUtils.isLandscapeOrientation()) {
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
            AppBarLayout appBarLayout = ((FileDisplayActivity) requireActivity()).findViewById(R.id.appbar);

            if (appBarLayout != null) {
                appBarLayout.setExpanded(false, true);
            }
        }
    }

    private void disableSearchView(View view) {
        view.setEnabled(false);

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

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
    public void initiateSharingProcess(String shareeName, ShareType shareType) {
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.share_fragment_container,
                                                                                 FileDetailsSharingProcessFragment.newInstance(file,
                                                                                                                               shareeName,
                                                                                                                               shareType, SharingMenuHelper.isFileWithNoTextFile(file)),
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
                                                                                                                               isExpiryDateShown, SharingMenuHelper.isFileWithNoTextFile(file)),
                                                                                 FileDetailsSharingProcessFragment.TAG)
            .addToBackStack(null)
            .commit();
    }

    private void setShareWithYou() {
        if (accountManager.userOwnsFile(file, user)) {
            binding.sharedWithYouContainer.setVisibility(View.GONE);
            binding.shareCreateNewLink.setVisibility(View.VISIBLE);
            binding.tvSharingDetailsMessage.setText(getResources().getString(R.string.sharing_description));
            setUpSearchView();
        } else {
            binding.sharedWithYouUsername.setText(
                String.format(getString(R.string.shared_with_you_by), file.getOwnerDisplayName()));
          /*  DisplayUtils.setAvatar(user,
                                   file.getOwnerId(),
                                   this,
                                   getResources().getDimension(
                                       R.dimen.file_list_item_avatar_icon_radius),
                                   getResources(),
                                   binding.sharedWithYouAvatar,
                                   getContext());
            binding.sharedWithYouAvatar.setVisibility(View.VISIBLE);*/

            String note = file.getNote();

            if (!TextUtils.isEmpty(note)) {
                binding.sharedWithYouNote.setText(file.getNote());
                binding.sharedWithYouNoteContainer.setVisibility(View.VISIBLE);
            } else {
                binding.sharedWithYouNoteContainer.setVisibility(View.GONE);
            }

            if (file.canReshare()) {
                binding.tvSharingDetailsMessage.setText(getResources().getString(R.string.reshare_allowed) + " " + getResources().getString(R.string.sharing_description));
                setUpSearchView();
            } else {
                binding.searchView.setVisibility(View.GONE);
                binding.labelPersonalShare.setVisibility(View.GONE);
                binding.shareCreateNewLink.setVisibility(View.GONE);
                binding.tvSharingDetailsMessage.setText(getResources().getString(R.string.reshare_not_allowed));
            }
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

    private void showSendLinkTo(OCShare publicShare) {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(publicShare.getShareLink())) {
                fileOperationsHelper.getFileWithLink(file);
            } else {
                FileDisplayActivity.showShareLinkDialog(fileActivity, file, publicShare.getShareLink());
            }
        }
    }

    public void copyLink(OCShare share) {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(share.getShareLink())) {
                fileOperationsHelper.getFileWithLink(file);
            } else {
                ClipboardUtil.copyToClipboard(getActivity(), share.getShareLink());
            }
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
        new FileDetailSharingMenuBottomSheetDialog(fileActivity, this, share).show();
    }

    /**
     * show quick sharing permission dialog
     *
     * @param share
     */
    @Override
    public void showPermissionsDialog(OCShare share) {
        new QuickSharingPermissionsBottomSheetDialog(fileActivity, this, share).show();
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
     * will be called when download limit from api is fetched
     *
     * @param result
     */
    public void onLinkShareDownloadLimitFetched(RemoteOperationResult result) {
        //onEditShareListener.onLinkShareDownloadLimitFetched();
    }

    /**
     * Get {@link OCShare} instance from DB and updates the UI.
     */
    private void refreshUiFromDB() {
        refreshSharesFromDB();
        // Updates UI with new state
        setupView();
    }

    private void unshareWith(OCShare share) {
        fileOperationsHelper.unshareShare(file, share);
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
        if (user.getServer().getVersion().isNewerOrEqual(NextcloudVersion.Companion.getNextcloud_23())) {
            new RetrieveHoverCardAsyncTask(user, shareWith, fileActivity, clientFactory).execute();
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
    public void refreshSharesFromDB() {
        ShareeListAdapter adapter = (ShareeListAdapter) binding.sharesList.getAdapter();

        if (adapter == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.could_not_retrieve_shares));
            return;
        }
        adapter.getShares().clear();

        // to show share with users/groups info
        List<OCShare> shares = fileDataStorageManager.getSharesWithForAFile(file.getRemotePath(),
                                                                            user.getAccountName());

        adapter.addShares(shares);

        if (FileDetailSharingFragmentHelper.isPublicShareDisabled(capabilities) || !file.canReshare()) {
            return;
        }

        // Get public share
        List<OCShare> publicShares = fileDataStorageManager.getSharesByPathAndType(file.getRemotePath(),
                                                                                   ShareType.PUBLIC_LINK,
                                                                                   "");


       /* if (publicShares.isEmpty() && containsNoNewPublicShare(adapter.getShares())) {
            publicShares.add(new OCShare().setShareType(ShareType.NEW_PUBLIC_LINK));
        } else {
            adapter.removeNewPublicShare();
        }*/
        if (publicShares.isEmpty() && containsNoNewPublicShare(adapter.getShares())) {
            final OCShare ocShare = new OCShare();
            ocShare.setShareType(ShareType.NEW_PUBLIC_LINK);
            publicShares.add(ocShare);
        } else {
            adapter.removeNewPublicShare();
        }


        adapter.addShares(publicShares);

        if ((shares != null && !shares.isEmpty()) || (publicShares != null && !publicShares.isEmpty())) {
            showHideView(false);
        } else {
            showHideView(true);
        }
    }

    private void showHideView(boolean isEmptyList) {
        binding.sharesList.setVisibility(isEmptyList ? View.GONE : View.VISIBLE);
        binding.tvYourShares.setVisibility(isEmptyList ? View.GONE : View.VISIBLE);
        binding.tvEmptyShares.setVisibility(isEmptyList ? View.VISIBLE : View.GONE);
    }


    private boolean containsNoNewPublicShare(List<OCShare> shares) {
        for (OCShare share : shares) {
            if (share.getShareType() == ShareType.NEW_PUBLIC_LINK) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_FILE, file);
        outState.putParcelable(ARG_USER, user);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        binding.sharedWithYouAvatar.setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return false;
    }

    private boolean isReshareForbidden(OCShare share) {
        return ShareType.FEDERATED.equals(share.getShareType()) ||
            capabilities != null && capabilities.getFilesSharingResharing().isFalse();
    }

    @VisibleForTesting
    public void search(String query) {
        SearchView searchView = getView().findViewById(R.id.searchView);
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
        unshareWith(share);
        ShareeListAdapter adapter = (ShareeListAdapter) binding.sharesList.getAdapter();
        if (adapter == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.failed_update_ui));
            return;
        }
        adapter.remove(share);
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

    public interface OnEditShareListener {
        void editExistingShare(OCShare share, int screenTypePermission, boolean isReshareShown,
                               boolean isExpiryDateShown);

        void onLinkShareDownloadLimitFetched(long downloadLimit, long downloadCount);

        void onShareProcessClosed();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //when user is in portrait mode and search view is focused and keyboard is open
        //so when user rotate the device we have to fix the search view properly in landscape mode
        scrollToSearchViewPosition(true);
    }
}
