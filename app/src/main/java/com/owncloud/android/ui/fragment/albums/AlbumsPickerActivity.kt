/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.albums

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.FilesFolderPickerBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.albums.CreateNewAlbumOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter

class AlbumsPickerActivity : FileActivity(), FileFragment.ContainerActivity, Injectable {

    private var captionText: String? = null

    private var action: String? = null

    private lateinit var folderPickerBinding: FilesFolderPickerBinding

    private fun initBinding() {
        folderPickerBinding = FilesFolderPickerBinding.inflate(layoutInflater)
        setContentView(folderPickerBinding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.d(TAG, "onCreate() start")

        super.onCreate(savedInstanceState)

        initBinding()
        setupToolbar()
        setupActionBar()
        setupAction()

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
    }

    private fun setupAction() {
        action = intent.getStringExtra(EXTRA_ACTION)

        setupUIForChooseButton()
    }

    private fun setupUIForChooseButton() {
        captionText = resources.getText(R.string.folder_picker_choose_caption_text).toString()

        folderPickerBinding.folderPickerBtnCopy.visibility = View.GONE
        folderPickerBinding.folderPickerBtnMove.visibility = View.GONE
        folderPickerBinding.folderPickerBtnChoose.visibility = View.GONE
        folderPickerBinding.chooseButtonSpacer.visibility = View.GONE
        folderPickerBinding.moveOrCopyButtonSpacer.visibility = View.GONE
    }

    protected fun createFragments() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, AlbumsFragment.newInstance(isSelectionMode = true), AlbumsFragment.TAG)
        transaction.commit()
    }

    protected val listOfFilesFragment: AlbumsFragment?
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
        if (operation is CreateNewAlbumOperation) {
            onCreateAlbumOperationFinish(operation, result)
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to create a new folder.
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private fun onCreateAlbumOperationFinish(operation: CreateNewAlbumOperation, result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            val fileListFragment = listOfFilesFragment
            fileListFragment?.newAlbumCreated()
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

    companion object {
        private val EXTRA_ACTION = AlbumsPickerActivity::class.java.canonicalName?.plus(".EXTRA_ACTION")
        private val CHOOSE_ALBUM = AlbumsPickerActivity::class.java.canonicalName?.plus(".CHOOSE_ALBUM")

        private val TAG = AlbumsPickerActivity::class.java.simpleName

        fun intentForPickingAlbum(context: FragmentActivity): Intent {
            return Intent(context, AlbumsPickerActivity::class.java).apply {
                putExtra(EXTRA_ACTION, CHOOSE_ALBUM)
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
}
