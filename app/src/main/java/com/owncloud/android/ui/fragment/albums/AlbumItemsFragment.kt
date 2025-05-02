/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.Throttler
import com.nextcloud.ui.albumItemActions.AlbumItemActionsBottomSheet
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.albums.ReadAlbumItemsOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.GalleryAdapter
import com.owncloud.android.ui.dialog.CreateAlbumDialogFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.Optional
import javax.inject.Inject

class AlbumItemsFragment : Fragment(), OCFileListFragmentInterface, Injectable {

    private var adapter: GalleryAdapter? = null
    private var client: OwnCloudClient? = null
    private var optionalUser: Optional<User>? = null

    private lateinit var binding: ListFragmentBinding

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var throttler: Throttler

    private var mContainerActivity: FileFragment.ContainerActivity? = null

    private var columnSize = 0

    private lateinit var albumName: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mContainerActivity = context as FileFragment.ContainerActivity
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                context.toString() + " must implement " +
                    FileFragment.ContainerActivity::class.java.simpleName, e
            )
        }
        arguments?.let {
            albumName = it.getString(ARG_ALBUM_NAME) ?: ""
        }
    }

    override fun onDetach() {
        mContainerActivity = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        columnSize = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            maxColumnSizeLandscape;
        } else {
            maxColumnSizePortrait;
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        optionalUser = Optional.of(accountManager.user)
        if (optionalUser?.isPresent == false) {
            showError()
        }
        createMenu()
        setupContainingList()
        setupContent()
    }

    private fun createMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_album_items, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_three_dot_icon -> {
                        openActionsMenu()
                        true
                    }

                    R.id.action_add_more_photos -> {
                        // open Gallery fragment as selection then add items to current album
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun openActionsMenu() {
        throttler.run("overflowClick") {
            val supportFragmentManager = requireActivity().supportFragmentManager

            AlbumItemActionsBottomSheet.newInstance()
                .setResultListener(
                    supportFragmentManager,
                    this
                ) { id: Int ->
                    onFileActionChosen(id)
                }
                .show(supportFragmentManager, "album_actions")
        }
    }

    private fun onFileActionChosen(@IdRes itemId: Int): Boolean {
        return when (itemId) {
            // action to rename album
            R.id.action_rename_file -> {
                CreateAlbumDialogFragment.newInstance(albumName)
                    .show(
                        requireActivity().supportFragmentManager,
                        CreateAlbumDialogFragment.TAG
                    )
                true
            }

            // action to delete album
            R.id.action_delete -> {
                mContainerActivity?.getFileOperationsHelper()?.removeAlbum(albumName);
                true
            }

            else -> false
        }
    }

    private fun showError() {
        requireActivity().runOnUiThread {
            setMessageForEmptyList(
                R.string.albums_no_results_headline,
                resources.getString(R.string.account_not_found),
                R.drawable.ic_notification,
                false
            )
        }
        return
    }

    private fun setupContent() {
        binding.listRoot.setEmptyView(binding.emptyList.emptyListView)
        val layoutManager = GridLayoutManager(requireContext(), 1)
        binding.listRoot.layoutManager = layoutManager
        fetchAndSetData()
    }

    private fun setupContainingList() {
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener {
            binding.swipeContainingList.isRefreshing = true
            fetchAndSetData()
        }
    }

    @VisibleForTesting
    fun populateList(albums: List<OCFile>) {
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).setMainFabVisible(false)
        }
        initializeAdapter()
        adapter?.showAlbumItems(albums)
    }

    private fun fetchAndSetData() {
        initializeAdapter()
        val t = Thread {
            setEmptyListLoadingMessage()
            val getRemoteNotificationOperation = ReadAlbumItemsOperation(albumName, mContainerActivity?.storageManager)
            val result = client?.let { getRemoteNotificationOperation.execute(it) }
            if (result?.isSuccess == true && result.resultData != null) {
                if (result.resultData.isEmpty()) {
                    setMessageForEmptyList(
                        R.string.albums_no_results_headline,
                        resources.getString(R.string.albums_no_results_message),
                        R.drawable.ic_notification,
                        false
                    )
                } else {
                    requireActivity().runOnUiThread { populateList(result.resultData) }
                }
            } else {
                Log_OC.d(TAG, result?.logMessage)
                // show error
                setMessageForEmptyList(
                    R.string.albums_no_results_headline,
                    result?.getLogMessage(requireContext()) ?: resources.getString(R.string.albums_no_results_message),
                    R.drawable.ic_notification,
                    false
                )
            }
            hideRefreshLayoutLoader()
        }
        t.start()
    }

    private fun hideRefreshLayoutLoader() {
        requireActivity().runOnUiThread {
            binding.swipeContainingList.isRefreshing = false
        }
    }

    private fun setEmptyListLoadingMessage() {
        Handler(Looper.getMainLooper()).post {
            val fileActivity = this.getTypedActivity(FileActivity::class.java)
            fileActivity?.connectivityService?.isNetworkAndServerAvailable { result: Boolean? ->
                if (!result!!) return@isNetworkAndServerAvailable
                binding.emptyList.emptyListViewHeadline.setText(R.string.file_list_loading)
                binding.emptyList.emptyListViewText.text = ""
                binding.emptyList.emptyListIcon.visibility = View.GONE
            }
        }
    }

    private fun initializeClient() {
        if (client == null && optionalUser?.isPresent == true) {
            try {
                val user = optionalUser?.get()
                client = clientFactory.create(user)
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Error initializing client", e)
            }
        }
    }

    private fun initializeAdapter() {
        initializeClient()
        if (adapter == null) {
            adapter = GalleryAdapter(
                requireContext(),
                accountManager.user,
                this,
                preferences,
                mContainerActivity!!,
                viewThemeUtils,
                columnSize,
                ThumbnailsCacheManager.getThumbnailDimension()
            )
            adapter?.setHasStableIds(true)
        }
        binding.listRoot.adapter = adapter

        lastMediaItemPosition?.let {
            binding.listRoot.layoutManager?.scrollToPosition(it)
        }
    }

    private fun setMessageForEmptyList(
        @StringRes headline: Int, message: String,
        @DrawableRes icon: Int, tintIcon: Boolean
    ) {
        Handler(Looper.getMainLooper()).post {
            binding.emptyList.emptyListViewHeadline.setText(headline)
            binding.emptyList.emptyListViewText.text = message

            if (tintIcon) {
                if (context != null) {
                    binding.emptyList.emptyListIcon.setImageDrawable(
                        viewThemeUtils.platform.tintPrimaryDrawable(requireContext(), icon)
                    )
                }
            } else {
                binding.emptyList.emptyListIcon.setImageResource(icon)
            }

            binding.emptyList.emptyListIcon.visibility = View.VISIBLE
            binding.emptyList.emptyListViewText.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).setupToolbar()
            (requireActivity() as FileDisplayActivity).updateActionBarTitleAndHomeButtonByString(albumName)
            (requireActivity() as FileDisplayActivity).showSortListGroup(false)
            (requireActivity() as FileDisplayActivity).setMainFabVisible(false)

            // clear the subtitle while navigating to any other screen from Media screen
            (requireActivity() as FileDisplayActivity).clearToolbarSubtitle()
        }
    }

    override fun onPause() {
        super.onPause()
        adapter?.cancelAllPendingTasks()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            columnSize = maxColumnSizeLandscape
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            columnSize = maxColumnSizePortrait
        }
        adapter?.changeColumn(columnSize)
        adapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lastMediaItemPosition = 0
    }

    companion object {
        val TAG: String = AlbumItemsFragment::class.java.simpleName
        private const val ARG_ALBUM_NAME = "album_name"
        var lastMediaItemPosition: Int? = null

        private const val maxColumnSizeLandscape: Int = 5
        private const val maxColumnSizePortrait: Int = 2

        fun newInstance(albumName: String): AlbumItemsFragment {
            val args = Bundle()

            val fragment = AlbumItemsFragment()
            fragment.arguments = args
            args.putString(ARG_ALBUM_NAME, albumName)
            return fragment
        }
    }

    override fun getColumnsCount(): Int {
        return columnSize
    }

    override fun onShareIconClick(file: OCFile?) {
        TODO("Not yet implemented")
    }

    override fun showShareDetailView(file: OCFile?) {
        TODO("Not yet implemented")
    }

    override fun showActivityDetailView(file: OCFile?) {
        TODO("Not yet implemented")
    }

    override fun onOverflowIconClicked(file: OCFile?, view: View?) {
        TODO("Not yet implemented")
    }

    override fun onItemClicked(file: OCFile?) {
        TODO("Not yet implemented")
    }

    override fun onLongItemClicked(file: OCFile?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isLoading(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onHeaderClicked() {
        TODO("Not yet implemented")
    }

    fun onAlbumRenamed(newAlbumName: String) {
        albumName = newAlbumName
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).updateActionBarTitleAndHomeButtonByString(albumName)
        }
    }

    fun onAlbumDeleted() {
        requireActivity().supportFragmentManager.popBackStack()
    }
}