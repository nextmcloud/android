/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nmc.android.utils.CheckableThemeUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsSharingProcessFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment
import com.owncloud.android.ui.fragment.util.SharingMenuHelper
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

/**
 * Fragment class to show share permission options, set expiration date, change label, set password, send note
 *
 * This fragment handles following:
 * 1. This will be shown while creating new internal and external share. So that user can set every share
 * configuration at one time.
 * 2. This will handle both Advanced Permissions and Send New Email functionality for existing shares to modify them.
 */
@Suppress("TooManyFunctions")
class FileDetailsSharingProcessFragment :
    Fragment(),
    Injectable,
    ExpirationDatePickerDialogFragment.OnExpiryDateListener,
    RadioGroup.OnCheckedChangeListener {

    companion object {
        const val TAG = "FileDetailsSharingProcessFragment"
        private const val ARG_OCFILE = "arg_sharing_oc_file"
        private const val ARG_SHAREE_NAME = "arg_sharee_name"
        private const val ARG_SHARE_TYPE = "arg_share_type"
        private const val ARG_OCSHARE = "arg_ocshare"
        private const val ARG_SCREEN_TYPE = "arg_screen_type"
        private const val ARG_RESHARE_SHOWN = "arg_reshare_shown"
        private const val ARG_EXP_DATE_SHOWN = "arg_exp_date_shown"
        private const val ARG_SECURE_SHARE = "secure_share"
        private const val ARG_IS_TEXT_FILE = "is_text_file"

        // types of screens to be displayed
        const val SCREEN_TYPE_PERMISSION = 1 // permissions screen
        const val SCREEN_TYPE_NOTE = 2 // note screen

        /**
         * fragment instance to be called while creating new share for internal and external share
         */
        @JvmStatic
        fun newInstance(
            file: OCFile,
            shareeName: String,
            shareType: ShareType,
            secureShare: Boolean,
            isTextFile: Boolean
        ): FileDetailsSharingProcessFragment {
            val args = Bundle()
            args.putParcelable(ARG_OCFILE, file)
            args.putSerializable(ARG_SHARE_TYPE, shareType)
            args.putString(ARG_SHAREE_NAME, shareeName)
            args.putBoolean(ARG_SECURE_SHARE, secureShare)
            args.putBoolean(ARG_IS_TEXT_FILE, isTextFile)
            val fragment = FileDetailsSharingProcessFragment()
            fragment.arguments = args
            return fragment
        }

        /**
         * fragment instance to be called while modifying existing share information
         */
        @JvmStatic
        fun newInstance(
            share: OCShare,
            screenType: Int,
            isReshareShown: Boolean,
            isExpirationDateShown: Boolean,
            isTextFile: Boolean
        ): FileDetailsSharingProcessFragment {
            val args = Bundle()
            args.putParcelable(ARG_OCSHARE, share)
            args.putInt(ARG_SCREEN_TYPE, screenType)
            args.putBoolean(ARG_RESHARE_SHOWN, isReshareShown)
            args.putBoolean(ARG_EXP_DATE_SHOWN, isExpirationDateShown)
            args.putBoolean(ARG_IS_TEXT_FILE, isTextFile)
            val fragment = FileDetailsSharingProcessFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils
    @Inject
    lateinit var keyboardUtils: KeyboardUtils

    private lateinit var onEditShareListener: FileDetailSharingFragment.OnEditShareListener

    private lateinit var binding: FileDetailsSharingProcessFragmentBinding
    private var fileOperationsHelper: FileOperationsHelper? = null
    private var fileActivity: FileActivity? = null

    private var file: OCFile? = null // file to be share
    private var shareeName: String? = null
    private lateinit var shareType: ShareType
    private var shareProcessStep = SCREEN_TYPE_PERMISSION // default screen type
    private var permission = OCShare.NO_PERMISSION // no permission
    private var chosenExpDateInMills: Long = -1 // for no expiry date
    private var isTextFile: Boolean = false

    private var share: OCShare? = null
    private var isReShareShown: Boolean = true // show or hide reShare option
    private var isExpDateShown: Boolean = true // show or hide expiry date option
    private var isSecureShare: Boolean = false
    private var isDownloadCountFetched: Boolean = false

    private lateinit var capabilities: OCCapability

    private var expirationDatePickerFragment: ExpirationDatePickerDialogFragment? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onEditShareListener = context as FileDetailSharingFragment.OnEditShareListener
        } catch (e: ClassCastException) {
            throw IllegalStateException("Calling activity must implement the interface")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            file = it.getParcelableArgument(ARG_OCFILE, OCFile::class.java)
            shareeName = it.getString(ARG_SHAREE_NAME)
            share = it.getParcelableArgument(ARG_OCSHARE, OCShare::class.java)

            if (it.containsKey(ARG_SHARE_TYPE)) {
                shareType = it.getSerializableArgument(ARG_SHARE_TYPE, ShareType::class.java)!!
            } else if (share != null) {
                shareType = share!!.shareType!!
            }

            shareProcessStep = it.getInt(ARG_SCREEN_TYPE, SCREEN_TYPE_PERMISSION)
            isReShareShown = it.getBoolean(ARG_RESHARE_SHOWN, true)
            isExpDateShown = it.getBoolean(ARG_EXP_DATE_SHOWN, true)
            isSecureShare = it.getBoolean(ARG_SECURE_SHARE, false)
            isTextFile = it.getBoolean(ARG_IS_TEXT_FILE, false)
        }

        fileActivity = activity as FileActivity?
        capabilities = CapabilityUtils.getCapability(context)

        requireNotNull(fileActivity) { "FileActivity may not be null" }

        permission = capabilities.defaultPermissions ?: OCShare.NO_PERMISSION
    }

    // Updating Hide Download enable/disable on selection of FileDrop
    override fun onCheckedChanged(group: RadioGroup?, checkId: Int) {
        if (binding.shareProcessPermissionFileDrop.id == checkId) {
            binding.shareProcessHideDownloadCheckbox.isChecked = true
            binding.shareProcessHideDownloadCheckbox.isEnabled = false
        } else {
            binding.shareProcessHideDownloadCheckbox.isEnabled = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FileDetailsSharingProcessFragmentBinding.inflate(inflater, container, false)
        fileOperationsHelper = fileActivity?.fileOperationsHelper
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (shareProcessStep == SCREEN_TYPE_PERMISSION) {
            binding.shareProcessPermissionReadOnly.isChecked = true
            showShareProcessFirst()
        } else {
            showShareProcessSecond()
        }
        //Set default value to 0 for download count
        if (!isDownloadCountFetched) {
            binding.shareProcessRemainingDownloadCountTv.text =
                String.format(resources.getString(R.string.download_text), "0")
        }
        binding.shareProcessPermissionRadioGroup.setOnCheckedChangeListener(this)
        implementClickEvents()
        themeView()
    }

    private fun themeView() {
        CheckableThemeUtils.tintSwitch(binding.shareProcessSetPasswordSwitch)
        CheckableThemeUtils.tintSwitch(binding.shareProcessAllowResharingCheckbox)
        CheckableThemeUtils.tintSwitch(binding.shareProcessSetExpDateSwitch)
        CheckableThemeUtils.tintSwitch(binding.shareProcessHideDownloadCheckbox)
        CheckableThemeUtils.tintSwitch(binding.shareProcessChangeNameSwitch)
        CheckableThemeUtils.tintSwitch(binding.shareProcessDownloadLimitSwitch)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Force recreation of dialog fragment when screen rotates
        // This is needed because the calendar layout should be different in portrait and landscape,
        // but as FDA persists through config changes, the dialog is not recreated automatically
        val datePicker = expirationDatePickerFragment
        if (datePicker?.dialog?.isShowing == true) {
            val currentSelectionMillis = datePicker.currentSelectionMillis
            datePicker.dismiss()
            showExpirationDateDialog(currentSelectionMillis)
        }
    }

    private fun showShareProcessFirst() {
        binding.shareProcessGroupOne.visibility = View.VISIBLE
        binding.shareProcessGroupTwo.visibility = View.GONE
        binding.tvSetPasswordEmailWarning.visibility = View.GONE

        if (share != null) {
            setupModificationUI()
        } else {
            setupUpdateUI()
        }

        if (isSecureShare) {
            binding.shareProcessAdvancePermissionTitle.visibility = View.GONE
        }

        // show or hide expiry date
        if (isExpDateShown && !isSecureShare) {
            binding.shareProcessSetExpDateSwitch.visibility = View.VISIBLE
            binding.dividerSharingExpDate.visibility = View.VISIBLE
        } else {
            binding.shareProcessSetExpDateSwitch.visibility = View.GONE
            binding.dividerSharingExpDate.visibility = View.GONE
        }
        shareProcessStep = SCREEN_TYPE_PERMISSION
    }

    private fun setupModificationUI() {
        binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.common_confirm)

        if (share?.isFolder == true) updateViewForFolder() else updateViewForFile()

        // read only / allow upload and editing / file drop
        if (SharingMenuHelper.isUploadAndEditingAllowed(share)) {
            binding.shareProcessPermissionUploadEditing.isChecked = true
        } else if (SharingMenuHelper.isFileDrop(share) && share?.isFolder == true) {
            binding.shareProcessPermissionFileDrop.isChecked = true
        } else if (SharingMenuHelper.isReadOnly(share)) {
            binding.shareProcessPermissionReadOnly.isChecked = true
        }

        shareType = share?.shareType ?: ShareType.NO_SHARED
        updateViewForShareType()
        binding.shareProcessSetPasswordSwitch.isChecked = share?.isPasswordProtected == true
        showPasswordInput(binding.shareProcessSetPasswordSwitch.isChecked)
        updateExpirationDateView()
        showExpirationDateInput(binding.shareProcessSetExpDateSwitch.isChecked)
    }

    private fun setupUpdateUI() {
        binding.shareProcessBtnNext.text = getString(R.string.common_next)
        file.let {
            if (file?.isFolder == true) {
                updateViewForFolder()
            } else {
                updateViewForFile()
            }
            updateViewForShareType()
        }
        showPasswordInput(binding.shareProcessSetPasswordSwitch.isChecked)
        showExpirationDateInput(binding.shareProcessSetExpDateSwitch.isChecked)
    }

    private fun updateViewForShareType() {
        when (shareType) {
            ShareType.EMAIL -> {
                updateViewForExternalShare()
            }

            ShareType.PUBLIC_LINK -> {
                updateViewForLinkShare()
            }

            else -> {
                updateViewForInternalShare()
            }
        }
    }

    private fun updateViewForExternalShare() {
        hideLinkLabelViews()
        updateViewForExternalAndLinkShare()
    }

    private fun updateViewForLinkShare() {
        updateViewForExternalAndLinkShare()
        binding.shareProcessChangeNameSwitch.visibility = View.VISIBLE
        binding.dividerSharingChangeName.visibility = View.VISIBLE
        if (share != null) {
            binding.shareProcessChangeNameEt.setText(share?.label)
            binding.shareProcessChangeNameSwitch.isChecked = !TextUtils.isEmpty(share?.label)
        }
        showChangeNameInput(binding.shareProcessChangeNameSwitch.isChecked)

        //download limit will only be available for files
        if (share?.isFolder == false || file?.isFolder == false) {
            binding.shareProcessDownloadLimitSwitch.visibility = View.VISIBLE
            binding.dividerSharingDownloadLimit.visibility = View.VISIBLE

            //fetch the download limit for link share
            fetchDownloadLimitForShareLink()
        } else {
            binding.shareProcessDownloadLimitSwitch.visibility = View.GONE
            binding.dividerSharingDownloadLimit.visibility = View.GONE
        }

        //the input for download limit will be hidden initially
        //and can be visible back or no depending on the api result
        //from the download limit api
        binding.shareProcessDownloadLimitEt.visibility = View.GONE
        binding.shareProcessRemainingDownloadCountTv.visibility = View.GONE
    }

    private fun updateViewForInternalShare() {
        hideLinkLabelViews()
        binding.shareProcessHideDownloadCheckbox.visibility = View.GONE
        if (isSecureShare) {
            binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
        } else {
            binding.shareProcessAllowResharingCheckbox.visibility = View.VISIBLE
        }
        binding.shareProcessSetPasswordSwitch.visibility = View.GONE

        if (share != null) {
            if (!isReShareShown) {
                binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
            }
            binding.shareProcessAllowResharingCheckbox.isChecked = SharingMenuHelper.canReshare(share)
            if (share?.isFolder == true) {
                hideFileDropView()
            }
        } else if (file?.isFolder == true) {
            hideFileDropView()
        }
    }

    private fun hideFileDropView() {
        //no file drop for internal share due to 403 bad request api issue
        binding.shareProcessPermissionFileDrop.visibility = View.GONE
        binding.shareFileDropInfo.visibility = View.GONE
    }

    private fun hideLinkLabelViews() {
        binding.shareProcessChangeNameSwitch.visibility = View.GONE
        binding.shareProcessChangeNameEt.visibility = View.GONE
        binding.dividerSharingChangeName.visibility = View.GONE

        binding.shareProcessDownloadLimitSwitch.visibility = View.GONE
        binding.shareProcessDownloadLimitEt.visibility = View.GONE
        binding.shareProcessRemainingDownloadCountTv.visibility = View.GONE
        binding.dividerSharingDownloadLimit.visibility = View.GONE
    }

    /**
     * update views where share type external or link share
     */
    private fun updateViewForExternalAndLinkShare() {
        binding.shareProcessHideDownloadCheckbox.visibility = View.VISIBLE
        binding.dividerSharingHideDownload.visibility = View.VISIBLE
        binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
        binding.shareProcessAllowResharingInfo.visibility = View.GONE
        binding.dividerSharingAllowResharing.visibility = View.GONE
        binding.shareProcessSetPasswordSwitch.visibility = View.VISIBLE
        binding.dividerSharingEnterPassword.visibility = View.VISIBLE

        if (share != null) {
            if (SharingMenuHelper.isFileDrop(share)) {
                binding.shareProcessHideDownloadCheckbox.isChecked = true
                binding.shareProcessHideDownloadCheckbox.isEnabled = false
                binding.dividerSharingHideDownload.visibility = View.GONE
            } else {
                binding.shareProcessHideDownloadCheckbox.isEnabled = true
                binding.dividerSharingHideDownload.visibility = View.VISIBLE
                binding.shareProcessHideDownloadCheckbox.isChecked = share?.isHideFileDownload == true
            }
        }
    }

    /**
     * update expiration date view while modifying the share
     */
    private fun updateExpirationDateView() {
        share?.let { share ->
            if (share.expirationDate > 0) {
                chosenExpDateInMills = share.expirationDate
                binding.shareProcessSetExpDateSwitch.isChecked = true
                binding.shareProcessSelectExpDate.text = getString(
                    R.string.share_expiration_date_format,
                    SimpleDateFormat.getDateInstance().format(Date(share.expirationDate))
                )
            }
        }
    }

    private fun updateViewForFile() {
        binding.shareProcessPermissionUploadEditing.text = getString(R.string.link_share_editing)
        binding.shareProcessPermissionFileDrop.visibility = View.GONE
        binding.shareFileDropInfo.visibility = View.GONE
    }

    private fun updateViewForFolder() {
        binding.shareProcessPermissionUploadEditing.text = getString(R.string.link_share_allow_upload_and_editing)
        binding.shareProcessPermissionFileDrop.visibility = View.VISIBLE
        binding.shareFileDropInfo.visibility = View.VISIBLE
        if (isSecureShare) {
            binding.shareProcessPermissionFileDrop.visibility = View.GONE
            binding.shareFileDropInfo.visibility = View.GONE
            binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
            binding.shareProcessSetExpDateSwitch.visibility = View.GONE
        }
    }

    /**
     * update views for screen type Note
     */
    private fun showShareProcessSecond() {
        binding.shareProcessGroupOne.visibility = View.GONE
        binding.shareProcessGroupTwo.visibility = View.VISIBLE
        if (share != null) {
            binding.shareProcessBtnNext.text = getString(R.string.send_email)
            binding.noteText.setText(share?.note)

            //show the warning label if password protection is enabled
            /* binding.tvSetPasswordEmailWarning.visibility =
                 if (share?.isPasswordProtected == true) View.VISIBLEF
                 else View.GONE*/
        } else {
            binding.shareProcessBtnNext.text = getString(R.string.send_share)
            binding.noteText.setText(R.string.empty)
            //show the warning label if password protection is enabled
            /* binding.tvSetPasswordEmailWarning.visibility =
                 if (binding.shareProcessSetPasswordSwitch.isChecked) View.VISIBLE
                 else View.GONE*/
        }
        shareProcessStep = SCREEN_TYPE_NOTE
    }

    private fun implementClickEvents() {
        binding.shareProcessBtnCancel.setOnClickListener {
            onCancelClick()
        }
        binding.shareProcessBtnNext.setOnClickListener {
            if (shareProcessStep == SCREEN_TYPE_PERMISSION) {
                validateShareProcessFirst()
            } else {
                validateShareProcessSecond()
            }
        }
        binding.shareProcessSetPasswordSwitch.setOnCheckedChangeListener { _, isChecked ->
            showPasswordInput(isChecked)
        }
        binding.shareProcessSetExpDateSwitch.setOnCheckedChangeListener { _, isChecked ->
            showExpirationDateInput(isChecked)
        }
        binding.shareProcessChangeNameSwitch.setOnCheckedChangeListener { _, isChecked ->
            showChangeNameInput(isChecked)
        }
        binding.shareProcessSelectExpDate.setOnClickListener {
            showExpirationDateDialog()
        }
        binding.shareProcessDownloadLimitSwitch.setOnCheckedChangeListener { _, isChecked ->
            showDownloadLimitInput(isChecked)
        }
        binding.noteText.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            return@setOnTouchListener false
        }

    }

    private fun showExpirationDateDialog(chosenDateInMillis: Long = chosenExpDateInMills) {
        val dialog = ExpirationDatePickerDialogFragment.newInstance(chosenDateInMillis)
        dialog.setOnExpiryDateListener(this)
        expirationDatePickerFragment = dialog
        fileActivity?.let {
            dialog.show(
                it.supportFragmentManager,
                ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
            )
        }
    }

    private fun showChangeNameInput(isChecked: Boolean) {
        binding.shareProcessChangeNameEt.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (!isChecked) {
            binding.shareProcessChangeNameEt.setText("")
            //hide keyboard when user unchecks
            hideKeyboard()
        }
    }

    private fun showDownloadLimitInput(isChecked: Boolean) {
        binding.shareProcessDownloadLimitEt.visibility = if (isChecked) View.VISIBLE else View.GONE
        binding.shareProcessRemainingDownloadCountTv.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (!isChecked) {
            binding.shareProcessDownloadLimitEt.setText(R.string.empty)
            if (!isDownloadCountFetched) {
                binding.shareProcessRemainingDownloadCountTv.text = String.format(resources.getString(R.string.download_text), "0")
            }
            //hide keyboard when user unchecks
            hideKeyboard()
        }
    }

    private fun onCancelClick() {
        // hide keyboard when user clicks cancel button
        hideKeyboard()
        // if modifying the existing share then on back press remove the current fragment
        if (share != null) {
            removeCurrentFragment()
        }
        // else we have to check if user is in step 2(note screen) then show step 1 (permission screen)
        // and if user is in step 1 (permission screen) then remove the fragment
        else {
            if (shareProcessStep == SCREEN_TYPE_NOTE) {
                showShareProcessFirst()
            } else {
                removeCurrentFragment()
            }
        }
    }

    private fun showExpirationDateInput(isChecked: Boolean) {
        binding.shareProcessSelectExpDate.visibility = if (isChecked) View.VISIBLE else View.GONE
        binding.shareProcessExpDateDivider.visibility = if (isChecked) View.VISIBLE else View.GONE

        //update margin of divider when switch is enabled/disabled
        val margin = if (isChecked) requireContext().resources.getDimensionPixelOffset(R.dimen.standard_half_margin)
        else 0
        val param = binding.dividerSharingExpDate.layoutParams as ViewGroup.MarginLayoutParams
        param.setMargins(0, margin, 0, 0)
        binding.dividerSharingExpDate.layoutParams = param

        // reset the expiration date if switch is unchecked
        if (!isChecked) {
            chosenExpDateInMills = -1
            binding.shareProcessSelectExpDate.text = getString(R.string.empty)
        }
    }

    private fun showPasswordInput(isChecked: Boolean) {
        binding.shareProcessEnterPassword.visibility = if (isChecked) View.VISIBLE else View.GONE

        // reset the password if switch is unchecked
        if (!isChecked) {
            binding.shareProcessEnterPassword.setText(R.string.empty)
            // hide keyboard when user unchecks
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        if (this::binding.isInitialized) {
            keyboardUtils.hideKeyboardFrom(requireContext(), binding.root)
        }
    }

    /**
     * remove the fragment and pop it from backstack because we are adding it to backstack
     */
    private fun removeCurrentFragment() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun getReSharePermission(): Int {
        val spb = SharePermissionsBuilder()
        spb.setSharePermission(true)
        return spb.build()
    }

    /**
     * method to validate the step 1 screen information
     */
    @Suppress("ReturnCount")
    private fun validateShareProcessFirst() {
        hideKeyboard()
        permission = getSelectedPermission()
        if (permission == OCShare.NO_PERMISSION) {
            DisplayUtils.showSnackMessage(binding.root, R.string.no_share_permission_selected)
            return
        }

        if (binding.shareProcessSetPasswordSwitch.isChecked &&
            binding.shareProcessEnterPassword.text?.trim().isNullOrEmpty()
        ) {
            DisplayUtils.showSnackMessage(binding.root, R.string.share_link_empty_password)
            return
        }

        if (binding.shareProcessSetExpDateSwitch.isChecked &&
            binding.shareProcessSelectExpDate.text?.trim().isNullOrEmpty()
        ) {
            showExpirationDateDialog()
            return
        }

        if (binding.shareProcessChangeNameSwitch.isChecked &&
            binding.shareProcessChangeNameEt.text?.trim().isNullOrEmpty()
        ) {
            DisplayUtils.showSnackMessage(binding.root, R.string.label_empty)
            return
        }

        if (binding.shareProcessDownloadLimitSwitch.isChecked) {
            val downloadLimit = binding.shareProcessDownloadLimitEt.text?.trim()
            if (downloadLimit.isNullOrEmpty()) {
                DisplayUtils.showSnackMessage(binding.root, R.string.download_limit_empty)
                return
            } else if (downloadLimit.toString().toLong() <= 0) {
                DisplayUtils.showSnackMessage(binding.root, R.string.download_limit_zero)
                return
            }
        }

        // if modifying existing share information then execute the process
        if (share != null) {
            updateShare()
            removeCurrentFragment()
        } else {
            // else show step 2 (note screen)
            showShareProcessSecond()
        }
    }

    /**
     *  get the permissions on the basis of selection
     */
    private fun getSelectedPermission() = when {
        binding.shareProcessAllowResharingCheckbox.isChecked -> getReSharePermission()
        binding.shareProcessPermissionReadOnly.isChecked -> OCShare.READ_PERMISSION_FLAG
        binding.shareProcessPermissionUploadEditing.isChecked -> when {
            file?.isFolder == true || share?.isFolder == true -> SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER
            else -> SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
        }

        binding.shareProcessPermissionFileDrop.isChecked -> OCShare.CREATE_PERMISSION_FLAG
        else -> permission
    }

    private fun updateShare() {
        fileOperationsHelper?.updateShareInformation(
            share,
            permission,
            binding.shareProcessHideDownloadCheckbox.isChecked,
            binding.shareProcessEnterPassword.text.toString().trim(),
            chosenExpDateInMills,
            binding.shareProcessChangeNameEt.text.toString().trim(),
            binding.shareProcessDownloadLimitEt.text.toString().trim()
        )
        // copy the share link if available
        if (!TextUtils.isEmpty(share?.shareLink)) {
            ClipboardUtil.copyToClipboard(requireActivity(), share?.shareLink)
        }
    }

    /**
     * method to validate step 2 (note screen) information
     */
    private fun validateShareProcessSecond() {
        hideKeyboard()
        // if modifying existing share then directly update the note and send email
        if (share != null) {
            if (TextUtils.isEmpty(binding.noteText.text.toString().trim())) {
                DisplayUtils.showSnackMessage(binding.root, R.string.share_link_empty_note_message)
                return
            }
            fileOperationsHelper?.updateNoteToShare(share, binding.noteText.text.toString().trim())
        } else {
            // else create new share
            fileOperationsHelper?.shareFileWithSharee(
                file,
                shareeName,
                shareType,
                permission,
                binding
                    .shareProcessHideDownloadCheckbox.isChecked,
                binding.shareProcessEnterPassword.text.toString().trim(),
                chosenExpDateInMills,
                binding.noteText.text.toString().trim(),
                binding.shareProcessChangeNameEt.text.toString().trim(),
                true
            )
        }
        removeCurrentFragment()
    }

    /**
     * fetch the download limit for the link share
     * the response will be received in FileActivity --> onRemoteOperationFinish() method
     */
    private fun fetchDownloadLimitForShareLink() {
        //need to call this method in handler else to show progress dialog it will throw exception
        Handler(Looper.getMainLooper()).post {
            share?.let {
                fileOperationsHelper?.getShareDownloadLimit(it.token)
            }
        }
    }

    /**
     * method will be called from DrawerActivity on back press to handle screen backstack
     */
    fun onBackPressed() {
        onCancelClick()
    }

    override fun onDateSet(year: Int, monthOfYear: Int, dayOfMonth: Int, chosenDateInMillis: Long) {
        binding.shareProcessSelectExpDate.text = getString(
            R.string.share_expiration_date_format,
            SimpleDateFormat.getDateInstance().format(Date(chosenDateInMillis))
        )
        this.chosenExpDateInMills = chosenDateInMillis
    }

    override fun onDateUnSet() {
        binding.shareProcessSetExpDateSwitch.isChecked = false
    }

    /**
     * will be called when download limit is fetched
     */
    fun onLinkShareDownloadLimitFetched(downloadLimit: Long, downloadCount: Long) {
        binding.shareProcessDownloadLimitSwitch.isChecked = downloadLimit > 0
        showDownloadLimitInput(binding.shareProcessDownloadLimitSwitch.isChecked)
        binding.shareProcessDownloadLimitEt.setText(if (downloadLimit > 0) downloadLimit.toString() else "")
        binding.shareProcessRemainingDownloadCountTv.text = String.format(resources.getString(R.string.download_text), downloadCount.toString())
        isDownloadCountFetched = true
    }
}
