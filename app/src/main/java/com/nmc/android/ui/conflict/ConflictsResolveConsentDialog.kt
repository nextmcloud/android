/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.nmc.android.ui.conflict

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ConflictResolveConsentDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.dialog.conflict.ConflictsResolveDialog
import com.owncloud.android.ui.dialog.conflict.model.ConflictDialogType
import com.owncloud.android.utils.DisplayUtils
import java.io.File
import javax.inject.Inject

/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
class ConflictsResolveConsentDialog : DialogFragment(), Injectable {
    private lateinit var binding: ConflictResolveConsentDialogBinding

    var listener: ConflictsResolveDialog.OnConflictDecisionMadeListener? = null
    private lateinit var dialogType: ConflictDialogType
    private var user: User? = null
    private var leftDataFile: File? = null
    private var rightDataFile: OCFile? = null

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as ConflictsResolveDialog.OnConflictDecisionMadeListener
        } catch (_: ClassCastException) {
            throw ClassCastException("Activity of this dialog must implement OnConflictDecisionMadeListener")
        }
    }

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog?

        if (alertDialog == null) {
            activity?.let {
                DisplayUtils.showSnackMessage(it, R.string.failed_to_create_conflict_dialog)
            }
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = requireArguments()
        dialogType = requireNotNull(bundle.getParcelableArgument(ARG_CONFLICT_DATA, ConflictDialogType::class.java)) {
            "$ARG_CONFLICT_DATA is required, create this dialog through ConflictResolveDialogFactory"
        }
        leftDataFile = bundle.getSerializableArgument(ARG_LEFT_FILE, File::class.java)
        rightDataFile = bundle.getParcelableArgument(ARG_RIGHT_FILE, OCFile::class.java)
        user = bundle.getParcelableArgument(ARG_USER, User::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = ConflictResolveConsentDialogBinding.inflate(requireActivity().layoutInflater)

        // TODO: 26-05-2021 change replace and keep both button text for multiple files
        binding.replaceBtn.setOnClickListener {
            val isDialogTitleNotExists = (dialogType.dialogTitle == null)
            listener?.conflictDecisionMade(
                if (isDialogTitleNotExists) ConflictsResolveDialog.Decision.KEEP_OFFLINE_FOLDER
                else ConflictsResolveDialog.Decision.KEEP_LOCAL
            )
        }

        binding.keepBothBtn.setOnClickListener {
            val isDialogTitleNotExists = (dialogType.dialogTitle == null)
            listener?.conflictDecisionMade(
                if (isDialogTitleNotExists) ConflictsResolveDialog.Decision.KEEP_BOTH_FOLDER
                else ConflictsResolveDialog.Decision.KEEP_BOTH
            )
        }

        binding.moreDetailsBtn.setOnClickListener { }

        binding.cancelKeepExistingBtn.setOnClickListener {
            val isDialogTitleNotExists = (dialogType.dialogTitle == null)
            listener?.conflictDecisionMade(
                if (isDialogTitleNotExists) ConflictsResolveDialog.Decision.KEEP_SERVER_FOLDER
                else ConflictsResolveDialog.Decision.KEEP_SERVER
            )
        }

        // Build the dialog
        // TODO: 26-05-2021 Handle multiple dialog message
        val dialogMessage = String.format(
            getString(R.string.conflict_dialog_message),
            fileDataStorageManager.getFileByEncryptedRemotePath(rightDataFile?.remotePath).fileName
        )
        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(binding.root)
            // TODO: 26-05-2021 handle multiple dialog title
            .setTitle(getString(R.string.conflict_dialog_title))
            .setMessage(dialogMessage)


        return builder.create()
    }

    fun showDialog(activity: AppCompatActivity) {
        val prev = activity.supportFragmentManager.findFragmentByTag("dialog")
        activity.supportFragmentManager.beginTransaction().run {
            if (prev != null) {
                this.remove(prev)
            }
            addToBackStack(null)
            show(this, "dialog")
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        listener?.conflictDecisionMade(ConflictsResolveDialog.Decision.CANCEL)
    }

    companion object {
        internal const val ARG_CONFLICT_DATA = "CONFLICT_DATA"
        internal const val ARG_LEFT_FILE = "LEFT_FILE"
        internal const val ARG_RIGHT_FILE = "RIGHT_FILE"
        internal const val ARG_USER = "USER"
    }
}
