/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.uploadList

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadEventBroadcaster
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.Throttler
import com.nextcloud.ui.component.UploadWarningCard
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.nmc.android.utils.SwipeRefreshThemeUtils
import com.nmc.android.utils.DisplayUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentUploadListBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.operations.factory.UploadFileOperationFactory
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.adapter.uploadList.UploadListAdapter
import com.owncloud.android.ui.adapter.uploadList.helper.UploadListAdapterHelper
import com.owncloud.android.ui.adapter.uploadList.helper.UploadListItemOnClick
import com.owncloud.android.ui.decoration.MediaGridItemDecoration
import com.owncloud.android.ui.decoration.SimpleListItemDividerDecoration
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject
import com.nextcloud.utils.AppConstants.ONE_SECOND

@Suppress("TooManyFunctions")
class UploadListFragment :
    Fragment(),
    UploadListItemOnClick {

    @Inject lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject lateinit var powerManagementService: PowerManagementService

    @Inject lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    @Inject lateinit var throttler: Throttler

    @Inject lateinit var uploadFileOperationFactory: UploadFileOperationFactory

    @Inject lateinit var thumbnailGenerator: ThumbnailGenerator

    @Inject lateinit var userAccountManager: UserAccountManager

    @Inject lateinit var connectivityService: ConnectivityService

    @Inject lateinit var backgroundJobManager: BackgroundJobManager

    @Inject lateinit var preferences: AppPreferences

    @Inject lateinit var viewThemeUtils: ViewThemeUtils

    private var binding: FragmentUploadListBinding? = null
    private var uploadWarningCard: UploadWarningCard? = null
    private lateinit var uploadListAdapter: UploadListAdapter
    private lateinit var conflictHandler: UploadListConflictHandler
    private lateinit var credentialsHandler: UploadListCredentialsHandler

    private var simpleListItemDividerDecoration : SimpleListItemDividerDecoration? = null

    private val navigatorActivity: NavigatorActivity?
        get() = getTypedActivity(NavigatorActivity::class.java)

    private val credentialsUpdateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                restartUploadsIfNeeded()
            }
        }

    private val uploadFinishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            throttler.run(UPDATE_UPLOAD_LIST_KEY) { uploadListAdapter.loadUploadItemsFromDb() }
        }
    }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        throttler.intervalMillis = ONE_SECOND.inWholeMilliseconds
        if (savedInstanceState == null) {
            setUserFromIntent()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentUploadListBinding.inflate(inflater, container, false)
        val binding = binding!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = navigatorActivity ?: return
        val binding = binding ?: return

        setupWarningCard(activity, binding)
        setupEmptyList(binding)
        setupList(activity, binding)
        addMenuProvider()
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter().apply {
            addAction(FileUploadEventBroadcaster.ACTION_UPLOAD_ENQUEUED)
            addAction(FileUploadEventBroadcaster.ACTION_UPLOAD_STARTED)
            addAction(FileUploadEventBroadcaster.ACTION_UPLOAD_COMPLETED)
        }
        localBroadcastManager.registerReceiver(uploadFinishReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        binding?.autoUploadBatterySaverWarningCard?.let { uploadWarningCard?.bind(it) }
        loadItems()
    }

    override fun onStop() {
        localBroadcastManager.unregisterReceiver(uploadFinishReceiver)
        super.onStop()
    }

    override fun onDestroyView() {
        uploadWarningCard?.unregister(requireContext())
        uploadWarningCard = null
        super.onDestroyView()
        binding = null
    }
    // endregion

    // region UploadListItemOnClick
    override fun onLastUploadResultConflictClick(upload: OCUpload) {
        conflictHandler.handleConflict(upload)
    }

    override fun onCredentialErrorClick(user: User) {
        credentialsHandler.checkCredentials(user)
    }
    // endregion

    // region Setup
    private fun setUserFromIntent() {
        val user = activity?.intent?.getParcelableArgument(FileActivity.EXTRA_USER, User::class.java) ?: return
        navigatorActivity?.setUser(user)
    }

    private fun setupWarningCard(activity: NavigatorActivity, binding: FragmentUploadListBinding) {
        uploadWarningCard = UploadWarningCard(
            activity,
            powerManagementService,
            syncedFolderProvider,
            backgroundJobManager,
            viewLifecycleOwner.lifecycleScope,
            viewThemeUtils
        ).apply {
            register(activity, binding.autoUploadBatterySaverWarningCard)
        }
    }

    private fun setupEmptyList(binding: FragmentUploadListBinding) {
        binding.list.setEmptyView(binding.emptyList.root)
        binding.emptyList.run {
            root.visibility = View.GONE

            emptyListIcon.run {
                setImageResource(R.drawable.uploads)
                drawable.mutate()
                alpha = EMPTY_LIST_ICON_ALPHA
                visibility = View.VISIBLE
            }

            emptyListViewHeadline.text = getString(R.string.upload_list_empty_headline)

            emptyListViewText.run {
                text = getString(R.string.upload_list_empty_text_auto_upload)
                visibility = View.VISIBLE
            }
        }
    }

    private fun setupList(activity: NavigatorActivity, binding: FragmentUploadListBinding) {
        val adapterHelper = UploadListAdapterHelper(activity)
        uploadListAdapter = UploadListAdapter(
            activity,
            activity.storageManager,
            uploadsStorageManager,
            userAccountManager,
            connectivityService,
            powerManagementService,
            viewThemeUtils,
            this,
            adapterHelper,
            thumbnailGenerator
        )
        conflictHandler = UploadListConflictHandler(
            this,
            uploadsStorageManager,
            uploadFileOperationFactory,
            adapterHelper,
            uploadListAdapter
        )
        credentialsHandler = UploadListCredentialsHandler(this, backgroundJobManager, credentialsUpdateLauncher) {
            restartUploadsIfNeeded()
        }

        val layoutManager = GridLayoutManager(activity, SPAN_COUNT)
        uploadListAdapter.setLayoutManager(layoutManager)

        // NMC decorator
        simpleListItemDividerDecoration = SimpleListItemDividerDecoration(activity, R.drawable.item_divider, true)

        val spacing = resources.getDimensionPixelSize(R.dimen.media_grid_spacing)
        binding.list.run {
            addItemDecoration(MediaGridItemDecoration(spacing))
            setLayoutManager(layoutManager)
            addListItemDecorator()
            adapter = uploadListAdapter
        }

        // NMC Customisation
        SwipeRefreshThemeUtils.themeSwipeRefreshLayout(activity, binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener { refresh() }
    }

    private fun addListItemDecorator() {
        if (DisplayUtils.isShowDividerForList(requireContext())) {
            //check and remove divider item decorator if exist then add item decorator
            removeListDividerDecorator()
            simpleListItemDividerDecoration?.let { binding?.list?.addItemDecoration(it) }
        }
    }

    /**
     * method to remove the divider item decorator
     */
    private fun removeListDividerDecorator() {
        binding?.list?.itemDecorationCount?.let {
            if (it > 0) {
                simpleListItemDividerDecoration?.let { decor -> binding?.list?.removeItemDecoration(decor) }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addMenuProvider() {
        val activity = requireActivity()
        val menuProvider = UploadListMenuProvider(
            activity,
            preferences,
            userAccountManager,
            uploadsStorageManager
        ) { uploadListAdapter.notifyDataSetChanged() }

        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    // endregion

    // region Uploads
    private fun loadItems() {
        val binding = binding ?: return
        binding.swipeContainingList.isRefreshing = true
        uploadListAdapter.loadUploadItemsFromDb { this.binding?.swipeContainingList?.isRefreshing = false }
    }

    private fun refresh() {
        FileUploadHelper.instance().retryFailedUploads(
            uploadsStorageManager,
            connectivityService,
            userAccountManager,
            powerManagementService
        )

        loadItems()
    }

    private fun restartUploadsIfNeeded() {
        FilesSyncHelper.restartUploadsIfNeeded(
            uploadsStorageManager,
            userAccountManager,
            connectivityService,
            powerManagementService
        )
    }
    // endregion

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //this should only run when device is not tablet because we are adding dividers in tablet for both the
        // orientations
        if (!DisplayUtils.isTablet(requireContext())) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //add the divider item decorator when orientation is landscape
                addListItemDecorator()
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                //remove the divider item decorator when orientation is portrait
                removeListDividerDecorator()
            }
        }
    }

    companion object {
        private const val SPAN_COUNT = 1
        private const val EMPTY_LIST_ICON_ALPHA = 0.5f
        private const val UPDATE_UPLOAD_LIST_KEY = "update_upload_list"
    }
}
