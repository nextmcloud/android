package com.nmc.android.scans

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.HandlerCompat
import androidx.fragment.app.Fragment
import com.nmc.android.interfaces.OnDocScanListener
import com.nmc.android.utils.ScanBotSdkUtils.resizeForPreview
import com.owncloud.android.R
import com.owncloud.android.databinding.ItemScannedDocBinding
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.process.FilterOperation
import io.scanbot.sdk.process.ImageFilterType
import io.scanbot.sdk.process.RotateOperation
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanPagerFragment : Fragment() {
    private lateinit var binding: ItemScannedDocBinding

    private lateinit var scanbotSDK: ScanbotSDK
    private var originalBitmap: Bitmap? = null
    private var previewBitmap: Bitmap? = null

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

    private var lastRotationEventTs = 0L
    private var rotationDegrees = 0
    private var index = 0

    private var onDocScanListener: OnDocScanListener? = null
    private var applyFilterDialog: AlertDialog? = null
    private var selectedFilter = 0

    // scan should not be saved till filter is applied
    // flag to check if applying filter is in progress or not
    var isFilterApplyInProgress: Boolean = false
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onDocScanListener = context as OnDocScanListener
        } catch (ignored: Exception) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
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
        //File file = new File(scannedDocPath);
        //originalBitmap = FileUtils.convertFileToBitmap(file);
        // previewBitmap = ScanBotSdkUtils.resizeForPreview(originalBitmap);
        // loadImage();
        setUpBitmap()
    }

    private fun setUpBitmap() {
        executorService.execute {
            if (index >= 0 && index < ScanActivity.filteredImages.size) {
                originalBitmap = onDocScanListener?.getScannedDocs()?.get(index)
                originalBitmap?.let {
                    previewBitmap = resizeForPreview(it)
                }
            }
            if (index >= 0 && index < ScanActivity.scannedImagesFilterIndex.size) {
                selectedFilter = ScanActivity.scannedImagesFilterIndex[index]
            }
            handler.post { loadImage() }
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
        originalBitmap?.let{
            executorService.execute {
                val rotatedBitmap = scanbotSDK.imageProcessor().processBitmap(
                    it,
                    ArrayList(listOf(RotateOperation(rotationDegrees))), false
                )
                onDocScanListener?.replaceScannedDoc(index, rotatedBitmap, false)
            }
        }
    }

    fun showApplyFilterDialog() {
        val filterArray = resources.getStringArray(R.array.edit_scan_filter_values)
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.edit_scan_filter_dialog_title)
            .setSingleChoiceItems(
                filterArray,
                selectedFilter
            ) { dialog: DialogInterface, which: Int ->
                selectedFilter = which
                onDocScanListener?.replaceFilterIndex(index, selectedFilter)
                if (filterArray[which].equals(resources.getString(R.string.edit_scan_filter_none), ignoreCase = true)) {
                    applyFilter(ImageFilterType.NONE)
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_pure_binarized),
                        ignoreCase = true
                    )
                ) {
                    applyFilter(ImageFilterType.PURE_BINARIZED)
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_color_enhanced),
                        ignoreCase = true
                    )
                ) {
                    applyFilter(ImageFilterType.COLOR_ENHANCED, ImageFilterType.EDGE_HIGHLIGHT)
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_color_document),
                        ignoreCase = true
                    )
                ) {
                    applyFilter(ImageFilterType.COLOR_DOCUMENT)
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_grey),
                        ignoreCase = true
                    )
                ) {
                    applyFilter(ImageFilterType.GRAYSCALE)
                } else if (filterArray[which].equals(
                        resources.getString(R.string.edit_scan_filter_b_n_w),
                        ignoreCase = true
                    )
                ) {
                    applyFilter(ImageFilterType.BLACK_AND_WHITE)
                }
                dialog.dismiss()
            }
            .setOnCancelListener { }
        applyFilterDialog = builder.create()
        applyFilterDialog?.show()
    }

    private fun applyFilter(vararg imageFilterType: ImageFilterType) {
        binding.editScanImageProgressBar.visibility = View.VISIBLE
        isFilterApplyInProgress = true
        originalBitmap?.let {
            executorService.execute {
                if (imageFilterType[0] != ImageFilterType.NONE) {
                    val filterOperationList: MutableList<FilterOperation> = ArrayList()
                    for (filters in imageFilterType) {
                        filterOperationList.add(FilterOperation(filters))
                    }
                    previewBitmap =
                        scanbotSDK.imageProcessor().processBitmap(it, filterOperationList, false)
                } else {
                    previewBitmap = ScanActivity.originalScannedImages[index]
                }
                onDocScanListener?.replaceScannedDoc(index, previewBitmap, true)
                handler.post {
                    isFilterApplyInProgress = false
                    binding.editScanImageProgressBar.visibility = View.GONE
                    loadImage()
                }
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
}
