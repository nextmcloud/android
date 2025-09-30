package com.nmc.android.scans

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nmc.android.interfaces.OnDocScanListener
import com.nmc.android.interfaces.OnFragmentChangeListener
import com.nmc.android.scans.ScanDocumentFragment.Companion.newInstance
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityScanBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.operations.CreateFolderIfNotExistOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.DisplayUtils
import io.scanbot.sdk.ScanbotSDK
import androidx.core.graphics.drawable.toDrawable

class ScanActivity : FileActivity(), OnFragmentChangeListener, OnDocScanListener {
    private lateinit var binding: ActivityScanBinding
    lateinit var scanbotSDK: ScanbotSDK

    var remoteFile: OCFile? = null
        private set

    // flag to avoid checking folder existence whenever user goes to save fragment
    // we will make it true when the operation finishes first time
    private var isFolderCheckOperationFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate and set the layout view
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        remoteFile = intent.getParcelableArgument(EXTRA_REMOTE_PATH, OCFile::class.java)
        originalScannedImages.clear()
        filteredImages.clear()
        scannedImagesFilterIndex.clear()
        initScanbotSDK()
        setupToolbar()
        setupActionBar()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupActionBar() {
        val actionBar = delegate.supportActionBar
        actionBar?.let {
            it.setBackgroundDrawable(resources.getColor(R.color.bg_default, null).toDrawable())
            it.setDisplayHomeAsUpEnabled(true)
            viewThemeUtils.files.themeActionBar(this, it, false)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        createScanFragment(savedInstanceState)
    }

    private fun createScanFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            val scanDocumentFragment = newInstance(TAG)
            onReplaceFragment(scanDocumentFragment, FRAGMENT_SCAN_TAG, false)
        } else {
            supportFragmentManager.findFragmentByTag(FRAGMENT_SCAN_TAG)
        }
    }

    override fun onReplaceFragment(fragment: Fragment, tag: String, addToBackStack: Boolean) {
        // only during replacing save scan fragment
        if (tag.equals(FRAGMENT_SAVE_SCAN_TAG, ignoreCase = true)) {
            checkAndCreateFolderIfRequired()
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.scan_frame_container, fragment, tag)
        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }
        transaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressHandle()
        }
        return super.onOptionsItemSelected(item)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressHandle()
        }
    }

    private fun onBackPressHandle() {
        val editScanFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_EDIT_SCAN_TAG)
        val cropScanFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_CROP_SCAN_TAG)
        val saveScanFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_SAVE_SCAN_TAG)
        if (cropScanFragment != null || saveScanFragment != null) {
            var index = 0
            if (cropScanFragment is CropScannedDocumentFragment) {
                index = cropScanFragment.getScannedDocIndex()
            }
            onReplaceFragment(EditScannedDocumentFragment.newInstance(index), FRAGMENT_EDIT_SCAN_TAG, false)
        } else if (editScanFragment != null) {
            createScanFragment(null)
        } else {
            finish()
        }
    }

    private fun initScanbotSDK() {
        scanbotSDK = ScanbotSDK(this)
    }

    override fun addScannedDoc(file: Bitmap?) {
        file?.let {
            originalScannedImages.add(it)
            filteredImages.add(it)
            scannedImagesFilterIndex.add(0) // no filter by default
        }
    }

    override fun getScannedDocs(): List<Bitmap> {
        return filteredImages
    }

    override fun removedScannedDoc(file: Bitmap?, index: Int): Boolean {
        //removed the filter applied index also when scanned document is removed
        if (scannedImagesFilterIndex.isNotEmpty() && scannedImagesFilterIndex.size > index) {
            scannedImagesFilterIndex.removeAt(index)
        }
        if (originalScannedImages.isNotEmpty() && file != null) {
            originalScannedImages.removeAt(index)
        }
        if (filteredImages.isNotEmpty() && file != null) {
            filteredImages.removeAt(index)
            return true
        }
        return false
    }

    override fun replaceScannedDoc(index: Int, newFile: Bitmap?, isFilterApplied: Boolean): Bitmap? {
        //only update the original bitmap if no filter is applied
        if (!isFilterApplied && originalScannedImages.isNotEmpty() && newFile != null && index >= 0 && originalScannedImages.size - 1 >= index) {
            originalScannedImages[index] = newFile
        }
        if (filteredImages.isNotEmpty() && newFile != null && index >= 0 && filteredImages.size - 1 >= index) {
            return filteredImages.set(index, newFile)
        }
        return null
    }

    override fun replaceFilterIndex(index: Int, filterIndex: Int) {
        if (scannedImagesFilterIndex.isNotEmpty() && scannedImagesFilterIndex.size > index) {
            scannedImagesFilterIndex[index] = filterIndex
        }
    }

    private fun checkAndCreateFolderIfRequired() {
        val remotePath = remoteFile?.remotePath

        //if user is coming from sub-folder then we should not check for existence as folder will be available
        if (!TextUtils.isEmpty(remotePath) && remotePath != OCFile.ROOT_PATH) {
            return
        }

        //no need to do any operation if its already finished earlier
        if (isFolderCheckOperationFinished) {
            return
        }

        val lastRemotePath = appPreferences.uploadScansLastPath

        //create the default scan folder if it doesn't exist or if user has not selected any other folder
        if (lastRemotePath.equals(DEFAULT_UPLOAD_SCAN_PATH, ignoreCase = true)) {
            fileOperationsHelper.createFolderIfNotExist(lastRemotePath, false)
            return
        }

        //if last saved remote path is not root path then we have to check if the folder exist or not
        if (lastRemotePath != OCFile.ROOT_PATH) {
            fileOperationsHelper.createFolderIfNotExist(lastRemotePath, true)
        }
    }

    // NMC-3670
    // check if selected folder is encrypted and e2ee is configured or not
    fun checkEncryption(file: OCFile, resultListener: (success: Boolean) -> Unit) {
        // get file from storage to have the encrypted information
        // as we are making OCFile without the flag {see-> SaveScannedDocumentFragment.setRemoteFilePath()}
        var remoteFile = storageManager.getFileByEncryptedRemotePath(file.remotePath)
        // there can be case where the remoteFile can be null
        if (remoteFile == null) {
            remoteFile = file
        }

        if (!remoteFile.isEncrypted) {
            resultListener(true)
            return
        }

        if (remoteFile.isEncrypted) {
            val user = user.orElseThrow { RuntimeException() }

            // check if e2e app is enabled
            val ocCapability: OCCapability = storageManager
                .getCapability(user.accountName)

            if (ocCapability.endToEndEncryption.isFalse ||
                ocCapability.endToEndEncryption.isUnknown
            ) {
                DisplayUtils.showSnackMessage(this, R.string.end_to_end_encryption_not_enabled)
                resultListener(false)
                return
            }
            // check if keys are stored
            if (FileOperationsHelper.isEndToEndEncryptionSetup(this, user)) {
                resultListener(true)
            } else {
                val setupEncryptionDialogFragment = SetupEncryptionDialogFragment.newInstance(user, -1)
                supportFragmentManager.setFragmentResultListener(
                    SetupEncryptionDialogFragment.RESULT_REQUEST_KEY,
                    this
                ) { requestKey, result ->
                    if (requestKey == SetupEncryptionDialogFragment.RESULT_REQUEST_KEY) {
                        resultListener(
                            !result.getBoolean(SetupEncryptionDialogFragment.RESULT_KEY_CANCELLED, false)
                                && result.getBoolean(SetupEncryptionDialogFragment.SUCCESS, false)
                        )
                    }
                }
                setupEncryptionDialogFragment.show(
                    supportFragmentManager,
                    SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG
                )
            }
        }
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)
        if (operation is CreateFolderIfNotExistOperation) {
            //we are only handling callback when we are checking if folder exist or not to update the UI
            //in case the folder doesn't exist (user has deleted)
            if (!result.isSuccess && result.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
                val saveScanFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_SAVE_SCAN_TAG)
                if (saveScanFragment != null && saveScanFragment.isVisible) {
                    //update the root path in preferences as well
                    //so that next time folder issue won't come
                    appPreferences.uploadScansLastPath = OCFile.ROOT_PATH
                    //if folder doesn't exist then we have to set the remote path as root i.e. fallback mechanism
                    (saveScanFragment as SaveScannedDocumentFragment).setRemoteFilePath(OCFile.ROOT_PATH)
                }
            }
            // NMC-4746 fix
            else if (result.isSuccess) {
                val saveScanFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_SAVE_SCAN_TAG)
                if (saveScanFragment != null && saveScanFragment.isVisible) {
                    // when folder creation is success update the path in save fragment
                    (saveScanFragment as SaveScannedDocumentFragment).setRemoteFilePath(appPreferences.uploadScansLastPath)
                }
            }
            isFolderCheckOperationFinished = true
        }
    }

    companion object {
        const val FRAGMENT_SCAN_TAG: String = "SCAN_FRAGMENT_TAG"
        const val FRAGMENT_EDIT_SCAN_TAG: String = "EDIT_SCAN_FRAGMENT_TAG"
        const val FRAGMENT_CROP_SCAN_TAG: String = "CROP_SCAN_FRAGMENT_TAG"
        const val FRAGMENT_SAVE_SCAN_TAG: String = "SAVE_SCAN_FRAGMENT_TAG"

        // default path to upload the scanned document
        // if user doesn't select any location then this will be the default location
        const val DEFAULT_UPLOAD_SCAN_PATH: String = OCFile.ROOT_PATH + "Scans" + OCFile.PATH_SEPARATOR

        const val TAG: String = "ScanActivity"
        private const val EXTRA_REMOTE_PATH = "com.nmc.android.scans.scan_activity.extras.remote_path"

        @JvmField
        val originalScannedImages: MutableList<Bitmap> = ArrayList() //list with original bitmaps

        @JvmField
        val filteredImages: MutableList<Bitmap> = ArrayList() //list with bitmaps applied filters

        @JvmField
        val scannedImagesFilterIndex: MutableList<Int> = ArrayList() //list to maintain the state of
        // applied filter index when device rotated

        @JvmStatic
        fun openScanActivity(context: Context, remoteFile: OCFile, requestCode: Int) {
            val intent = Intent(context, ScanActivity::class.java)
            intent.putExtra(EXTRA_REMOTE_PATH, remoteFile)
            (context as AppCompatActivity).startActivityForResult(intent, requestCode)
        }
    }
}
