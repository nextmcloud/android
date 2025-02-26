/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.AlbumImageThumbnailBinding
import com.owncloud.android.databinding.AlbumSharingBottomSheetBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.operations.albums.PhotoAlbumEntry
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.overlay.OverlayManager
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AlbumSharingBottomSheet(
    private val photoAlbumEntry: PhotoAlbumEntry?,
    private val fileList: List<OCFile>?,
    private val actions: AlbumSharingBottomSheetActions
) : BottomSheetDialogFragment(), Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var currentUserProvider: CurrentAccountProvider

    @Inject
    lateinit var storageManager: FileDataStorageManager

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var overlayManager: OverlayManager

    private val thumbnailAsyncTasks = mutableListOf<ThumbnailsCacheManager.ThumbnailGenerationTask>()

    private var _binding: AlbumSharingBottomSheetBinding? = null
    val binding
        get() = _binding!!

    private var shareId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoAlbumEntry?.let {
            // read only the 1st item of result and 1st item of collaborators
            // as there will be no more data apart from current Album
            if (it.collaborators.isNotEmpty()) {
                shareId = it.collaborators[0].id
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AlbumSharingBottomSheetBinding.inflate(inflater, container, false)

        val bottomSheetDialog = dialog as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bottomSheetLoading.visibility = View.GONE
        setUpContent()
        setUpShareComponentsVisibility()
        setClickListeners()
    }

    private fun setUpContent() {
        photoAlbumEntry?.let {
            binding.apply {
                albumTitle.text = it.albumName
                albumImageLayout.thumbnail.tag = it.lastPhoto
                albumElements.text = String.format(
                    requireContext().resources.getString(R.string.album_elements_text),
                    it.nbItems,
                )
                albumDate.text = it.createdDate

                if (it.lastPhoto > 0) {
                    var ocLocal = storageManager.getFileByLocalId(it.lastPhoto)
                    if (ocLocal == null) {
                        // if local file is not present make dummy file with fake remotePath
                        // without remotePath it won't work
                        // lastPhoto is file id which we can set it to localId and remoteId for thumbnail generation
                        val nFile = OCFile("/" + it.albumName)
                        nFile.localId = it.lastPhoto
                        nFile.remoteId = it.lastPhoto.toString()
                        ocLocal = nFile
                    }
                    DisplayUtils.setThumbnail(
                        ocLocal,
                        albumImageLayout.thumbnail,
                        currentUserProvider.user,
                        storageManager,
                        thumbnailAsyncTasks,
                        false,
                        context,
                        albumImageLayout.thumbnailShimmer,
                        syncedFolderProvider.preferences,
                        viewThemeUtils,
                        overlayManager,
                        true,
                        true
                    )
                } else {
                    albumImageLayout.thumbnail.setImageResource(R.drawable.album_no_photo_placeholder)
                    albumImageLayout.thumbnail.visibility = View.VISIBLE
                    albumImageLayout.thumbnailShimmer.visibility = View.GONE
                }

                initializeImageCollage()
            }
        }
    }

    private fun initializeImageCollage() {
        fileList?.let {
            if (it.isNotEmpty()) {
                binding.imageCollage.visibility = View.VISIBLE

                val imageViews = listOf(
                    binding.imgTopLeft,
                    binding.imgBottomLeft,
                    binding.imgCenter,
                    binding.imgTopRight,
                    binding.imgBottomRight
                )

                imageViews.forEach { image -> image.root.visibility = View.GONE }

                rearrangeImageCollage(imageViews, it.size)

                it.forEachIndexed { index, url ->
                    imageViews[index].root.visibility = View.VISIBLE
                    DisplayUtils.setThumbnail(
                        url,
                        imageViews[index].thumbnail,
                        currentUserProvider.user,
                        storageManager,
                        thumbnailAsyncTasks,
                        false,
                        context,
                        imageViews[index].thumbnailShimmer,
                        syncedFolderProvider.preferences,
                        viewThemeUtils,
                        overlayManager,
                        true,
                        true
                    )
                }
            }
        }
    }

    /**
     * rearrange the collage images based on the number of images to be shown
     * for IMAGE_COLLAGE_MAX_LIMIT which is 5 images the default xml layout will be used
     */
    private fun rearrangeImageCollage(imageViews: List<AlbumImageThumbnailBinding>, count: Int) {
        if (count == IMAGE_COLLAGE_MAX_LIMIT) {
            return
        }

        val set = ConstraintSet()
        set.clone(binding.imageCollage)

        imageViews.forEach {
            set.clear(it.root.id)
            it.root.visibility = View.GONE
        }

        when (count) {
            1 -> layout1Image(set)
            2 -> layout2Images(set)
            3 -> layout3Images(set)
            4 -> layout4Images(set)
        }

        set.applyTo(binding.imageCollage)
    }

    /**
     * show single image with full width and height
     */
    private fun layout1Image(set: ConstraintSet) {
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    }

    /**
     * for 2 images place both images equal width and full height
     */
    private fun layout2Images(set: ConstraintSet) {
        // for horizontal spacing between images we have used 0.48f
        listOf(binding.imgTopLeft.root, binding.imgBottomLeft.root)
            .forEach {
                set.constrainPercentWidth(it.id, 0.48f)
            }

        set.connect(binding.imgTopLeft.root.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.END, binding.imgBottomLeft.root.id, ConstraintSet.START)

        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.START, binding.imgTopLeft.root.id, ConstraintSet.END)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    }

    /**
     * for 3 images place first 2 images in 1 column and 3rd image in 2nd column with full height
     */
    private fun layout3Images(set: ConstraintSet) {

        // for horizontal spacing between images we have used 0.48f
        listOf(binding.imgTopLeft.root, binding.imgBottomLeft.root, binding.imgCenter.root)
            .forEach {
                set.constrainPercentWidth(it.id, 0.48f)
            }

        // for vertical spacing between images we have used 0.48f
        listOf(binding.imgTopLeft.root, binding.imgBottomLeft.root)
            .forEach {
                set.constrainPercentHeight(it.id, 0.48f)
            }

        // 0.98f is used to align the full height image with 1st column image
        set.constrainPercentHeight(binding.imgCenter.root.id, 0.98f)

        // Left column
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.END, binding.imgCenter.root.id, ConstraintSet.START)

        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.TOP, binding.imgTopLeft.root.id, ConstraintSet.BOTTOM)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.END, binding.imgCenter.root.id, ConstraintSet.START)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        // Right column full
        set.connect(binding.imgCenter.root.id, ConstraintSet.START, binding.imgTopLeft.root.id, ConstraintSet.END)
        set.connect(binding.imgCenter.root.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        set.connect(binding.imgCenter.root.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.connect(binding.imgCenter.root.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    }

    /**
     * for 4 images place the images in 2*2 grid with different height to look them staggered
     */
    private fun layout4Images(set: ConstraintSet) {

        // for horizontal spacing between images we have used 0.48f
        listOf(binding.imgTopLeft.root, binding.imgBottomLeft.root, binding.imgCenter.root, binding.imgTopRight.root)
            .forEach {
                set.constrainPercentWidth(it.id, 0.48f)
            }

        // for vertical spacing between images we have used 0.52f
        listOf(binding.imgTopLeft.root, binding.imgTopRight.root)
            .forEach {
                set.constrainPercentHeight(it.id, 0.52f)
            }

        // for vertical spacing between images we have used 0.43f
        listOf(binding.imgBottomLeft.root, binding.imgCenter.root)
            .forEach {
                set.constrainPercentHeight(it.id, 0.43f)
            }

        // Top row
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(binding.imgTopLeft.root.id, ConstraintSet.END, binding.imgCenter.root.id, ConstraintSet.START)

        set.connect(binding.imgCenter.root.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        set.connect(binding.imgCenter.root.id, ConstraintSet.START, binding.imgTopLeft.root.id, ConstraintSet.END)
        set.connect(binding.imgCenter.root.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // Bottom row
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.TOP, binding.imgTopLeft.root.id, ConstraintSet.BOTTOM)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.END, binding.imgTopRight.root.id, ConstraintSet.START)
        set.connect(binding.imgBottomLeft.root.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        set.connect(binding.imgTopRight.root.id, ConstraintSet.TOP, binding.imgCenter.root.id, ConstraintSet.BOTTOM)
        set.connect(binding.imgTopRight.root.id, ConstraintSet.START, binding.imgBottomLeft.root.id, ConstraintSet.END)
        set.connect(binding.imgTopRight.root.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        set.connect(binding.imgTopRight.root.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
    }

    private fun setUpShareComponentsVisibility() {
        binding.createShareGroup.setVisibleIf(shareId.isNullOrEmpty())
        binding.shareGroup.setVisibleIf(!shareId.isNullOrEmpty())
    }

    private fun setClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        binding.btnCreateLink.setOnClickListener {
            actions.createShare()
        }
        binding.lblCreateLink.setOnClickListener {
            actions.createShare()
        }

        binding.btnStopSharing.setOnClickListener {
            actions.removeShare()
        }
        binding.lblStopSharing.setOnClickListener {
            actions.removeShare()
        }

        binding.btnCopy.setOnClickListener {
            actions.copyShareLink()
            dismiss()
        }
        binding.lblCopy.setOnClickListener {
            actions.copyShareLink()
            dismiss()
        }

        binding.btnShareAlbumLink.setOnClickListener {
            actions.shareAlbumLink()
            dismiss()
        }
        binding.lblShareAlbumLink.setOnClickListener {
            actions.shareAlbumLink()
            dismiss()
        }
    }

    // has to be called when the new share is created or removed
    fun updateShareId(updatedShareId: String?) {
        shareId = updatedShareId
        setUpShareComponentsVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val IMAGE_COLLAGE_MAX_LIMIT = 5

        @JvmStatic
        fun newInstance(
            photoAlbumEntry: PhotoAlbumEntry?,
            fileList: List<OCFile>?,
            actions: AlbumSharingBottomSheetActions
        ): AlbumSharingBottomSheet {
            return AlbumSharingBottomSheet(photoAlbumEntry, fileList, actions)
        }
    }
}
