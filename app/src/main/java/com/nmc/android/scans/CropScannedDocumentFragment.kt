package com.nmc.android.scans

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.nmc.android.interfaces.OnDocScanListener
import com.nmc.android.interfaces.OnFragmentChangeListener
import com.nmc.android.utils.ScanBotSdkUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentCropScanBinding
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.core.contourdetector.ContourDetector
import io.scanbot.sdk.core.contourdetector.DetectionStatus
import io.scanbot.sdk.core.contourdetector.Line2D
import io.scanbot.sdk.process.CropOperation
import io.scanbot.sdk.process.ImageProcessor
import java.util.concurrent.Executors
import kotlin.math.absoluteValue

class CropScannedDocumentFragment : Fragment() {
    private lateinit var binding: FragmentCropScanBinding
    private lateinit var onFragmentChangeListener: OnFragmentChangeListener
    private lateinit var onDocScanListener: OnDocScanListener

    private lateinit var scanbotSDK: ScanbotSDK
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var contourDetector: ContourDetector

    private var scannedDocIndex: Int = -1
    private lateinit var originalBitmap: Bitmap
    private var rotationDegrees = 0
    private var polygonPoints: List<PointF>? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getInt(ARG_SCANNED_DOC_INDEX)?.let {
            scannedDocIndex = it
        }
        // Fragment locked in portrait screen orientation
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        run {
            try {
                onFragmentChangeListener = context as OnFragmentChangeListener
                onDocScanListener = context as OnDocScanListener
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (requireActivity() is ScanActivity) {
            (requireActivity() as ScanActivity).showHideToolbar(true)
            (requireActivity() as ScanActivity).showHideDefaultToolbarDivider(true)
            (requireActivity() as ScanActivity).updateActionBarTitleAndHomeButtonByString(resources.getString(R.string.title_crop_scan))
        }
        binding = FragmentCropScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scanbotSDK = (requireActivity() as ScanActivity).scanbotSDK
        contourDetector = scanbotSDK.createContourDetector()
        imageProcessor = scanbotSDK.imageProcessor()

        detectDocument()
        binding.cropBtnResetBorders.setOnClickListener {
            onClickListener(it)
        }
        addExtraMarginForSwipeGesture()
        addMenuHost()
    }

    /**
     * method to add extra margins for gestured devices
     * where user has to swipe left or right to go back from current screen
     * this swipe gestures create issue with existing crop gestures
     * to avoid that we have added extra margins on left and right for devices
     * greater than API level 9+ (Pie)
     */
    private fun addExtraMarginForSwipeGesture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (binding.cropPolygonView.layoutParams is ViewGroup.MarginLayoutParams) {
                (binding.cropPolygonView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                    resources.getDimensionPixelOffset(R.dimen.standard_margin), 0,
                    resources.getDimensionPixelOffset(R.dimen.standard_margin), 0
                )
                binding.cropPolygonView.requestLayout()
            }
        }
    }

    private fun onCropDragListener() {
        polygonPoints?.let { points ->
            var previous = points
            binding.cropPolygonView.setEditPolygonDragStateListener { dragging ->
                if (dragging) {
                    previous = ArrayList(binding.cropPolygonView.polygon.map { PointF(it.x, it.y) })
                } else {
                    if (!isBigEnough(binding.cropPolygonView.polygon)) {
                        binding.cropPolygonView.polygon = previous
                    }
                }
            }
        }
    }

    private fun onClickListener(view: View) {
        when (view.id) {
            R.id.crop_btn_reset_borders -> {
                if (binding.cropBtnResetBorders.tag.equals(resources.getString(R.string.crop_btn_reset_crop_text))) {
                    updateButtonText(resources.getString(R.string.crop_btn_detect_doc_text))
                    resetCrop()
                } else if (binding.cropBtnResetBorders.tag.equals(resources.getString(R.string.crop_btn_detect_doc_text))) {
                    updateButtonText(resources.getString(R.string.crop_btn_reset_crop_text))
                    detectDocument()
                }
            }
        }
    }

    private fun updateButtonText(label: String) {
        binding.cropBtnResetBorders.tag = label
        binding.cropBtnResetBorders.text = label
    }

    private fun resetCrop() {
        polygonPoints = getResetPolygons()
        binding.cropPolygonView.polygon = getResetPolygons()
        onCropDragListener()
    }

    private fun getResetPolygons(): List<PointF> {
        val polygonList = mutableListOf<PointF>()
        val pointF = PointF(0.0f, 0.0f)
        val pointF1 = PointF(1.0f, 0.0f)
        val pointF2 = PointF(1.0f, 1.0f)
        val pointF3 = PointF(0.0f, 1.0f)
        polygonList.add(pointF)
        polygonList.add(pointF1)
        polygonList.add(pointF2)
        polygonList.add(pointF3)
        return polygonList
    }

    private fun detectDocument() {
        InitImageViewTask().executeOnExecutor(Executors.newSingleThreadExecutor())
    }

    @SuppressLint("StaticFieldLeak")
    internal inner class InitImageViewTask : AsyncTask<Void?, Void?, InitImageResult>() {
        private var previewBitmap: Bitmap? = null

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): InitImageResult {
            originalBitmap = onDocScanListener.getScannedDocs()[scannedDocIndex]
            previewBitmap = ScanBotSdkUtils.resizeForPreview(originalBitmap)

            val result = contourDetector.detect(originalBitmap)
            return when (result?.status) {
                DetectionStatus.OK,
                DetectionStatus.OK_BUT_BAD_ANGLES,
                DetectionStatus.OK_BUT_TOO_SMALL,
                DetectionStatus.OK_BUT_BAD_ASPECT_RATIO -> {
                    val linesPair = Pair(result.horizontalLines, result.verticalLines)
                    val polygon = result.polygonF

                    InitImageResult(linesPair, polygon)
                }

                else -> InitImageResult(Pair(listOf(), listOf()), listOf())
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(initImageResult: InitImageResult) {
            binding.cropPolygonView.setImageBitmap(previewBitmap)
            binding.magnifier.setupMagnifier(binding.cropPolygonView)

            // set detected polygon and lines into binding.cropPolygonView
            polygonPoints = initImageResult.polygon
            binding.cropPolygonView.polygon = initImageResult.polygon
            binding.cropPolygonView.setLines(initImageResult.linesPair.first, initImageResult.linesPair.second)

            if (initImageResult.polygon.isEmpty()) {
                resetCrop()
            } else {
                onCropDragListener()
            }
        }
    }

    internal inner class InitImageResult(val linesPair: Pair<List<Line2D>, List<Line2D>>, val polygon: List<PointF>)

    private fun crop() {
        // crop & warp image by selected polygon (editPolygonView.getPolygon())
        val operations = listOf(CropOperation(binding.cropPolygonView.polygon))

        var documentImage = imageProcessor.processBitmap(originalBitmap, operations, false)
        documentImage?.let {
            if (rotationDegrees > 0) {
                // rotate the final cropped image result based on current rotation value:
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                documentImage = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            }
            onDocScanListener.replaceScannedDoc(scannedDocIndex, documentImage, false)

            onFragmentChangeListener.onReplaceFragment(
                EditScannedDocumentFragment.newInstance(scannedDocIndex), ScanActivity.FRAGMENT_EDIT_SCAN_TAG, false
            )
        }
    }

    private fun addMenuHost() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.edit_scan, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_save -> {
                            crop()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    fun getScannedDocIndex(): Int {
        return scannedDocIndex
    }

    private fun isBigEnough(polygon: List<PointF>): Boolean {
        if (polygon.isEmpty()) {
            return true
        }

        /*
           We receive the array of 4 Polygons when user start dragging the borders to crop the document
            1. polygon[0].x to polygon[3].x  --> When user drag from left to right or right to left
            2. polygon[0].y to polygon[3].y  --> When user drag from top to bottom or bottom to top

            Now to find the minimum difference we need to compare X and Y polygons. Here we have 2 cases:
            1. For Y polygon:
               1.1. When user dragging from Top to Bottom --> In this case Y will have same value in 0 & 1 index
                    i.e. polygon[0].y & polygon[1].y

               1.2. When user dragging from Bottom to Top --> In this case Y will have same value in 2 & 3 index
                    i.e. polygon[2].y & polygon[3].y

             2. For X polygon:
               2.1. When user dragging from Left to Right --> In this case X will have same value in 0 & 3 index
                    i.e. polygon[0].x & polygon[3].x

               2.2. When user dragging from Right to Left --> In this case X will have same value in 1 & 2 index
                    i.e. polygon[1].x & polygon[2].x


            Now to avoid user cropping the whole document we need to have minimum cropping point. To do that
            we need to check the difference between the polygon for X and Y like:
            1. For Y: check the difference between polygon[0].y - polygon[2].y and so on
            2. For X: check the difference between polygon[0].x - polygon[1].x and so on

         */

        if ((polygon[0].y - polygon[2].y).absoluteValue < MINIMUM_CROP_REQUIRED) {
            return false
        }

        if ((polygon[0].y - polygon[3].y).absoluteValue < MINIMUM_CROP_REQUIRED) {
            return false
        }

        if ((polygon[1].y - polygon[2].y).absoluteValue < MINIMUM_CROP_REQUIRED) {
            return false
        }

        if ((polygon[1].y - polygon[3].y).absoluteValue < MINIMUM_CROP_REQUIRED) {
            return false
        }

        if ((polygon[0].x - polygon[1].x).absoluteValue < MINIMUM_CROP_REQUIRED) {
            return false
        }

        if ((polygon[0].x - polygon[2].x).absoluteValue < MINIMUM_CROP_REQUIRED) {
            return false
        }

        if ((polygon[3].x - polygon[1].x).absoluteValue < MINIMUM_CROP_REQUIRED) {
            return false
        }

        if ((polygon[3].x - polygon[2].x).absoluteValue < MINIMUM_CROP_REQUIRED) {
            return false
        }

        return true
    }

    companion object {
        private const val ARG_SCANNED_DOC_INDEX = "scanned_doc_index"

        //variable used to avoid cropping the whole document
        private const val MINIMUM_CROP_REQUIRED = 0.1

        @JvmStatic
        fun newInstance(index: Int): CropScannedDocumentFragment {
            val args = Bundle()
            args.putInt(ARG_SCANNED_DOC_INDEX, index)
            val fragment = CropScannedDocumentFragment()
            fragment.arguments = args
            return fragment
        }
    }
}