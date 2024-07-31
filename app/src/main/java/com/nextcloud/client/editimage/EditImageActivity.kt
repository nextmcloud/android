/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.editimage

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageView
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityEditImageBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeType
import java.io.File

class EditImageActivity :
    FileActivity(),
    OnRemoteOperationListener,
    CropImageView.OnSetImageUriCompleteListener,
    CropImageView.OnCropImageCompleteListener,
    Injectable {

    private lateinit var binding: ActivityEditImageBinding
    private lateinit var file: OCFile
    private lateinit var format: Bitmap.CompressFormat

    companion object {
        const val EXTRA_FILE = "FILE"
        const val OPEN_IMAGE_EDITOR = "OPEN_IMAGE_EDITOR"

        private val supportedMimeTypes = arrayOf(
            MimeType.PNG,
            MimeType.JPEG,
            MimeType.WEBP,
            MimeType.TIFF,
            MimeType.BMP,
            MimeType.HEIC
        )

        fun canBePreviewed(file: OCFile): Boolean {
            return file.mimeType in supportedMimeTypes
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        file = intent.extras?.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
            ?: throw IllegalArgumentException("Missing file argument")

        //NMC Customization
        setupToolbar()
        setupActionBar()

        setupCropper()
    }

    //NMC Customization
    private fun setupActionBar() {
        supportActionBar?.let {
            viewThemeUtils.platform.themeStatusBar(this)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
            //custom color for back arrow for NMC
            viewThemeUtils.files.themeActionBar(this, it, file.fileName)
            it.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.bg_default, null)))
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (!result.isSuccessful) {
            DisplayUtils.showSnackMessage(this, getString(R.string.image_editor_unable_to_edit_image))
            return
        }
        val resultUri = result.getUriFilePath(this, false)
        val newFileName = file.fileName.substring(0, file.fileName.lastIndexOf('.')) +
            " " + getString(R.string.image_editor_file_edited_suffix) +
            resultUri?.substring(resultUri.lastIndexOf('.'))

        resultUri?.let {
            FileUploadHelper().uploadNewFiles(
                user = storageManager.user,
                localPaths = arrayOf(it),
                remotePaths = arrayOf(file.parentRemotePath + File.separator + newFileName),
                createRemoteFolder = false,
                createdBy = UploadFileOperation.CREATED_BY_USER,
                requiresWifi = false,
                requiresCharging = false,
                nameCollisionPolicy = NameCollisionPolicy.RENAME,
                localBehavior = FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
            )
        }
    }

    override fun onSetImageUriComplete(view: CropImageView, uri: Uri, error: Exception?) {
        if (error != null) {
            DisplayUtils.showSnackMessage(this, getString(R.string.image_editor_unable_to_edit_image))
            return
        }
        view.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // add save button to action bar
        menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
        //No need to apply NC tint here as we will be doing it later in code
        val saveIcon = AppCompatResources.getDrawable(this, R.drawable.ic_tick)
        menu?.findItem(R.id.custom_menu_placeholder_item)?.apply {
            icon = saveIcon
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                contentDescription = getString(R.string.common_save)
            }
            //NMC customization
            icon = icon?.let {
                viewThemeUtils.platform.colorDrawable(
                    it,
                    ContextCompat.getColor(this@EditImageActivity, R.color.fontAppbar)
                )
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.custom_menu_placeholder_item -> {
            binding.cropImageView.croppedImageAsync(format)
            finish()
            true
        }

        else -> {
            finish()
            true
        }
    }

    /**
     * Set up image cropper and image editor control strip.
     */
    private fun setupCropper() {
        val cropper = binding.cropImageView

        @Suppress("MagicNumber")
        binding.rotateLeft.setOnClickListener {
            cropper.rotateImage(-90)
        }

        @Suppress("MagicNumber")
        binding.rotateRight.setOnClickListener {
            cropper.rotateImage(90)
        }

        binding.flipVertical.setOnClickListener {
            cropper.flipImageVertically()
        }

        binding.flipHorizontal.setOnClickListener {
            cropper.flipImageHorizontally()
        }

        cropper.setOnSetImageUriCompleteListener(this)
        cropper.setOnCropImageCompleteListener(this)
        cropper.setImageUriAsync(file.storageUri)

        // determine output file format
        format = when (file.mimeType) {
            MimeType.PNG -> Bitmap.CompressFormat.PNG
            MimeType.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }

            else -> Bitmap.CompressFormat.JPEG
        }
    }
}
