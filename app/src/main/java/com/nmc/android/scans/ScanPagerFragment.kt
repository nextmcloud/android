package com.nmc.android.scans

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nmc.android.interfaces.OnDocScanListener
import com.nmc.android.utils.ScanBotSdkUtils.resizeForPreview
import com.owncloud.android.R
import com.owncloud.android.databinding.ItemScannedDocBinding
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.docprocessing.Document
import io.scanbot.sdk.docprocessing.Page
import io.scanbot.sdk.imagefilters.ColorDocumentFilter
import io.scanbot.sdk.imagefilters.GrayscaleFilter
import io.scanbot.sdk.imagefilters.LegacyFilter
import io.scanbot.sdk.imagefilters.ParametricFilter
import io.scanbot.sdk.process.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanPagerFragment : Fragment() {
    private lateinit var binding: ItemScannedDocBinding

    private lateinit var scanbotSDK: ScanbotSDK

    private lateinit var document: Document
    private lateinit var page: Page

    private var originalBitmap: Bitmap? = null
    private var previewBitmap: Bitmap? = null

    private var lastRotationEventTs = 0L
    private var rotationDegrees = 0
    private var index = 0

    private var onDocScanListener: OnDocScanListener? = null
    private var applyFilterDialog: AlertDialog? = null
    private var selectedFilterIndex = 0
    var filteringState: FilteringState = FilteringState.IDLE
    private var selectedFilter: List<ParametricFilter?>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onDocScanListener = context as OnDocScanListener
        } catch (ignored: Exception) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            index = it.getInt(ARG_SCANNED_DOC_PATH)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (requireActivity() is ScanActivity) {
            scanbotSDK = (requireActivity() as ScanActivity).scanbotSDK
        }
        binding = ItemScannedDocBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch { loadDocument() }
    }

    private suspend fun loadDocument() {
        val doc = withContext(Dispatchers.IO) { scanbotSDK.documentApi.createDocument() }
        withContext(Dispatchers.Main) {
            document = doc
            
            if (index >= 0 && index < ScanActivity.filteredImages.size) {
                originalBitmap = onDocScanListener?.getScannedDocs()?.get(index)
                originalBitmap?.let {
                    previewBitmap = resizeForPreview(it)
                    val page = document.addPage(it)
                    this@ScanPagerFragment.page = page

                    val appliedFilter = page.filters.getOrNull(index)
                    selectedFilter = listOf(appliedFilter)

                }
            }
            if (index >= 0 && index < ScanActivity.scannedImagesFilterIndex.size) {
                selectedFilterIndex = ScanActivity.scannedImagesFilterIndex[index]
            }

            loadImage()
        }
    }

    private fun loadImage() {
        if (this::binding.isInitialized) {
            if (previewBitmap != null) {
                binding.editScannedImageView.setImageBitmap(previewBitmap)
            } else if (originalBitmap != null) {
                binding.editScannedImageView.setImageBitmap(originalBitmap)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        applyFilterDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    fun rotate() {
        if (System.currentTimeMillis() - lastRotationEventTs < 350) {
            return
        }
        rotationDegrees += 90
        binding.editScannedImageView.rotateClockwise()
        lastRotationEventTs = System.currentTimeMillis()
        originalBitmap?.let {
            lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    val rotatedBitmap = ImageProcessor(it).rotate(rotationDegrees).processedBitmap()
                    onDocScanListener?.replaceScannedDoc(index, rotatedBitmap, false)
                }
            }
        }
    }

    fun showApplyFilterDialog() {
        val filterArray = resources.getStringArray(R.array.edit_scan_filter_values)
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.edit_scan_filter_dialog_title)
            .setSingleChoiceItems(
                filterArray,
                selectedFilterIndex
            ) { dialog: DialogInterface, which: Int ->
                selectedFilterIndex = which
                onDocScanListener?.replaceFilterIndex(index, selectedFilterIndex)
                if (filterArray[which].equals(resources.getString(R.string.edit_scan_filter_none), ignoreCase = true)) {
                    applyFilter(null)
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_pure_binarized),
                        ignoreCase = true
                    )
                ) {
                    // PURE_BINARIZED filter type in int
                    applyFilter(LegacyFilter(11))
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_color_enhanced),
                        ignoreCase = true
                    )
                ) {
                    // COLOR_ENHANCED & EDGE_HIGHLIGHT filter type in int
                    applyFilter(LegacyFilter(1), LegacyFilter(17))
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_color_document),
                        ignoreCase = true
                    )
                ) {
                    applyFilter(ColorDocumentFilter())
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_grey),
                        ignoreCase = true
                    )
                ) {
                    applyFilter(GrayscaleFilter())
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_b_n_w),
                        ignoreCase = true
                    )
                ) {
                    // BLACK_AND_WHITE filter type in int
                    applyFilter(LegacyFilter(14))
                }
                dialog.dismiss()
            }
            .setOnCancelListener { }
        applyFilterDialog = builder.create()
        applyFilterDialog?.show()
    }

    private fun applyFilter(vararg imageFilterType: ParametricFilter?) {
        if (selectedFilter == imageFilterType.toList()) {
            return
        }

        binding.editScanImageProgressBar.visibility = View.VISIBLE
        selectedFilter = imageFilterType.toList()
        originalBitmap?.let {
            if (filteringState == FilteringState.IDLE) {
                lifecycleScope.launch {
                    filteringState = FilteringState.PROCESSING

                    withContext(Dispatchers.Default) {
                        // applying empty collection of filters will remove all filters
                        val filtersToApply = selectedFilter?.filterNotNull()
                        page.apply(newFilters = filtersToApply)
                        previewBitmap = page.documentPreviewImage
                    }
                    onDocScanListener?.replaceScannedDoc(index, previewBitmap, true)

                    withContext(Dispatchers.Main) {
                        loadImage()
                        binding.editScanImageProgressBar.visibility = View.GONE
                        filteringState = FilteringState.IDLE
                    }
                }
            } else {
                // already in progress
            }
        }
    }

    companion object {
        private const val ARG_SCANNED_DOC_PATH = "scanned_doc_path"

        fun newInstance(i: Int): ScanPagerFragment {
            val args = Bundle()
            args.putInt(ARG_SCANNED_DOC_PATH, i)

            val fragment = ScanPagerFragment()
            fragment.arguments = args
            return fragment
        }
    }

    enum class FilteringState {
        IDLE,
        PROCESSING,
    }
}
