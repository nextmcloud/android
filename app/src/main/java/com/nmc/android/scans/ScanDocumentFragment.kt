package com.nmc.android.scans

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.nmc.android.interfaces.OnDocScanListener
import com.nmc.android.interfaces.OnFragmentChangeListener
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentScanDocumentBinding
import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.camera.CaptureInfo
import io.scanbot.sdk.camera.FrameHandlerResult
import io.scanbot.sdk.contourdetector.ContourDetectorFrameHandler
import io.scanbot.sdk.core.contourdetector.ContourDetector
import io.scanbot.sdk.core.contourdetector.DocumentDetectionStatus
import io.scanbot.sdk.docdetection.ui.IDocumentScannerViewCallback
import io.scanbot.sdk.ocr.OpticalCharacterRecognizer
import io.scanbot.sdk.process.ImageProcessor
import io.scanbot.sdk.ui.camera.CameraUiSettings
import io.scanbot.sdk.ui.view.base.configuration.CameraOrientationMode

class ScanDocumentFragment : Fragment() {

    private lateinit var scanbotSDK: ScanbotSDK
    private lateinit var contourDetector: ContourDetector

    private var lastUserGuidanceHintTs = 0L
    private var flashEnabled = false
    private var autoSnappingEnabled = true
    private val ignoreBadAspectRatio = true

    //OCR
    private lateinit var opticalCharacterRecognizer: OpticalCharacterRecognizer

    private lateinit var onDocScanListener: OnDocScanListener
    private lateinit var onFragmentChangeListener: OnFragmentChangeListener

    private lateinit var calledFrom: String

