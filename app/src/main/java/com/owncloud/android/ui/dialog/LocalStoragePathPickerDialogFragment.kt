/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2019 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nmc.android.utils.DialogThemeUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.StoragePathDialogBinding
import com.owncloud.android.ui.adapter.StoragePathAdapter
import com.owncloud.android.ui.adapter.StoragePathAdapter.StoragePathAdapterListener
import com.owncloud.android.ui.adapter.StoragePathItem
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.FileStorageUtils.StandardDirectory
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import javax.inject.Inject

class LocalStoragePathPickerDialogFragment :
    DialogFragment(),
    DialogInterface.OnClickListener,
    StoragePathAdapterListener,
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: StoragePathDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        require(activity is StoragePathAdapterListener) {
            "Calling activity must implement " +
                "StoragePathAdapter.StoragePathAdapterListener"
        }

        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        binding = StoragePathDialogBinding.inflate(inflater, null, false)

        val adapter = StoragePathAdapter(pathList, this, viewThemeUtils)
        binding.storagePathRecyclerView.adapter = adapter
        binding.storagePathRecyclerView.layoutManager = LinearLayoutManager(requireActivity())

        // Build the dialog
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder
            .setView(binding.root)
            .setPositiveButton(R.string.common_cancel, this)
            .setTitle(R.string.storage_choose_location)

        //NMC customization
        DialogThemeUtils.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            dismissAllowingStateLoss()
        }
    }

    private val pathList: List<StoragePathItem>
        get() {
            val storagePathItems: MutableList<StoragePathItem> = ArrayList()
            for (standardDirectory in StandardDirectory.getStandardDirectories()) {
                addIfExists(
                    storagePathItems,
                    standardDirectory.icon,
                    getString(standardDirectory.displayName),
                    Environment.getExternalStoragePublicDirectory(standardDirectory.name).absolutePath
                )
            }
            for (dir in FileStorageUtils.getStorageDirectories(requireActivity())) {
                //NMC Customisation
                if (internalStoragePaths.contains(dir)) {
                    val internalStorage = getString(R.string.storage_internal_storage)
                    addIfExists(storagePathItems, R.drawable.ic_sd_grey600, internalStorage, dir)
                } else {
                    val sdCard = getString(R.string.storage_sd_card)
                    addIfExists(storagePathItems, R.drawable.ic_sd, sdCard, dir)
                }
            }
            return storagePathItems
        }

    private fun addIfExists(storagePathItems: MutableList<StoragePathItem>, icon: Int, name: String, path: String) {
        val file = File(path)
        if (file.exists() && file.canRead()) {
            storagePathItems.add(StoragePathItem(icon, name, path))
        }
    }

    override fun chosenPath(path: String) {
        if (activity != null) {
            (activity as StoragePathAdapterListener?)!!.chosenPath(path)
        }
        dismissAllowingStateLoss()
    }

    companion object {
        const val LOCAL_STORAGE_PATH_PICKER_FRAGMENT = "LOCAL_STORAGE_PATH_PICKER_FRAGMENT"
        private val internalStoragePaths: MutableSet<String> = HashSet()

        init {
            internalStoragePaths.add("/storage/emulated/legacy")
            internalStoragePaths.add("/storage/emulated/0")
            internalStoragePaths.add("/mnt/sdcard")
        }

        @JvmStatic
        fun newInstance(): LocalStoragePathPickerDialogFragment {
            return LocalStoragePathPickerDialogFragment()
        }
    }
}
