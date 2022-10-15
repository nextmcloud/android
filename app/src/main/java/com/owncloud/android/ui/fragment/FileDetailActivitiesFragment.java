/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.nmc.android.ui.CommentsActionsBottomSheetDialog;
import com.nmc.android.utils.KeyboardUtils;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsActivitiesFragmentBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.model.RichObject;
import com.owncloud.android.lib.resources.comments.MarkCommentsAsReadRemoteOperation;
import com.owncloud.android.lib.resources.files.model.FileVersion;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.CommentFileOperation;
import com.owncloud.android.operations.comments.Comments;
import com.owncloud.android.operations.comments.DeleteCommentRemoteOperation;
import com.owncloud.android.operations.comments.GetCommentsRemoteOperation;
import com.owncloud.android.operations.comments.UpdateCommentRemoteOperation;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.adapter.ActivityAndVersionListAdapter;
import com.owncloud.android.ui.dialog.EditCommentDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.events.CommentsEvent;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.interfaces.VersionListInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeLayoutUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;

public class FileDetailActivitiesFragment extends Fragment implements
    ActivityListInterface,
    DisplayUtils.AvatarGenerationListener,
    VersionListInterface.View,
    CommentsActionsBottomSheetDialog.CommentsBottomSheetActions,
    Injectable {

    private static final String TAG = FileDetailActivitiesFragment.class.getSimpleName();

    private static final String ARG_FILE = "FILE";
    private static final String ARG_USER = "USER";
    private static final int END_REACHED = 0;

    private ActivityAndVersionListAdapter adapter;
    private OwnCloudClient ownCloudClient;
    private NextcloudClient nextcloudClient;

    private OCFile file;
    private User user;

    private int lastGiven;
    private boolean isLoadingActivities;

    //not using flag for NMC app
    private boolean restoreFileVersionSupported;
    private FileOperationsHelper operationsHelper;
    private VersionListInterface.CommentCallback callback;

    private FileDetailsActivitiesFragmentBinding binding;

    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    @Inject ContentResolver contentResolver;

    public static FileDetailActivitiesFragment newInstance(OCFile file, User user) {
        FileDetailActivitiesFragment fragment = new FileDetailActivitiesFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("arguments are mandatory");
        }
        file = arguments.getParcelable(ARG_FILE);
        user = arguments.getParcelable(ARG_USER);

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelable(ARG_FILE);
            user = savedInstanceState.getParcelable(ARG_USER);
        }

        binding = FileDetailsActivitiesFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        setupView();

        ThemeLayoutUtils.colorSwipeRefreshLayout(getContext(), binding.swipeContainingEmpty);
        ThemeLayoutUtils.colorSwipeRefreshLayout(getContext(), binding.swipeContainingList);

        fetchAndSetData(-1);

        binding.swipeContainingList.setOnRefreshListener(() -> {
            setLoadingMessage();
            binding.swipeContainingList.setRefreshing(true);
            fetchAndSetData(-1);
        });

        binding.swipeContainingEmpty.setOnRefreshListener(() -> {
            setLoadingMessageEmpty();
            fetchAndSetData(-1);
        });


        callback = new VersionListInterface.CommentCallback() {

            @Override
            public void onSuccess() {
                binding.commentInputField.getText().clear();
                fetchAndSetData(-1);
            }

            @Override
            public void onError(int error) {
                Snackbar.make(binding.list, error, Snackbar.LENGTH_LONG).show();
            }
        };

        binding.submitComment.setOnClickListener(v -> submitComment());

        binding.commentInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitComment();
                    return true;
                }
                return false;
            }
        });


        //remove focus from input view on click of root view
        binding.getRoot().setOnClickListener(v -> {
            binding.commentInputField.clearFocus();
            KeyboardUtils.hideKeyboardFrom(requireContext(), binding.getRoot());
        });

        DisplayUtils.setAvatar(user,
                               this,
                               getResources().getDimension(R.dimen.activity_icon_radius),
                               getResources(),
                               binding.avatar,
                               getContext());


        return view;
    }

    public void submitComment() {
        Editable commentField = binding.commentInputField.getText();

        if (commentField == null) {
            return;
        }

        String trimmedComment = commentField.toString().trim();

        if (trimmedComment.length() > 0) {
            new SubmitCommentTask(trimmedComment, file.getLocalId(), callback, ownCloudClient).execute();
        }
    }

    private void setLoadingMessage() {
        binding.swipeContainingEmpty.setVisibility(View.GONE);
    }

    @VisibleForTesting
    public void setLoadingMessageEmpty() {
        binding.swipeContainingList.setVisibility(View.GONE);
        binding.emptyList.emptyListView.setVisibility(View.GONE);
        binding.loadingContent.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupView() {
        FileDataStorageManager storageManager = new FileDataStorageManager(user,
                                                                           contentResolver);
        operationsHelper = ((ComponentsGetter) requireActivity()).getFileOperationsHelper();

        OCCapability capability = storageManager.getCapability(user.getAccountName());
        restoreFileVersionSupported = capability.getFilesVersioning().isTrue();

        binding.emptyList.emptyListIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_activity, null));
        binding.emptyList.emptyListView.setVisibility(View.GONE);

        AccountManager acctManager = AccountManager.get(getContext());
        String userId = acctManager.getUserData(user.toPlatformAccount(),
                                                com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

        adapter = new ActivityAndVersionListAdapter(getContext(),
                                                    accountManager,
                                                    this,
                                                    this,
                                                    clientFactory,
                                                    userId
        );
        binding.list.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        binding.list.setLayoutManager(layoutManager);
        /*binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = recyclerView.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition();

                // synchronize loading state when item count changes
                if (!isLoadingActivities && (totalItemCount - visibleItemCount) <= (firstVisibleItemIndex + 5)
                    && lastGiven > 0) {
                    // Almost reached the end, continue to load new activities
                    fetchAndSetData(lastGiven);
                }
            }
        });*/
    }

    public void reload() {
        fetchAndSetData(-1);
    }

    private void fetchAndSetData(int lastGiven) {
        final FragmentActivity activity = getActivity();

        if (activity == null) {
            Log_OC.e(this, "Activity is null, aborting!");
            return;
        }

        final User user = accountManager.getUser();

        if (user.isAnonymous()) {
            activity.runOnUiThread(() -> {
                setEmptyContent(getString(R.string.common_error), getString(R.string.file_detail_comment_error));
            });
            return;
        }

        Thread t = new Thread(() -> {
            try {
                ownCloudClient = clientFactory.create(user);
                nextcloudClient = clientFactory.createNextcloudClient(user);

                isLoadingActivities = true;

                GetCommentsRemoteOperation getCommentsRemoteOperation = new GetCommentsRemoteOperation(file.getLocalId(), 0, 0);

                Log_OC.d(TAG, "BEFORE getCommentsRemoteOperation.execute");
                RemoteOperationResult result = getCommentsRemoteOperation.execute(ownCloudClient);


                if (result.isSuccess() && result.getResultData() != null) {
                    List<Object> commentsList = (List<Object>) result.getResultData();


                    activity.runOnUiThread(() -> {
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            populateList(commentsList, lastGiven == -1);
                        }
                    });
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    String logMessage = result.getLogMessage();
                    if (result.getHttpCode() == HttpStatus.SC_NOT_MODIFIED) {
                        logMessage = getString(R.string.activities_no_results_message);
                    }
                    final String finalLogMessage = logMessage;
                    activity.runOnUiThread(() -> {
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            setErrorContent(finalLogMessage);
                            isLoadingActivities = false;
                        }
                    });
                }

                hideRefreshLayoutLoader(activity);
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(TAG, "Error fetching file details comments", e);
            }
        });

        t.start();
    }

    /**
     * @param lastGiven int; -1 to disable
     */
    //NMC: Not using this method as we don't have to show the activities
    /*private void fetchAndSetData(int lastGiven) {
        final FragmentActivity activity = getActivity();

        if (activity == null) {
            Log_OC.e(this, "Activity is null, aborting!");
            return;
        }

        final User user = accountManager.getUser();

        if (user.isAnonymous()) {
            activity.runOnUiThread(() -> {
                setEmptyContent(getString(R.string.common_error), getString(R.string.file_detail_activity_error));
            });
            return;
        }

        Thread t = new Thread(() -> {
            try {
                ownCloudClient = clientFactory.create(user);
                nextcloudClient = clientFactory.createNextcloudClient(user);

                isLoadingActivities = true;

                GetActivitiesRemoteOperation getRemoteNotificationOperation;

                if (lastGiven > 0) {
                    getRemoteNotificationOperation = new GetActivitiesRemoteOperation(file.getLocalId(), lastGiven);
                } else {
                    getRemoteNotificationOperation = new GetActivitiesRemoteOperation(file.getLocalId());
                }

                Log_OC.d(TAG, "BEFORE getRemoteActivitiesOperation.execute");
                RemoteOperationResult result = nextcloudClient.execute(getRemoteNotificationOperation);

                ArrayList<Object> versions = null;
                if (restoreFileVersionSupported) {
                    ReadFileVersionsRemoteOperation readFileVersionsOperation = new ReadFileVersionsRemoteOperation(
                        file.getLocalId());

                    RemoteOperationResult result1 = readFileVersionsOperation.execute(ownCloudClient);

                    if (result1.isSuccess()) {
                        versions = result1.getData();
                    }
                }

                if (result.isSuccess() && result.getData() != null) {
                    final List<Object> data = result.getData();
                    final List<Object> activitiesAndVersions = (ArrayList) data.get(0);

                    this.lastGiven = (int) data.get(1);

                    if (activitiesAndVersions.isEmpty()) {
                        this.lastGiven = END_REACHED;
                    }

                    if (restoreFileVersionSupported && versions != null) {
                        activitiesAndVersions.addAll(versions);
                    }

                    activity.runOnUiThread(() -> {
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            populateList(activitiesAndVersions, lastGiven == -1);
                        }
                    });
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    String logMessage = result.getLogMessage();
                    if (result.getHttpCode() == HttpStatus.SC_NOT_MODIFIED) {
                        logMessage = getString(R.string.activities_no_results_message);
                    }
                    final String finalLogMessage = logMessage;
                    activity.runOnUiThread(() -> {
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            setErrorContent(finalLogMessage);
                            isLoadingActivities = false;
                        }
                    });
                }

                hideRefreshLayoutLoader(activity);
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(TAG, "Error fetching file details activities", e);
            }
        });

        t.start();
    }*/
    public void markCommentsAsRead() {
        new Thread(() -> {
            if (file.getUnreadCommentsCount() > 0) {
                MarkCommentsAsReadRemoteOperation unreadOperation = new MarkCommentsAsReadRemoteOperation(
                    file.getLocalId());
                RemoteOperationResult remoteOperationResult = unreadOperation.execute(ownCloudClient);

                if (remoteOperationResult.isSuccess()) {
                    EventBus.getDefault().post(new CommentsEvent(file.getRemoteId()));
                }
            }
        }).start();
    }

    @VisibleForTesting
    public void populateList(List<Object> activities, boolean clear) {
        adapter.setActivityAndVersionItems(activities, nextcloudClient, clear);

        if (adapter.getItemCount() == 0) {
            setEmptyContent(
                getString(R.string.comments_no_results_headline),
                getString(R.string.comments_no_results_message)
                           );
        } else {
            binding.swipeContainingList.setVisibility(View.VISIBLE);
            binding.swipeContainingEmpty.setVisibility(View.GONE);
            binding.emptyList.emptyListView.setVisibility(View.GONE);
        }
        isLoadingActivities = false;
    }

    private void setEmptyContent(String headline, String message) {
        setInfoContent(R.drawable.ic_activity, headline, message);
    }

    @VisibleForTesting
    public void setErrorContent(String message) {
        setInfoContent(R.drawable.ic_list_empty_error, getString(R.string.common_error), message);
    }

    private void setInfoContent(@DrawableRes int icon, String headline, String message) {
        binding.emptyList.emptyListIcon.setImageDrawable(ResourcesCompat.getDrawable(requireContext().getResources(),
                                                                                     icon,
                                                                                     null));
        binding.emptyList.emptyListViewHeadline.setText(headline);
        binding.emptyList.emptyListViewText.setText(message);

        binding.swipeContainingList.setVisibility(View.GONE);
        binding.loadingContent.setVisibility(View.GONE);

        binding.emptyList.emptyListViewHeadline.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
        binding.swipeContainingEmpty.setVisibility(View.VISIBLE);
    }

    private void hideRefreshLayoutLoader(FragmentActivity activity) {
        activity.runOnUiThread(() -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                binding.swipeContainingList.setRefreshing(false);
                binding.swipeContainingEmpty.setRefreshing(false);
                isLoadingActivities = false;
            }
        });
    }

    @Override
    public void onActivityClicked(RichObject richObject) {
        // TODO implement activity click
    }

    @Override
    public void onCommentsOverflowMenuClicked(Comments comments) {
        new CommentsActionsBottomSheetDialog(requireContext(), comments, this).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_FILE, file);
        outState.putParcelable(ARG_USER, user);
    }

    @Override
    public void onRestoreClicked(FileVersion fileVersion) {
        operationsHelper.restoreFileVersion(fileVersion);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        binding.avatar.setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return false;
    }

    @Override
    public void onUpdateComment(@NonNull Comments comments) {
        EditCommentDialogFragment dialog = EditCommentDialogFragment.newInstance(comments);
        dialog.setOnEditCommentListener((comments1, message) -> {
            new UpdateCommentTask(message, file.getLocalId(), comments1.getCommentId(), callback, ownCloudClient).execute();
        });
        dialog.show(requireActivity().getSupportFragmentManager(), EditCommentDialogFragment.EDIT_COMMENT_FRAGMENT_TAG);
    }

    @Override
    public void onDeleteComment(@NonNull Comments comments) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setPositiveButton(R.string.common_yes, (dialogInterface, i) -> new DeleteCommentTask(file.getLocalId(), comments.getCommentId(),
                                                                                                     callback, ownCloudClient).execute())
            .setNegativeButton(R.string.common_no, null)
            .setMessage(R.string.delete_comment_dialog_message);
        Dialog dialog = builder.create();
        dialog.show();
    }

    private static class SubmitCommentTask extends AsyncTask<Void, Void, Boolean> {

        private String message;
        private String fileId;
        private VersionListInterface.CommentCallback callback;
        private OwnCloudClient client;

        private SubmitCommentTask(String message, String fileId, VersionListInterface.CommentCallback callback,
                                  OwnCloudClient client) {
            this.message = message;
            this.fileId = fileId;
            this.callback = callback;
            this.client = client;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            CommentFileOperation commentFileOperation = new CommentFileOperation(message, fileId);

            RemoteOperationResult result = commentFileOperation.execute(client);

            return result.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            if (success) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.error_comment_file);

            }
        }
    }

    private static class UpdateCommentTask extends AsyncTask<Void, Void, Boolean> {

        private final String message;
        private final String fileId;
        private final int commentId;
        private final VersionListInterface.CommentCallback callback;
        private final OwnCloudClient client;

        private UpdateCommentTask(String message, String fileId, int commentId, VersionListInterface.CommentCallback callback,
                                  OwnCloudClient client) {
            this.message = message;
            this.fileId = fileId;
            this.commentId = commentId;
            this.callback = callback;
            this.client = client;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            UpdateCommentRemoteOperation updateCommentRemoteOperation = new UpdateCommentRemoteOperation(fileId, commentId, message);

            RemoteOperationResult result = updateCommentRemoteOperation.execute(client);

            return result.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                callback.onSuccess();
                //call error to show success message
                callback.onError(R.string.success_update_comment_file);
            } else {
                callback.onError(R.string.error_update_comment_file);
            }
        }
    }

    private static class DeleteCommentTask extends AsyncTask<Void, Void, Boolean> {

        private final String fileId;
        private final int commentId;
        private final VersionListInterface.CommentCallback callback;
        private final OwnCloudClient client;

        private DeleteCommentTask(String fileId, int commentId, VersionListInterface.CommentCallback callback,
                                  OwnCloudClient client) {
            this.fileId = fileId;
            this.commentId = commentId;
            this.callback = callback;
            this.client = client;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            DeleteCommentRemoteOperation deleteCommentRemoteOperation = new DeleteCommentRemoteOperation(fileId, commentId);

            RemoteOperationResult result = deleteCommentRemoteOperation.execute(client);

            return result.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                callback.onSuccess();
                //call error to show success message
                callback.onError(R.string.success_delete_comment_file);
            } else {
                callback.onError(R.string.error_delete_comment_file);
            }
        }
    }

}