    private lateinit var binding: FragmentScanDocumentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_CALLED_FROM)?.let {
            calledFrom = it
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onDocScanListener = context as OnDocScanListener
            onFragmentChangeListener = context as OnFragmentChangeListener
        } catch (ignored: Exception) {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (requireActivity() is ScanActivity) {
            (requireActivity() as ScanActivity).showHideToolbar(false)
            (requireActivity() as ScanActivity).showHideDefaultToolbarDivider(false)
        }
        binding = FragmentScanDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        askPermission()
        initDependencies()

        binding.camera.apply {
            initCamera(CameraUiSettings(true))
            initDetectionBehavior(contourDetector,
                { result ->
                    // Here you are continuously notified about contour detection results.
                    // For example, you can show a user guidance text depending on the current detection status.
                    // don't update the text if fragment is removing
                    if (!isRemoving) {
                        // Here you are continuously notified about contour detection results.
                        // For example, you can show a user guidance text depending on the current detection status.
                        binding.userGuidanceHint.post {
                            if (result is FrameHandlerResult.Success<*>) {
                                showUserGuidance((result as FrameHandlerResult.Success<ContourDetectorFrameHandler.DetectedFrame>).value.detectionStatus)
                            }
                        }
                    }
                    false // typically you need to return false
                },
                object : IDocumentScannerViewCallback {
                    override fun onCameraOpen() {
                        // In this example we demonstrate how to lock the orientation of the UI (Activity)
                        // as well as the orientation of the taken picture to portrait.
                        binding.camera.cameraConfiguration.setCameraOrientationMode(CameraOrientationMode.PORTRAIT)

                        binding.camera.viewController.useFlash(flashEnabled)
                        binding.camera.viewController.continuousFocus()
                    }

                    override fun onPictureTaken(image: ByteArray, captureInfo: CaptureInfo) {
                        processPictureTaken(image, captureInfo.imageOrientation)

                        // continue scanning
                        /*binding.camera.postDelayed({
                            binding.camera.viewController.startPreview()
                        }, 1000)*/
                    }
                }
            )

            // See https://docs.scanbot.io/document-scanner-sdk/android/features/document-scanner/using-scanbot-camera-view/#preview-mode
            // cameraConfiguration.setCameraPreviewMode(io.scanbot.sdk.camera.CameraPreviewMode.FIT_IN)
        }

        binding.camera.viewController.apply {
            setAcceptedAngleScore(60.0)
            setAcceptedSizeScore(75.0)
            setIgnoreBadAspectRatio(ignoreBadAspectRatio)

            // Please note: https://docs.scanbot.io/document-scanner-sdk/android/features/document-scanner/autosnapping/#sensitivity
            setAutoSnappingSensitivity(0.85f)
        }

        binding.shutterButton.setOnClickListener { binding.camera.viewController.takePicture(false) }
        binding.shutterButton.visibility = View.VISIBLE

        binding.scanDocBtnFlash.setOnClickListener {
            flashEnabled = !flashEnabled
            binding.camera.viewController.useFlash(flashEnabled)
            toggleFlashButtonUI()
        }
        binding.scanDocBtnCancel.setOnClickListener {
            // if fragment opened from Edit Scan Fragment then on cancel click it should go to that fragment
            if (calledFrom == EditScannedDocumentFragment.TAG) {
                openEditScanFragment()
            } else {
                // else default behaviour
                (requireActivity() as ScanActivity).onBackPressed()
            }
        }

        binding.scanDocBtnAutomatic.setOnClickListener {
            autoSnappingEnabled = !autoSnappingEnabled
            setAutoSnapEnabled(autoSnappingEnabled)
        }
        binding.scanDocBtnAutomatic.post { setAutoSnapEnabled(autoSnappingEnabled) }

        toggleFlashButtonUI()
    }

    private fun toggleFlashButtonUI() {
        if (flashEnabled) {
            binding.scanDocBtnFlash.setIconTintResource(R.color.primary)
            binding.scanDocBtnFlash.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.primary,
                    requireContext().theme
                )
            )
        } else {
            binding.scanDocBtnFlash.setIconTintResource(R.color.grey_60)
            binding.scanDocBtnFlash.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.grey_60,
                    requireContext().theme
                )
            )
        }
    }

    private fun askPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                )
            )
        }
    }

    private fun initDependencies() {
        scanbotSDK = (requireActivity() as ScanActivity).scanbotSDK
        contourDetector = scanbotSDK.createContourDetector()
        opticalCharacterRecognizer = scanbotSDK.createOcrRecognizer()
    }

    override fun onResume() {
        super.onResume()
        binding.camera.viewController.onResume()
        binding.scanDocProgressBar.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        binding.camera.viewController.onPause()
    }

    private fun showUserGuidance(result: DocumentDetectionStatus) {
        if (!autoSnappingEnabled) {
            return
        }
        if (System.currentTimeMillis() - lastUserGuidanceHintTs < 400) {
            return
        }

        // Make sure to reset the default polygon fill color (see the ignoreBadAspectRatio case).
        // polygonView.setFillColor(POLYGON_FILL_COLOR)
        // fragment should be added and visible because this method is being called from handler
        // it can be called when fragment is not attached or visible
        if (isAdded && isVisible) {
            when (result) {
                DocumentDetectionStatus.OK -> {
                    binding.userGuidanceHint.text = resources.getString(R.string.result_scan_doc_dont_move)
                    binding.userGuidanceHint.visibility = View.VISIBLE
                }

                DocumentDetectionStatus.OK_BUT_TOO_SMALL -> {
                    binding.userGuidanceHint.text = resources.getString(R.string.result_scan_doc_move_closer)
                    binding.userGuidanceHint.visibility = View.VISIBLE
                }

                DocumentDetectionStatus.OK_BUT_BAD_ANGLES -> {
                    binding.userGuidanceHint.text = resources.getString(R.string.result_scan_doc_perspective)
                    binding.userGuidanceHint.visibility = View.VISIBLE
                }

                DocumentDetectionStatus.ERROR_NOTHING_DETECTED -> {
                    binding.userGuidanceHint.text = resources.getString(R.string.result_scan_doc_no_doc)
                    binding.userGuidanceHint.visibility = View.VISIBLE
                }

                DocumentDetectionStatus.ERROR_TOO_NOISY -> {
                    binding.userGuidanceHint.text = resources.getString(R.string.result_scan_doc_bg_noisy)
                    binding.userGuidanceHint.visibility = View.VISIBLE
                }

                DocumentDetectionStatus.OK_BUT_BAD_ASPECT_RATIO -> {
                    if (ignoreBadAspectRatio) {
                        binding.userGuidanceHint.text = resources.getString(R.string.result_scan_doc_dont_move)
                        // change polygon color to "OK"
                        // polygonView.setFillColor(POLYGON_FILL_COLOR_OK)
                    } else {
                        binding.userGuidanceHint.text = resources.getString(R.string.result_scan_doc_aspect_ratio)
                    }
                    binding.userGuidanceHint.visibility = View.VISIBLE
                }

                DocumentDetectionStatus.ERROR_TOO_DARK -> {
                    binding.userGuidanceHint.text = resources.getString(R.string.result_scan_doc_poor_light)
                    binding.userGuidanceHint.visibility = View.VISIBLE
                }

                else -> binding.userGuidanceHint.visibility = View.GONE
            }
        }
        lastUserGuidanceHintTs = System.currentTimeMillis()
    }

    private fun processPictureTaken(image: ByteArray, imageOrientation: Int) {
        requireActivity().runOnUiThread {
            binding.camera.viewController.onPause()
            binding.scanDocProgressBar.visibility = View.VISIBLE
            //cameraView.visibility = View.GONE
        }
        // Here we get the full image from the camera.
        // Please see https://github.com/doo/Scanbot-SDK-Examples/wiki/Handling-camera-picture
        // This is just a demo showing the detected document image as a downscaled(!) preview image.

        // Decode Bitmap from bytes of original image:
        val options = BitmapFactory.Options()
        // Please note: In this simple demo we downscale the original image to 1/8 for the preview!
        //options.inSampleSize = 8
        // Typically you will need the full resolution of the original image! So please change the "inSampleSize" value to 1!
        options.inSampleSize = 1
        var originalBitmap = BitmapFactory.decodeByteArray(image, 0, image.size, options)

        // Rotate the original image based on the imageOrientation value.
        // Required for some Android devices like Samsung!
        if (imageOrientation > 0) {
            val matrix = Matrix()
            matrix.setRotate(imageOrientation.toFloat(), originalBitmap.width / 2f, originalBitmap.height / 2f)
            originalBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                false
            )
        }

        // Run document detection on original image:
        val result = contourDetector.detect(originalBitmap)!!
        val detectedPolygon = result.polygonF

        val documentImage = ImageProcessor(originalBitmap).crop(detectedPolygon).processedBitmap()

        // val file = saveImage(documentImage)
        // Log.d("SCANNING","File : $file")
        if (documentImage != null) {
            onDocScanListener.addScannedDoc(documentImage)
            // onDocScanListener.addScannedDoc(FileUtils.saveImage(requireContext(), documentImage, null))
            openEditScanFragment()

            /*  uiScope.launch {
                  recognizeTextWithoutPDFTask(documentImage)
              }*/
        }
        // RecognizeTextWithoutPDFTask(documentImage).execute()

        //resultView.post { resultView.setImageBitmap(documentImage) }

        // continue scanning
        /*        cameraView.postDelayed({
                    cameraView.continuousFocus()
                    cameraView.startPreview()
                }, 1000)*/
    }

    private fun openEditScanFragment() {
        onFragmentChangeListener.onReplaceFragment(
            EditScannedDocumentFragment.newInstance(onDocScanListener.getScannedDocs().size - 1),
            ScanActivity.FRAGMENT_EDIT_SCAN_TAG, false
        )
    }

    private fun setAutoSnapEnabled(enabled: Boolean) {
        binding.camera.viewController.apply {
            autoSnappingEnabled = enabled
            isFrameProcessingEnabled = enabled
        }
        binding.polygonView.visibility = if (enabled) View.VISIBLE else View.GONE
        /*autoSnappingToggleButton.text = resources.getString(R.string.automatic) + " ${
            if (enabled) "ON" else
                "OFF"
        }"*/
        if (enabled) {
            binding.scanDocBtnAutomatic.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.primary,
                    requireContext().theme
                )
            )
            binding.shutterButton.showAutoButton()
        } else {
            binding.scanDocBtnAutomatic.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.grey_60,
                    requireContext().theme
                )
            )
            binding.shutterButton.showManualButton()
            binding.userGuidanceHint.visibility = View.GONE
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            if (!it.value) {
                // permission not granted
                val showRationale = shouldShowRequestPermissionRationale(it.key)
                if (!showRationale) {
                    // user also CHECKED "never ask again"
                    // you can either enable some fall back,
                    // disable features of your app
                    // or open another dialog explaining
                    // again the permission and directing to
                    // the app setting
                    onPermissionDenied(requireActivity().resources.getString(R.string.camera_permission_rationale))
                } else if (Manifest.permission.CAMERA == it.key) {
                    // user did NOT check "never ask again"
                    // this is a good place to explain the user
                    // why you need the permission and ask if he wants
                    // to accept it (the rationale)
                    onPermissionDenied(requireActivity().resources.getString(R.string.camera_permission_denied))

                    // askPermission()
                }
                // else if ( /* possibly check more permissions...*/) {
                // }
            }
        }
    }

    private fun onPermissionDenied(message: String) {
        // Show Toast instead of snackbar as we are finishing the activity
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
        requireActivity().finish()
    }

    companion object {

        @JvmStatic
        val ARG_CALLED_FROM = "arg_called_From"

        @JvmStatic
        fun newInstance(calledFrom: String): ScanDocumentFragment {
            val args = Bundle()
            args.putString(ARG_CALLED_FROM, calledFrom)
            val fragment = ScanDocumentFragment()
            fragment.arguments = args
            return fragment
        }
    }
}