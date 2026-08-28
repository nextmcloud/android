/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.FilesFolderPickerBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.operations.albums.CreateNewAlbumRemoteOperation
import com.owncloud.android.ui.activity.FolderPickerActivity.Companion.TAG_LIST_OF_FOLDERS
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.albums.AlbumsFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter

class AlbumsPickerActivity : FileActivity(), FileFragment.ContainerActivity, OnEnforceableRefreshListener, Injectable {

    private var captionText: String? = null

    private var action: String? = null

    private lateinit var folderPickerBinding: FilesFolderPickerBinding

    private var targetFilePaths: ArrayList<String>? = null
    private var albumName: String? = null

    private fun initBinding() {
        folderPickerBinding = FilesFolderPickerBinding.inflate(layoutInflater)
        setContentView(folderPickerBinding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.d(TAG, "onCreate() start")

        super.onCreate(savedInstanceState)

        initBinding()
        setupToolbar()
        showHideDefaultToolbarDivider(true)
        setupAction()
        setupActionBar()
        initExtras()

        if (savedInstanceState == null) {
            createFragments()
        }

        updateActionBarTitleAndHomeButtonByString(captionText)
    }

    private fun setupActionBar() {
        findViewById<View>(R.id.sort_list_button_group).visibility =
            View.GONE
        findViewById<View>(R.id.switch_grid_view_button).visibility =
            View.GONE
        supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
            captionText?.let {
                viewThemeUtils.files.themeActionBar(this, actionBar, it)
            }
        }
    }

    private fun initExtras() {
        targetFilePaths = intent.getStringArrayListExtra(EXTRA_FILE_PATHS)
        albumName = intent.getStringExtra(EXTRA_ALBUM_NAME)
    }


    private fun setupAction() {
        action = intent.getStringExtra(EXTRA_ACTION)
        setupUIForChooseButton()
    }

    private fun setupUIForChooseButton() {
        if (action == CHOOSE_ALBUM) {
            captionText = resources.getText(R.string.album_picker_toolbar_title).toString()
        } else if (action == CHOOSE_MEDIA_FILES) {
            captionText = resources.getText(R.string.media_picker_toolbar_title).toString()
        }

        folderPickerBinding.bottomLayout.visibility = View.GONE
        folderPickerBinding.divider.visibility = View.GONE
    }

    private fun createFragments() {
        if (action == CHOOSE_ALBUM) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.fragment_container,
                AlbumsFragment.newInstance(isSelectionMode = true),
                AlbumsFragment.TAG
            )
            transaction.commit()
        } else if (action == CHOOSE_MEDIA_FILES) {
            createGalleryFragment()
        }
    }

    private fun createGalleryFragment() {
        val photoFragment = GalleryFragment()
        val bundle = Bundle()
        bundle.putParcelable(
            OCFileListFragment.SEARCH_EVENT,
            SearchEvent("image/%", SearchRemoteOperation.SearchType.PHOTO_SEARCH)
        )
        bundle.putBoolean(EXTRA_FROM_ALBUM, true)
        photoFragment.arguments = bundle

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, photoFragment, TAG_LIST_OF_FOLDERS)
        transaction.commit()
    }

    private val listOfFilesFragment: AlbumsFragment?
        get() {
            val listOfFiles = supportFragmentManager.findFragmentByTag(AlbumsFragment.TAG)

            return if (listOfFiles != null) {
                return listOfFiles as AlbumsFragment?
            } else {
                Log_OC.e(TAG, "Access to non existing list of albums fragment!!")
                null
            }
        }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)
        if (operation is CreateNewAlbumRemoteOperation) {
            onCreateAlbumOperationFinish(operation, result)
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to create a new folder.
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    @Suppress("MaxLineLength")
    private fun onCreateAlbumOperationFinish(
        operation: CreateNewAlbumRemoteOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            val fileListFragment = listOfFilesFragment
            fileListFragment?.refreshAlbums()
        } else {
            try {
                DisplayUtils.showSnackMessage(
                    this,
                    ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources)
                )
            } catch (e: Resources.NotFoundException) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e)
            }
        }
    }

    override fun showDetails(file: OCFile?) {
        // not used at the moment
    }

    override fun showDetails(file: OCFile?, activeTab: Int) {
        // not used at the moment
    }

    override fun onBrowsedDownTo(folder: OCFile?) {
        // not used at the moment
    }

    override fun onTransferStateChanged(file: OCFile?, downloading: Boolean, uploading: Boolean) {
        // not used at the moment
    }

    fun addFilesToAlbum(albumName: String?) {
        targetFilePaths?.let {
            fileOperationsHelper.albumCopyFiles(it, albumName)
        }
    }

    fun addFilesToAlbum(files: Collection<OCFile>) {
        val paths: List<String> = files.map { it.remotePath }
        albumName?.let {
            fileOperationsHelper.albumCopyFiles(paths, it)
        }
    }

    companion object {
        private val EXTRA_ACTION = AlbumsPickerActivity::class.java.canonicalName?.plus(".EXTRA_ACTION")
        private val CHOOSE_ALBUM = AlbumsPickerActivity::class.java.canonicalName?.plus(".CHOOSE_ALBUM")
        private val CHOOSE_MEDIA_FILES = AlbumsPickerActivity::class.java.canonicalName?.plus(".CHOOSE_MEDIA_FILES")
        private val EXTRA_FILE_PATHS = FolderPickerActivity::class.java.canonicalName?.plus(".EXTRA_FILE_PATHS")
        private val EXTRA_ALBUM_NAME = FolderPickerActivity::class.java.canonicalName?.plus(".EXTRA_ALBUM_NAME")
        val EXTRA_FROM_ALBUM = AlbumsPickerActivity::class.java.canonicalName?.plus(".EXTRA_FROM_ALBUM")

        private val TAG = AlbumsPickerActivity::class.java.simpleName

        fun intentForPickingAlbum(context: FragmentActivity, paths: ArrayList<String>): Intent {
            return Intent(context, AlbumsPickerActivity::class.java).apply {
                putExtra(EXTRA_ACTION, CHOOSE_ALBUM)
                putStringArrayListExtra(EXTRA_FILE_PATHS, paths)
            }
        }

        fun intentForPickingMediaFiles(context: FragmentActivity, albumName: String): Intent {
            return Intent(context, AlbumsPickerActivity::class.java).apply {
                putExtra(EXTRA_ACTION, CHOOSE_MEDIA_FILES)
                putExtra(EXTRA_ALBUM_NAME, albumName)
            }
        }
    }

    override fun onRefresh(enforced: Boolean) {
        // do nothing
    }

    override fun onRefresh() {
        // do nothing
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> super.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
