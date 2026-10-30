/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2011 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.fragment

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.repository.ClientRepository
import com.nextcloud.ui.fileDetail.FileDetailPreview
import com.nextcloud.ui.fileactions.FileAction
import com.nextcloud.ui.fileactions.FileActionHandler
import com.nextcloud.ui.fileactions.FileActionHost
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.HumanReadableFormatter
import com.nextcloud.utils.MenuUtils
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.collectWhenStarted
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.text.DisplayTextFormatter
import com.nextcloud.utils.thumbnail.FolderThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsFragmentBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ToggleFavoriteRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.adapter.progressListener.DownloadProgressListener
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.events.FavoriteEvent
import com.owncloud.android.ui.events.ShareSearchViewFocusEvent
import com.owncloud.android.ui.fragment.filedetail.FileDetailFragmentViewModel
import com.owncloud.android.ui.fragment.util.SharePermissionManager
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class FileDetailFragment :
    FileFragment(),
    View.OnClickListener,
    Injectable {

    // region Dependencies
    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var clientRepository: ClientRepository

    @Inject
    lateinit var storageManager: FileDataStorageManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var fileActionHandler: FileActionHandler

    @Inject
    lateinit var editorUtils: EditorUtils

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var folderThumbnailGenerator: FolderThumbnailGenerator

    @Inject
    lateinit var vmFactory: ViewModelFactory
    // endregion

    // region State
    private var inflatedRootView: View? = null
    private var user: User? = null
    private var parentFolder: OCFile? = null
    private var previewLoaded = false
    private var binding: FileDetailsFragmentBinding? = null
    private var progressListener: DownloadProgressListener? = null
    private var toolbarActivity: ToolbarActivity? = null
    private var activeTab = ACTIVITIES_TAB_POSITION
    private var viewModel: FileDetailFragmentViewModel? = null

    private val fileOperationsHelper: FileOperationsHelper?
        get() = containerActivity?.fileOperationsHelper

    val fileDetailSharingFragment: FileDetailSharingFragment?
        get() = requireActivity().supportFragmentManager.findFragmentByTag(FTAG_SHARING) as FileDetailSharingFragment?

    val fileDetailActivitiesFragment: FileDetailActivitiesFragment? = null

    val isEmpty: Boolean
        get() = file == null || user == null
    // endregion

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, vmFactory)[FileDetailFragmentViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val args = requireNotNull(arguments) { "Arguments may not be null" }
        (activity as? DrawerActivity)?.showBottomNavigationBar(false)
        readArguments(args)
        savedInstanceState?.let(::restoreInstanceState)

        val binding = FileDetailsFragmentBinding.inflate(inflater, container, false)
        this.binding = binding
        inflatedRootView = binding.root
        if (isEmpty) {
            showEmptyContent(binding)
        } else {
            binding.emptyList.emptyListView.visibility = View.GONE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val file = file
        val user = user
        if (file != null && user != null) {
            setupFileDetailViews()
            updateFileDetails(transferring = false, refresh = false)
            viewModel?.observeDownloadProgress(user, file)
        }

        collectDownloadProgress()
        addMenuProvider()
    }

    private fun replaceSharingFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(
                R.id.sharing_frame_container,
                FileDetailSharingFragment.newInstance(file, user),
                FTAG_SHARING
            ).commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_FILE, file)
        outState.putParcelable(ARG_USER, user)
    }

    override fun onStart() {
        super.onStart()
        listenForTransferProgress()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        if (previewLoaded) {
            toolbarActivity?.setPreviewImageVisibility(true)
        }
    }

    override fun onStop() {
        leaveTransferProgress()
        toolbarActivity?.hidePreviewImage()
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ToolbarActivity) {
            toolbarActivity = context
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun getView(): View? = super.getView() ?: inflatedRootView
    // endregion

    // region Setup
    private fun readArguments(args: Bundle) {
        file = args.getParcelableArgument(ARG_FILE, OCFile::class.java)
        parentFolder = args.getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)
        user = args.getParcelableArgument(ARG_USER, User::class.java)
        activeTab = args.getInt(ARG_ACTIVE_TAB, ACTIVITIES_TAB_POSITION)
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        file = savedInstanceState.getParcelableArgument(ARG_FILE, OCFile::class.java)
        user = savedInstanceState.getParcelableArgument(ARG_USER, User::class.java)
    }

    private fun setupFileDetailViews() {
        val binding = binding ?: return
        viewThemeUtils.material.colorProgressBar(binding.progressBar, ColorRole.PRIMARY)
        viewThemeUtils.platform.themeCheckbox(binding.folderSyncButton)
        progressListener = DownloadProgressListener(binding.progressBar)
        binding.run {
            listOf(cancelBtn, favorite, overflowMenu, lastModificationTimestamp, folderSyncButton)
                .forEach { it.setOnClickListener(this@FileDetailFragment) }
        }
    }

    private fun collectDownloadProgress() {
        val viewModel = viewModel ?: return
        viewLifecycleOwner.collectWhenStarted(viewModel.downloadProgress) { onDownloadProgress(it) }
    }

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = Unit

                override fun onPrepareMenu(menu: Menu) = MenuUtils.hideAll(menu)

                override fun onMenuItemSelected(menuItem: MenuItem) = false
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }
    // endregion

    // region File details
    fun updateFileDetails(file: OCFile?, user: User?) {
        this.file = file
        this.user = user
        updateFileDetails(transferring = false, refresh = false)
        if (file != null && user != null) {
            viewModel?.observeDownloadProgress(user, file)
        }
    }

    fun updateFileDetails(transferring: Boolean, refresh: Boolean) {
        if (!isEmpty) {
            val containerStorageManager = containerActivity?.storageManager ?: return
            if (refresh) {
                file = containerStorageManager.getFileByEncryptedRemotePath(file?.remotePath)
            }
            showFileDetails(containerStorageManager, transferring)
        }

        // TODO: 06/21/23 remove this condition after Comments section included (NMC)
        if (SendShareDialog.isPeopleShareClicked) {
            replaceSharingFragment();
        }
        view?.invalidate()
    }

    private fun showFileDetails(containerStorageManager: FileDataStorageManager, transferring: Boolean) {
        val binding = binding
        val file = file
        val user = user
        if (binding == null || file == null || user == null) return

        binding.filename.text = file.fileName
        binding.size.text = HumanReadableFormatter.formatBytes(file.fileLength)
        setFileModificationTimestamp(file, preferences.isShowDetailedTimestampEnabled)
        setFilePreview(file)
        binding.filename.isVisible = previewLoaded
        setFavoriteIconStatus(file.isFavorite)
        updateTransferProgress(binding, file, user, transferring)
        requireActivity().findViewById<FloatingActionButton>(R.id.fab_main)?.hide()
    }

    private fun setFileModificationTimestamp(file: OCFile, showDetailedTimestamp: Boolean) {
        val timestamp = file.modificationTimestamp
        binding?.lastModificationTimestamp?.text = if (showDetailedTimestamp) {
            HumanReadableFormatter.formatDateTime(timestamp)
        } else {
            DisplayTextFormatter.formatRelativeTimestamp(requireContext(), timestamp)
        }
    }

    private fun setFilePreview(file: OCFile) {
        val toolbarActivity = toolbarActivity
        if (toolbarActivity == null) {
            previewLoaded = false
            return
        }

        FileDetailPreview(toolbarActivity, folderThumbnailGenerator, viewThemeUtils).show(file)
        previewLoaded = true
    }

    private fun showEmptyContent(binding: FileDetailsFragmentBinding) {
        binding.emptyList.emptyListView.visibility = View.VISIBLE
        binding.detailContainer.visibility = View.GONE
        binding.emptyList.emptyListViewHeadline.setText(R.string.file_details_no_content)
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_list_empty_error)
        binding.emptyList.emptyListIcon.visibility = View.VISIBLE
    }
    // endregion

    // region Transfer progress
    private fun updateTransferProgress(
        binding: FileDetailsFragmentBinding,
        file: OCFile,
        user: User,
        transferring: Boolean
    ) {
        if (transferring || isDownloading(file, user) || isUploading(file, user)) {
            showTransferProgress(binding, file, user)
        } else {
            // TODO load default preview image; when the local file is removed, the preview remains there
            binding.progressBlock.visibility = View.GONE
        }
    }

    private fun showTransferProgress(binding: FileDetailsFragmentBinding, file: OCFile, user: User) {
        binding.progressBlock.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        when {
            isDownloading(file, user) -> binding.progressText.setText(R.string.downloader_download_in_progress_ticker)
            isUploading(file, user) -> binding.progressText.setText(R.string.uploader_upload_in_progress_ticker)
        }
    }

    private fun isDownloading(file: OCFile, user: User): Boolean =
        FileDownloadHelper.instance().isDownloading(user, file)

    private fun isUploading(file: OCFile, user: User): Boolean =
        FileUploadHelper.instance().isUploading(file.remotePath, user.accountName)

    private fun onDownloadProgress(percent: Int) {
        val binding = binding ?: return
        binding.progressBlock.visibility = View.VISIBLE
        binding.progressText.setText(R.string.downloader_download_in_progress_ticker)
        binding.progressBar.setProgressCompat(percent, true)
    }

    fun listenForTransferProgress() {
        val listener = progressListener
        if (listener == null) {
            Log_OC.d(TAG, "progressListener == null")
            return
        }

        val targetKey = transferProgressTargetKey() ?: return
        containerActivity?.fileUploaderHelper?.addUploadTransferProgressListener(listener, targetKey)
    }

    private fun leaveTransferProgress() {
        val listener = progressListener ?: return
        val targetKey = transferProgressTargetKey() ?: return
        containerActivity?.fileUploaderHelper?.removeUploadTransferProgressListener(listener, targetKey)
    }

    private fun transferProgressTargetKey(): String? {
        val user = user
        val file = file
        if (user == null || file == null) return null
        return FileUploadHelper.buildRemoteName(user.accountName, file.remotePath)
    }
    // endregion

    // region File actions
    override fun onClick(v: View) {
        val file = file ?: return
        when (v.id) {
            R.id.cancelBtn -> (containerActivity as? FileDisplayActivity)?.cancelTransference(file)

            R.id.favorite -> {
                fileOperationsHelper?.toggleFavoriteFile(file, !file.isFavorite)
                setFavoriteIconStatus(!file.isFavorite)
            }

            R.id.overflow_menu -> showFileActions()

            R.id.last_modification_timestamp -> toggleDetailedTimestamp(file)

            R.id.folder_sync_button -> updateInternalFolderSync(file)

            else -> Log_OC.e(TAG, "Incorrect view clicked!")
        }
    }

    private fun showFileActions() {
        val file = file ?: return
        FileActionsBottomSheet.newInstance(file, true, FileAction.getFileDetailActions(file))
            .setResultListener(childFragmentManager, this) { itemId -> onFileActionSelected(itemId) }
            .show(childFragmentManager, FTAG_FILE_ACTIONS)
    }

    private fun onFileActionSelected(@IdRes itemId: Int) {
        val file = file ?: return
        val containerActivity = containerActivity ?: return
        val host = FileActionHost(
            containerActivity,
            parentFragmentManager,
            view,
            parentFolder,
            onFavoriteChanged = ::setFavoriteIconStatus
        )
        fileActionHandler.handle(itemId, listOf(file), host)
    }

    private fun toggleDetailedTimestamp(file: OCFile) {
        val showDetailedTimestamp = !preferences.isShowDetailedTimestampEnabled
        preferences.isShowDetailedTimestampEnabled = showDetailedTimestamp
        setFileModificationTimestamp(file, showDetailedTimestamp)
    }

    private fun updateInternalFolderSync(file: OCFile) {
        val binding = binding ?: return
        file.internalFolderSyncTimestamp = if (binding.folderSyncButton.isChecked) {
            FOLDER_SYNC_ENABLED_TIMESTAMP
        } else {
            FOLDER_SYNC_DISABLED_TIMESTAMP
        }
        storageManager.saveFile(file)
    }
    // endregion

    // region Favorite
    private fun setFavoriteIconStatus(isFavorite: Boolean) {
        val binding = binding ?: return
        val (iconRes, descriptionRes) = if (isFavorite) {
            R.drawable.favorite to R.string.unset_favorite
        } else {
            R.drawable.ic_star_outline to R.string.favorite
        }
        binding.favorite.setImageDrawable(ResourcesCompat.getDrawable(resources, iconRes, null))
        // NMC Customization
        if (!isFavorite) {
            binding.favorite.getDrawable().mutate().setColorFilter(
                requireContext()
                    .resources
                    .getColor(R.color.list_item_lastmod_and_filesize_text, null),
                PorterDuff.Mode.SRC_IN
            )
        }
        binding.favorite.contentDescription = getString(descriptionRes)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: FavoriteEvent) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (!toggleFavoriteOnServer(event)) return@launch
            val file = file ?: return@launch
            file.isFavorite = event.shouldFavorite
            saveFavoriteState(event)
        }
    }

    private suspend fun toggleFavoriteOnServer(event: FavoriteEvent): Boolean {
        val client = clientRepository.getOwncloudClient() ?: return false
        return ToggleFavoriteRemoteOperation(event.shouldFavorite, event.remotePath).execute(client).isSuccess
    }

    private fun saveFavoriteState(event: FavoriteEvent) {
        val storedFile = storageManager.getFileByEncryptedRemotePath(event.remotePath) ?: return
        storedFile.isFavorite = event.shouldFavorite
        storageManager.saveFile(storedFile)
    }
    // endregion

    // region Sharing
    fun initiateSharingProcess(shareeName: String, shareType: ShareType, secureShare: Boolean) {
        val file = file
        if (file == null) {
            SnackbarUtil.show(requireView(), R.string.file_not_found_cannot_share)
            return
        }

        if (file.ownerId == shareeName) {
            SnackbarUtil.show(requireView(), R.string.file_detail_share_already_active)
            return
        }

        val user = user ?: return
        showSharingProcess(
            FileDetailsSharingProcessFragment.newInstance(
                file, shareeName, shareType, secureShare,
                SharePermissionManager.canEditFile(user, storageManager.getCapability(user), file, editorUtils)
            )
        )
    }

    fun editExistingShare(share: OCShare, screenTypePermission: Int, isReshareShown: Boolean) {
        val user = user ?: return
        showSharingProcess(
            FileDetailsSharingProcessFragment.newInstance(
                share, screenTypePermission, isReshareShown,
                SharePermissionManager.canEditFile(user, storageManager.getCapability(user), file, editorUtils)
            )
        )
    }

    private fun showSharingProcess(fragment: FileDetailsSharingProcessFragment) {
        requireActivity().supportFragmentManager.commit {
            replace(R.id.sharing_frame_container, fragment, FileDetailsSharingProcessFragment.TAG)
            addToBackStack(null)
        }
    }

    fun goBackToOCFileListFragment() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    /**
     * hide the view for landscape mode to have more space for the user to type in search view
     * {@link FileDetailSharingFragment#scrollToSearchViewPosition(boolean)}
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ShareSearchViewFocusEvent) {
        binding?.shareDetailFileContainer?.visibility = if (event.hasFocus) View.GONE else View.VISIBLE
    }
    // endregion

    companion object {
        private val TAG: String = FileDetailFragment::class.java.simpleName
        private const val FTAG_FILE_ACTIONS = "actions"
        private const val FTAG_TAG_MANAGEMENT = "tag_management"
        const val FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT"
        private const val FTAG_SHARING = "SHARING_DETAILS_FRAGMENT"
        private const val ARG_FILE = "FILE"
        private const val ARG_PARENT_FOLDER = "PARENT_FOLDER"
        private const val ARG_USER = "USER"
        private const val ARG_ACTIVE_TAB = "TAB"

        private const val ACTIVITIES_TAB_POSITION = 0
        private const val CHIP_CORNER_SIZE = 100f
        private const val FOLDER_SYNC_ENABLED_TIMESTAMP = 0L
        private const val FOLDER_SYNC_DISABLED_TIMESTAMP = -1L

        @JvmStatic
        fun newInstance(fileToDetail: OCFile?, parentFolder: OCFile?, user: User?): FileDetailFragment =
            FileDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FILE, fileToDetail)
                    putParcelable(ARG_PARENT_FOLDER, parentFolder)
                    putParcelable(ARG_USER, user)
                }
            }

        @JvmStatic
        fun newInstance(fileToDetail: OCFile?, user: User?, activeTab: Int): FileDetailFragment =
            FileDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FILE, fileToDetail)
                    putParcelable(ARG_USER, user)
                    putInt(ARG_ACTIVE_TAB, activeTab)
                }
            }
    }
}
