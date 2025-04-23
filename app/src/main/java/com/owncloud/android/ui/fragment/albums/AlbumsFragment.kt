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
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.albums.ReadAlbumsOperation
import com.owncloud.android.operations.albums.ReadAlbumsOperation.PhotoAlbumEntry
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.AlbumsAdapter
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.Optional
import javax.inject.Inject

class AlbumsFragment : Fragment(), Injectable {

    private var adapter: AlbumsAdapter? = null
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

    private var mContainerActivity: FileFragment.ContainerActivity? = null

    private var isGridView = true
    private var maxColumnSize = 2

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
    }

    override fun onDetach() {
        mContainerActivity = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        optionalUser = Optional.of(accountManager.user)
        if (optionalUser?.isPresent == false) {
            showError()
        }
        setupContainingList()
        setupContent()
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
        binding.listRoot.setHasFixedSize(true)
        if (isGridView) {
            val layoutManager = GridLayoutManager(requireContext(), maxColumnSize)
            binding.listRoot.layoutManager = layoutManager
        } else {
            val layoutManager = LinearLayoutManager(requireContext())
            binding.listRoot.layoutManager = layoutManager
        }
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
    fun populateList(albums: List<PhotoAlbumEntry>?) {
        initializeAdapter()
        adapter?.setAlbumItems(albums)
    }

    private fun fetchAndSetData() {
        val t = Thread {
            setEmptyListLoadingMessage()
            requireActivity().runOnUiThread { initializeAdapter() }
            val getRemoteNotificationOperation = ReadAlbumsOperation()
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
            adapter = AlbumsAdapter(
                requireContext(),
                mContainerActivity?.storageManager,
                accountManager.user,
                syncedFolderProvider,
                preferences,
                viewThemeUtils,
                isGridView
            )
            binding.listRoot.adapter = adapter
        }
    }

    private fun setMessageForEmptyList(
        @StringRes headline: Int, message: String,
        @DrawableRes icon: Int, tintIcon: Boolean
    ) {
        Handler(Looper.getMainLooper()).post {
            binding.emptyList.emptyListViewHeadline.setText(headline)
            binding.emptyList.emptyListViewText.setText(message)

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

    override fun onResume() {
        super.onResume()
        if (requireActivity() is FileDisplayActivity) {
            (requireActivity() as FileDisplayActivity).setupHomeSearchToolbarWithSortAndListButtons()
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

    private val isGridEnabled: Boolean
        get() {
            return binding.listRoot.layoutManager is GridLayoutManager
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isGridEnabled) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                maxColumnSize = 4
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                maxColumnSize = 2
            }
            (binding.listRoot.layoutManager as GridLayoutManager).setSpanCount(maxColumnSize)
        }
    }

    companion object {
        val TAG: String = AlbumsFragment::class.java.simpleName
    }
}