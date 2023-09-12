/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.accounts.AccountManager
import android.content.ContentResolver
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nmc.android.ui.CommentsActionsBottomSheetDialog
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsActivitiesFragmentBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.activities.GetActivitiesRemoteOperation
import com.owncloud.android.lib.resources.activities.model.RichObject
import com.owncloud.android.lib.resources.comments.MarkCommentsAsReadRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileVersionsRemoteOperation
import com.owncloud.android.lib.resources.files.model.FileVersion
import com.owncloud.android.operations.CommentFileOperation
import com.owncloud.android.operations.comments.Comments
import com.owncloud.android.operations.comments.DeleteCommentRemoteOperation
import com.owncloud.android.operations.comments.GetCommentsRemoteOperation
import com.owncloud.android.operations.comments.UpdateCommentRemoteOperation
import com.owncloud.android.ui.activities.adapter.ActivityAndVersionListAdapter
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.dialog.EditCommentDialogFragment
import com.owncloud.android.ui.events.CommentsEvent
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.ui.interfaces.ActivityListInterface
import com.owncloud.android.ui.interfaces.VersionListInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@Suppress("TooManyFunctions", "ReturnCount")
class FileDetailActivitiesFragment :
    Fragment(),
    ActivityListInterface,
    AvatarGenerationListener,
    VersionListInterface.View,
    CommentsActionsBottomSheetDialog.CommentsBottomSheetActions,
    Injectable {

    private var adapter: ActivityAndVersionListAdapter? = null
    private var ownCloudClient: OwnCloudClient? = null
    private var nextcloudClient: NextcloudClient? = null

    private var file: OCFile? = null
    private var user: User? = null

    private var lastGiven: Long = 0
    private var isLoadingActivities = false
    private var isDataFetched = false

    private var restoreFileVersionSupported = false
    private var operationsHelper: FileOperationsHelper? = null
    private var callback: VersionListInterface.CommentCallback? = null

    private var submitCommentJob: Job? = null

    private var binding: FileDetailsActivitiesFragmentBinding? = null

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var contentResolver: ContentResolver

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    // region Lifecycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val arguments = checkNotNull(arguments) { "arguments are mandatory" }
        val source = savedInstanceState ?: arguments
        file = source.getParcelableArgument(ARG_FILE, OCFile::class.java)
        user = source.getParcelableArgument(ARG_USER, User::class.java)

        val binding = FileDetailsActivitiesFragmentBinding.inflate(inflater, container, false)
        this.binding = binding

        setupView()

        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingEmpty)
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)

        isLoadingActivities = true
        fetchAndSetData(-1)

        setupRefreshListeners(binding)
        callback = createCommentCallback()

        binding.submitComment.setOnClickListener { submitComment() }
        binding.commentInputField.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(
                p0: TextView?,
                actionId: Int,
                p2: KeyEvent?
            ): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitComment()
                    return true
                }
                return false
            }
        })

        DisplayUtils.setAvatar(
            user!!,
            this,
            resources.getDimension(R.dimen.activity_icon_radius),
            resources,
            binding.avatar,
            context
        )

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        submitCommentJob?.cancel()
        submitCommentJob = null
        callback = null
        binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_FILE, file)
        outState.putParcelable(ARG_USER, user)
    }
    // endregion

    // region Setup
    private fun setupView() {
        val binding = binding ?: return
        val storageManager = FileDataStorageManager(user, contentResolver)
        operationsHelper = (requireActivity() as ComponentsGetter).fileOperationsHelper

        val capability = storageManager.getCapability(user?.accountName)
        restoreFileVersionSupported = capability.filesVersioning.isTrue

        binding.emptyList.emptyListIcon.setImageDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.ic_activity, null)
        )
        binding.emptyList.emptyListView.visibility = View.GONE
        val acctManager = AccountManager.get(context)
        val userId = acctManager.getUserData(
            user?.toPlatformAccount(),
            com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID
        )
        adapter = ActivityAndVersionListAdapter(requireActivity(), accountManager, this, this, viewThemeUtils, userId)
        binding.list.adapter = adapter

        val layoutManager = LinearLayoutManager(context)
        binding.list.layoutManager = layoutManager
    }

    private fun setupRefreshListeners(binding: FileDetailsActivitiesFragmentBinding) {
        binding.swipeContainingList.setOnRefreshListener {
            setLoadingMessage()
            binding.swipeContainingList.isRefreshing = true
            isLoadingActivities = true
            fetchAndSetData(-1)
        }

        binding.swipeContainingEmpty.setOnRefreshListener {
            setLoadingMessageEmpty()
            isLoadingActivities = true
            fetchAndSetData(-1)
        }
    }

    private fun createCommentCallback() = object : VersionListInterface.CommentCallback {
        override fun onSuccess() {
            if (binding != null && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                binding?.commentInputField?.text?.clear()
                fetchAndSetData(-1)
            }
        }

        override fun onError(error: Int) {
            val view = view ?: return
            if (isAdded) {
                Snackbar.make(view, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }
    // endregion

    // region Data loading
    fun reload() {
        fetchAndSetData(-1)
    }

    private fun fetchAndSetData(lastGiven: Int) {
        val activity = getActivity()

        if (activity == null) {
            Log_OC.e(this, "Activity is null, aborting!")
            return;
        }

        val user = accountManager.user;

        if (user.isAnonymous) {
            activity.runOnUiThread {
                setEmptyContent(getString(R.string.common_error), getString(R.string.file_detail_comment_error))
            }
            return
        }

        val t = Thread({
            try {
                ownCloudClient = clientFactory.create(user)
                nextcloudClient = clientFactory.createNextcloudClient(user)

                isLoadingActivities = true

                val getCommentsRemoteOperation = GetCommentsRemoteOperation(file!!.localId, 0, 0)

                Log_OC.d(TAG, "BEFORE getCommentsRemoteOperation.execute")
                val result = getCommentsRemoteOperation.execute(ownCloudClient)


                if (result.isSuccess && result.getResultData() != null) {
                    val commentsList = result.getResultData() as List<*>

                    activity.runOnUiThread({
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            populateList(commentsList, lastGiven == -1)
                        }
                    })
                } else {
                    Log_OC.d(TAG, result.logMessage)
                    // show error
                    var logMessage = result.logMessage
                    if (result.httpCode == HttpStatus.SC_NOT_MODIFIED) {
                        logMessage = getString(R.string.activities_no_results_message)
                    }
                    val finalLogMessage = logMessage
                    activity.runOnUiThread({
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            setErrorContent(finalLogMessage)
                            isLoadingActivities = false
                        }
                    })
                }

                hideRefreshLayoutLoader()
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Error fetching file details comments", e)
            }
        })

        t.start()
    }

    // NMC: Not using this method as we don't have to show the activities
    /* */
    /**
     * @param lastGiven long; -1 to disable
     *//*
    @Suppress("DEPRECATION")
    private fun fetchAndSetData(lastGiven: Long) {
        val activity = activity
        if (activity == null) {
            Log_OC.e(this, "Activity is null, aborting!")
            return
        }

        val user = accountManager.user
        if (user.isAnonymous) {
            setEmptyContent(
                getString(R.string.common_error),
                getString(R.string.file_detail_activity_error)
            )
            return
        }

        if (!isLoadingActivities) {
            return
        }

        val file = file ?: return

        lifecycleScope.launch {
            try {
                val (result, versions) = withContext(Dispatchers.IO) {
                    loadActivities(user, file, lastGiven)
                }
                handleActivitiesResult(activity, result, versions, lastGiven)
                hideRefreshLayoutLoader()
            } catch (e: CreationException) {
                isDataFetched = false
                Log_OC.e(TAG, "Error fetching file details activities", e)
            }
        }
    }*/

    @Suppress("DEPRECATION")
    private fun loadActivities(
        user: User,
        file: OCFile,
        lastGiven: Long
    ): Pair<RemoteOperationResult<Any?>, ArrayList<Any?>?> {
        val ownCloudClient = clientFactory.create(user)
        this.ownCloudClient = ownCloudClient
        val nextcloudClient = clientFactory.createNextcloudClient(user)
        this.nextcloudClient = nextcloudClient
        isLoadingActivities = true

        val operation = if (lastGiven > 0) {
            GetActivitiesRemoteOperation(file.localId, lastGiven)
        } else {
            GetActivitiesRemoteOperation(file.localId)
        }

        val result = nextcloudClient.execute<Any?>(operation)

        val versions = when {
            !restoreFileVersionSupported -> null

            else -> ReadFileVersionsRemoteOperation(file.localId).execute(ownCloudClient)
                .takeIf { it.isSuccess }
                ?.data
        }

        return result to versions
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    private fun handleActivitiesResult(
        activity: FragmentActivity,
        result: RemoteOperationResult<Any?>,
        versions: ArrayList<Any?>?,
        lastGiven: Long
    ) {
        val data = result.data
        if (result.isSuccess && data != null) {
            val activitiesAndVersions = data[0] as ArrayList<Any?>
            this.lastGiven = data[1] as Long

            if (activitiesAndVersions.isEmpty()) {
                this.lastGiven = END_REACHED.toLong()
            }

            if (restoreFileVersionSupported && versions != null) {
                activitiesAndVersions.addAll(versions)
            }

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                populateList(activitiesAndVersions, lastGiven == -1L)
            }

            isDataFetched = true
            return
        }

        Log_OC.d(TAG, result.logMessage)
        val logMessage = if (result.httpCode == HttpStatus.SC_NOT_MODIFIED) {
            getString(R.string.activities_no_results_message)
        } else {
            result.getLogMessage(activity)
        }

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            setErrorContent(logMessage)
            isLoadingActivities = false
        }

        isDataFetched = false
    }
    // endregion

    // region Comments
    fun submitComment() {
        val binding = binding ?: return
        val client = nextcloudClient
        val comment = binding.commentInputField.text?.toString()?.trim().orEmpty()
        if (comment.isEmpty() || client == null || !isDataFetched) {
            return
        }
        val fileId = file?.localId ?: return

        submitCommentJob?.cancel()
        submitCommentJob = lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                CommentFileOperation(comment, fileId).execute(client).isSuccess
            }
            if (success) {
                callback?.onSuccess()
            } else {
                callback?.onError(R.string.error_comment_file)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun markCommentsAsRead() {
        val file = file ?: return
        if (file.unreadCommentsCount <= 0) {
            return
        }
        val client = ownCloudClient

        lifecycleScope.launch(Dispatchers.IO) {
            val result = MarkCommentsAsReadRemoteOperation(file.localId).execute(client)
            if (result.isSuccess) {
                EventBus.getDefault().post(CommentsEvent(file.remoteId))
            }
        }
    }
    // endregion

    // region View state
    private fun setLoadingMessage() {
        binding?.swipeContainingEmpty?.visibility = View.GONE
    }

    @VisibleForTesting
    fun setLoadingMessageEmpty() {
        val binding = binding ?: return
        binding.swipeContainingList.visibility = View.GONE
        binding.emptyList.emptyListView.visibility = View.GONE
        binding.loadingContent.visibility = View.VISIBLE
    }

    @VisibleForTesting
    fun populateList(activities: List<Any?>?, clear: Boolean) {
        val items = ArrayList(activities ?: emptyList())
        adapter?.setActivityAndVersionItems(items, nextcloudClient, clear)

        val binding = binding ?: return

        if (adapter?.itemCount == 0) {
            setEmptyContent(
                getString(R.string.comments_no_results_headline),
                getString(R.string.comments_no_results_message)
            )
        } else {
            binding.swipeContainingList.visibility = View.VISIBLE
            binding.swipeContainingEmpty.visibility = View.GONE
            binding.emptyList.emptyListView.visibility = View.GONE
        }
        isLoadingActivities = false
    }

    private fun setEmptyContent(headline: String?, message: String?) {
        // NMC: no icon required for empty state
        setInfoContent(0, headline, message)
    }

    @VisibleForTesting
    fun setErrorContent(message: String?) {
        setInfoContent(R.drawable.ic_list_empty_error, getString(R.string.common_error), message)
    }

    private fun setInfoContent(@DrawableRes icon: Int, headline: String?, message: String?) {
        val binding = binding ?: return

        // NMC: to handle no icon visibility
        if (icon != 0) {
            binding.emptyList.emptyListIcon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    requireContext().resources,
                    icon,
                    null
                )
            )
            binding.emptyList.emptyListIcon.visibility = View.VISIBLE
        } else {
            binding.emptyList.emptyListIcon.visibility = View.GONE
        }

        binding.emptyList.emptyListViewHeadline.text = headline
        binding.emptyList.emptyListViewText.text = message

        binding.swipeContainingList.visibility = View.GONE
        binding.loadingContent.visibility = View.GONE

        binding.emptyList.emptyListViewHeadline.visibility = View.VISIBLE
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE
        binding.emptyList.emptyListView.visibility = View.VISIBLE
        binding.swipeContainingEmpty.visibility = View.VISIBLE
    }

    private fun hideRefreshLayoutLoader() {
        val binding = binding ?: return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }
        binding.swipeContainingList.isRefreshing = false
        binding.swipeContainingEmpty.isRefreshing = false
        isLoadingActivities = false
    }
    // endregion

    // region Interface callbacks
    override fun onActivityClicked(richObject: RichObject?) {
        // TODO implement activity click
    }

    override fun onCommentsOverflowMenuClicked(comments: Comments?) {
        comments?.let {
            CommentsActionsBottomSheetDialog(requireContext(), it, this).show()
        }
    }

    override fun onRestoreClicked(fileVersion: FileVersion?) {
        operationsHelper?.restoreFileVersion(fileVersion)
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        binding?.avatar?.setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean = false
    // endregion

    @VisibleForTesting
    fun disableLoadingActivities() {
        isLoadingActivities = false
    }

    override fun onUpdateComment(comments: Comments) {
        val dialog = EditCommentDialogFragment.newInstance(comments)
        dialog.setOnEditCommentListener { comments1, message ->
            UpdateCommentTask(message, file!!.localId, comments1.commentId, callback, ownCloudClient!!)
                .execute(lifecycleScope)
        }
        dialog.show(requireActivity().supportFragmentManager, EditCommentDialogFragment.EDIT_COMMENT_FRAGMENT_TAG)
    }

    override fun onDeleteComment(comments: Comments) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setPositiveButton(
            R.string.common_yes
        ) { _, _ ->
            DeleteCommentTask(
                file!!.localId, comments.commentId,
                callback, ownCloudClient!!
            ).execute(lifecycleScope)
        }
            .setNegativeButton(R.string.common_no, null)
            .setMessage(R.string.delete_comment_dialog_message);
        val dialog = builder.create()
        dialog.show()
    }

    companion object {
        private val TAG: String = FileDetailActivitiesFragment::class.java.simpleName

        private const val ARG_FILE = "FILE"
        private const val ARG_USER = "USER"
        private const val END_REACHED = 0
        private const val LOAD_MORE_THRESHOLD = 5

        @JvmStatic
        fun newInstance(file: OCFile?, user: User?): FileDetailActivitiesFragment =
            FileDetailActivitiesFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FILE, file)
                    putParcelable(ARG_USER, user)
                }
            }
    }

    class UpdateCommentTask(
        private val message: String,
        private val fileId: Long,
        private val commentId: Int,
        private val callback: VersionListInterface.CommentCallback?,
        private val client: OwnCloudClient
    ) {

        fun execute(scope: CoroutineScope) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    val operation =
                        UpdateCommentRemoteOperation(fileId, commentId, message)

                    val result = operation.execute(client)
                    result.isSuccess
                }

                if (success) {
                    callback?.onSuccess()
                    // Call error to show success message
                    callback?.onError(R.string.success_update_comment_file)
                } else {
                    callback?.onError(R.string.error_update_comment_file)
                }
            }
        }
    }

    class DeleteCommentTask(
        private val fileId: Long,
        private val commentId: Int,
        private val callback: VersionListInterface.CommentCallback?,
        private val client: OwnCloudClient
    ) {

        fun execute(scope: CoroutineScope) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    val operation = DeleteCommentRemoteOperation(fileId, commentId)

                    val result = operation.execute(client)
                    result.isSuccess
                }

                if (success) {
                    callback?.onSuccess()
                    // Call error to show success message
                    callback?.onError(R.string.success_delete_comment_file)
                } else {
                    callback?.onError(R.string.error_delete_comment_file)
                }
            }
        }
    }
}
