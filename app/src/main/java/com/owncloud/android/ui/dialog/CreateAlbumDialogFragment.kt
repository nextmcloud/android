/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.extensions.typedActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.EditBoxDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Dialog to input the name for a new folder to create.
 *
 *
 * Triggers the folder creation when name is confirmed.
 */
class CreateAlbumDialogFragment : DialogFragment(), DialogInterface.OnClickListener, Injectable {

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var keyboardUtils: KeyboardUtils

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var accountProvider: CurrentAccountProvider

    private var positiveButton: MaterialButton? = null

    private lateinit var binding: EditBoxDialogBinding

    override fun onStart() {
        super.onStart()
        bindButton()
    }

    private fun bindButton() {
        val dialog = dialog

        if (dialog is AlertDialog) {
            positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
            positiveButton?.let {
                it.isEnabled = false
                viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
            }

            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton
            negativeButton?.let {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bindButton()
        keyboardUtils.showKeyboardForEditText(requireDialog().window, binding.userInput)
    }

    @Suppress("EmptyFunctionBlock")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        binding = EditBoxDialogBinding.inflate(inflater, null, false)

        binding.userInput.setText(R.string.empty)
        viewThemeUtils.material.colorTextInputLayout(binding.userInputContainer)

        binding.userInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                checkFileNameAfterEachType()
            }
        })

        val builder = buildMaterialAlertDialog(binding.root)
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.userInputContainer.context, builder)
        return builder.create()
    }

    private fun getOCCapability(): OCCapability = fileDataStorageManager.getCapability(accountProvider.user.accountName)

    private fun checkFileNameAfterEachType() {
        val newAlbumName = binding.userInput.text?.toString() ?: ""

        val errorMessage = when {
            newAlbumName.isEmpty() -> getString(R.string.album_name_empty)
            else -> null
        }

        if (errorMessage != null) {
            binding.userInputContainer.error = errorMessage
            positiveButton?.isEnabled = false
            if (positiveButton == null) {
                bindButton()
            }
        } else {
            binding.userInputContainer.error = null
            binding.userInputContainer.isErrorEnabled = false
            positiveButton?.isEnabled = true
        }
    }

    private fun buildMaterialAlertDialog(view: View): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(requireActivity())
            .setView(view)
            .setPositiveButton(R.string.folder_confirm_create, this)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(R.string.create_album_dialog_title)
            .setMessage(R.string.create_album_dialog_message)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            val capabilities = getOCCapability()

            var newAlbumName = (getDialog()?.findViewById<View>(R.id.user_input) as TextView)
                .text.toString()

            val errorMessage = when {
                newAlbumName.isEmpty() -> getString(R.string.album_name_empty)
                else -> null
            }

            if (errorMessage != null) {
                DisplayUtils.showSnackMessage(requireActivity(), errorMessage)
                return
            }

            connectivityService.isNetworkAndServerAvailable { result ->
                if (result) {
                    typedActivity<ComponentsGetter>()?.fileOperationsHelper?.createAlbum(newAlbumName)
                } else {
                    DisplayUtils.showSnackMessage(requireActivity(), getString(R.string.offline_mode))
                }
            }
        }
    }

    companion object {
        val TAG: String = CreateAlbumDialogFragment::class.java.simpleName

        /**
         * Public factory method to create new CreateFolderDialogFragment instances.
         *
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(): CreateAlbumDialogFragment {
            return CreateAlbumDialogFragment().apply {
            }
        }
    }
}
