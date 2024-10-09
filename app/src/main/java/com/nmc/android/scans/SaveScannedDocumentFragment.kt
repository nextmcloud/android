package com.nmc.android.scans

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.BadTokenException
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nmc.android.utils.CheckableThemeUtils.tintCheckbox
import com.nmc.android.utils.CheckableThemeUtils.tintSwitch
import com.nmc.android.utils.FileUtils
import com.nmc.android.utils.KeyboardUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentScanSaveBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.utils.DisplayUtils
import javax.inject.Inject

class SaveScannedDocumentFragment : Fragment(), CompoundButton.OnCheckedChangeListener, Injectable,
    View.OnClickListener {

    private lateinit var binding: FragmentScanSaveBinding

    private var isFileNameEditable = false
    private var remotePath: String = "/"
    private var remoteFilePath: OCFile? = null

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fragment screen orientation normal both portrait and landscape
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (requireActivity() is ScanActivity) {
            (requireActivity() as ScanActivity).showHideToolbar(true)
            (requireActivity() as ScanActivity).showHideDefaultToolbarDivider(true)
            (requireActivity() as ScanActivity).updateActionBarTitleAndHomeButtonByString(
                resources.getString(R.string.title_save_as)
            )
        }
        binding = FragmentScanSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        prepareRemotePath()
        implementCheckListeners()
        implementClickEvent()
    }

    private fun implementClickEvent() {
        binding.scanSaveFilenameInputEditBtn.setOnClickListener(this)
        binding.scanSaveLocationEditBtn.setOnClickListener(this)
        binding.saveScanBtnCancel.setOnClickListener(this)
        binding.saveScanBtnSave.setOnClickListener(this)
    }

    /**
     * prepare remote path to save scanned files
     */
    private fun prepareRemotePath() {
        //check if user has selected scan document from sub folders
        //if yes then show that folder in location to save scanned documents
        //else check in preferences for last selected path
        //if no last path selected available then show default /Scans/ path

        if (requireActivity() is ScanActivity) {
            val remotePath = (requireActivity() as ScanActivity).remotePath
            //remote path should not be null and should not be root path i.e only /
            if (remotePath != null && remotePath != OCFile.ROOT_PATH) {
                setRemoteFilePath(remotePath)
                return
            }

            val lastRemotePath = appPreferences.uploadScansLastPath
            //if user coming from Root path and the last saved path is not Scans folder
            //then show the Root as scan doc path
            if (remotePath == OCFile.ROOT_PATH && lastRemotePath != ScanActivity.DEFAULT_UPLOAD_SCAN_PATH) {
                setRemoteFilePath(remotePath)
                return
            }
        }

        setRemoteFilePath(appPreferences.uploadScansLastPath)
    }

    fun setRemoteFilePath(remotePath: String) {
        remoteFilePath = OCFile(remotePath)
        remoteFilePath?.setFolder()

        updateSaveLocationText(remotePath)
    }

    private fun initViews() {
        binding.scanSaveFilenameInput.setText(FileUtils.scannedFileName())
        tintSwitch(binding.scanSavePdfPasswordSwitch)
        tintCheckbox(
            binding.scanSaveWithoutTxtRecognitionPdfCheckbox,
            binding.scanSaveWithoutTxtRecognitionPngCheckbox,
            binding.scanSaveWithoutTxtRecognitionJpgCheckbox,
            binding.scanSaveWithTxtRecognitionPdfCheckbox,
            binding.scanSaveWithTxtRecognitionTxtCheckbox
        )
        binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked = true
        binding.scanSavePdfPasswordTextInput.defaultHintTextColor = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_focused),
            ),
            intArrayOf(
                Color.GRAY,
                resources.getColor(R.color.text_color, null)
            )
        )
    }

    private fun implementCheckListeners() {
        binding.scanSaveWithoutTxtRecognitionPdfCheckbox.setOnCheckedChangeListener(this)
        binding.scanSaveWithoutTxtRecognitionJpgCheckbox.setOnCheckedChangeListener(this)
        binding.scanSaveWithoutTxtRecognitionPngCheckbox.setOnCheckedChangeListener(this)
        binding.scanSaveWithTxtRecognitionPdfCheckbox.setOnCheckedChangeListener(this)
        binding.scanSaveWithTxtRecognitionTxtCheckbox.setOnCheckedChangeListener(this)
        binding.scanSavePdfPasswordSwitch.setOnCheckedChangeListener(this)

        binding.scanSaveFilenameInput.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                enableFileNameEditing()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun enableDisablePdfPasswordSwitch() {
        binding.scanSavePdfPasswordSwitch.isEnabled =
            binding.scanSaveWithoutTxtRecognitionPdfCheckbox.isChecked || binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked
        if (!binding.scanSaveWithoutTxtRecognitionPdfCheckbox.isChecked && !binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked) {
            binding.scanSavePdfPasswordSwitch.isChecked = false
        }
    }

    private fun showHidePdfPasswordInput(isChecked: Boolean) {
        binding.scanSavePdfPasswordTextInput.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (isChecked) {
            binding.scanSaveNestedScrollView.post { binding.scanSaveNestedScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
        if (isChecked) {
            KeyboardUtils.showSoftKeyboard(requireContext(), binding.scanSavePdfPasswordEt)
        } else {
            KeyboardUtils.hideKeyboardFrom(requireContext(), binding.scanSavePdfPasswordEt)
        }
    }

    private fun enableFileNameEditing() {
        isFileNameEditable = !isFileNameEditable
        binding.scanSaveFilenameInput.isEnabled = isFileNameEditable
        if (isFileNameEditable) {
            binding.scanSaveFilenameInputEditBtn.setImageResource(R.drawable.ic_tick)
            KeyboardUtils.showSoftKeyboard(requireContext(), binding.scanSaveFilenameInput)
            binding.scanSaveFilenameInput.setSelection(
                binding.scanSaveFilenameInput.text.toString().trim { it <= ' ' }.length
            )
        } else {
            binding.scanSaveFilenameInputEditBtn.setImageResource(R.drawable.ic_pencil_edit)
            KeyboardUtils.hideKeyboardFrom(requireContext(), binding.scanSaveFilenameInput)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.scan_save_filename_input_edit_btn -> enableFileNameEditing()
            R.id.scan_save_location_edit_btn -> {
                val action = Intent(requireActivity(), FolderPickerActivity::class.java)
                action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
                action.putExtra(FolderPickerActivity.EXTRA_SHOW_ONLY_FOLDER, true)
                action.putExtra(FolderPickerActivity.EXTRA_HIDE_ENCRYPTED_FOLDER, false)
                scanDocSavePathResultLauncher.launch(action)
            }

            R.id.save_scan_btn_cancel -> requireActivity().onBackPressedDispatcher.onBackPressed()
            R.id.save_scan_btn_save -> saveScannedFiles()
        }
    }

    private fun saveScannedFiles() {
        val fileName = binding.scanSaveFilenameInput.text.toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(fileName)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_empty)
            return
        }

        if (!com.owncloud.android.lib.resources.files.FileUtils.isValidName(fileName)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_forbidden_charaters_from_server)
            return
        }

        if (!binding.scanSaveWithoutTxtRecognitionPdfCheckbox.isChecked
            && !binding.scanSaveWithoutTxtRecognitionJpgCheckbox.isChecked
            && !binding.scanSaveWithoutTxtRecognitionPngCheckbox.isChecked
            && !binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked
            && !binding.scanSaveWithTxtRecognitionTxtCheckbox.isChecked
        ) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.scan_save_no_file_select_toast)
            return
        }

        val fileTypesStringBuilder = StringBuilder()
        if (binding.scanSaveWithoutTxtRecognitionPdfCheckbox.isChecked) {
            fileTypesStringBuilder.append(SAVE_TYPE_PDF)
            fileTypesStringBuilder.append(",")
        }
        if (binding.scanSaveWithoutTxtRecognitionJpgCheckbox.isChecked) {
            fileTypesStringBuilder.append(SAVE_TYPE_JPG)
            fileTypesStringBuilder.append(",")
        }
        if (binding.scanSaveWithoutTxtRecognitionPngCheckbox.isChecked) {
            fileTypesStringBuilder.append(SAVE_TYPE_PNG)
            fileTypesStringBuilder.append(",")
        }
        if (binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked) {
            fileTypesStringBuilder.append(SAVE_TYPE_PDF_OCR)
            fileTypesStringBuilder.append(",")
        }
        if (binding.scanSaveWithTxtRecognitionTxtCheckbox.isChecked) {
            fileTypesStringBuilder.append(SAVE_TYPE_TXT)
        }
        val pdfPassword = binding.scanSavePdfPasswordEt.text.toString().trim { it <= ' ' }
        if (binding.scanSavePdfPasswordSwitch.isChecked && TextUtils.isEmpty(pdfPassword)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.save_scan_empty_pdf_password)
            return
        }

        showPromptToSave(fileName, fileTypesStringBuilder, pdfPassword)
    }

    private fun showPromptToSave(fileName: String, fileTypesStringBuilder: StringBuilder, pdfPassword: String) {
        try {
            val alertDialog = AlertDialog.Builder(requireContext())
                .setMessage(R.string.dialog_save_scan_message)
                .setPositiveButton(R.string.dialog_ok) { _: DialogInterface?, _: Int ->
                    startSaving(
                        fileName,
                        fileTypesStringBuilder, pdfPassword
                    )
                }
                .create()

            alertDialog.show()
        } catch (e: BadTokenException) {
            Log_OC.e(TAG, "Error showing wrong storage info, so skipping it: " + e.message)
        }
    }

    private fun startSaving(fileName: String, fileTypesStringBuilder: StringBuilder, pdfPassword: String) {
        //start the save and upload worker
        backgroundJobManager.scheduleImmediateScanDocUploadJob(
            fileTypesStringBuilder.toString(),
            fileName,
            remotePath,
            pdfPassword
        )

        //save the selected location to save scans in preference
        appPreferences.uploadScansLastPath = remotePath

        //send the result back with the selected remote path to open selected remote path
        val intent = Intent()
        val bundle = Bundle()
        bundle.putParcelable(EXTRA_SCAN_DOC_REMOTE_PATH, remoteFilePath)
        intent.putExtras(bundle)
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView.id) {
            R.id.scan_save_without_txt_recognition_pdf_checkbox, R.id.scan_save_with_txt_recognition_pdf_checkbox -> enableDisablePdfPasswordSwitch()
            R.id.scan_save_pdf_password_switch -> showHidePdfPasswordInput(isChecked)
        }
    }

    private fun updateSaveLocationText(path: String) {
        var newPath = path
        remotePath = newPath
        if (newPath.equals(OCFile.ROOT_PATH, ignoreCase = true)) {
            newPath = resources.getString(R.string.scan_save_location_root)
        }
        binding.scanSaveLocationInput.text = newPath
    }

    private var scanDocSavePathResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                if (data != null) {
                    val chosenFolder = FolderPickerActivity.EXTRA_FOLDER?.let {
                        data.getParcelableArgument(FolderPickerActivity.EXTRA_FOLDER, OCFile::class.java)
                    }
                    if (chosenFolder != null) {
                        remoteFilePath = chosenFolder
                        updateSaveLocationText(chosenFolder.remotePath)
                    }
                }
            }
        }

    companion object {
        private const val TAG: String = "SaveScannedDocumentFragment"

        fun newInstance(): SaveScannedDocumentFragment {
            val args = Bundle()
            val fragment = SaveScannedDocumentFragment()
            fragment.arguments = args
            return fragment
        }

        const val SAVE_TYPE_PDF: String = "pdf"
        const val SAVE_TYPE_PNG: String = "png"
        const val SAVE_TYPE_JPG: String = "jpg"
        const val SAVE_TYPE_PDF_OCR: String = "pdf_ocr"
        const val SAVE_TYPE_TXT: String = "txt"

        const val EXTRA_SCAN_DOC_REMOTE_PATH: String = "scan_doc_remote_path"
    }
}
