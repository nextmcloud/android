/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 *
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2021 TSI-mc
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.text.TextUtils
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.utils.extensions.remainingDownloadLimit
import com.nextcloud.utils.mdm.MDMConfig
import com.nmc.android.utils.EllipsizeListener
import com.nmc.android.utils.TextViewUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.fragment.util.SharePermissionManager
import com.owncloud.android.ui.fragment.util.SharePermissionManager.isSecureFileDrop
import com.owncloud.android.utils.theme.ViewThemeUtils

internal class LinkShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var binding: FileDetailsShareLinkShareItemBinding? = null
    private var context: Context? = null
    private var viewThemeUtils: ViewThemeUtils? = null
    private var encrypted = false

    constructor(
        binding: FileDetailsShareLinkShareItemBinding,
        context: Context,
        viewThemeUtils: ViewThemeUtils,
        encrypted: Boolean
    ) : this(binding.getRoot()) {
        this.binding = binding
        this.context = context
        this.viewThemeUtils = viewThemeUtils
        this.encrypted = encrypted
    }

    fun bind(publicShare: OCShare, listener: ShareeListAdapterListener, position: Int) {
        setName(binding, context, publicShare, position)
        setPermissionName(binding, context, publicShare)
        showHideCalendarIcon(publicShare.expirationDate)
        showHidePasswordIcon(publicShare.isPasswordProtected)
        setOnClickListeners(binding, listener, publicShare)
        configureCopyLink(binding, context, listener, publicShare)
    }

    @Suppress("ReturnCount")
    private fun setName(
        binding: FileDetailsShareLinkShareItemBinding?,
        context: Context?,
        publicShare: OCShare,
        position: Int
    ) {
        if (binding == null || context == null) {
            return
        }

        if (ShareType.PUBLIC_LINK == publicShare.shareType) {
            val label = publicShare.label
            binding.name.text = when {
                label.isNullOrBlank() && position == 0 ->
                    context.getString(R.string.share_link)

                label.isNullOrBlank() ->
                    context.getString(R.string.share_link_with_label, position.toString())

                else ->
                    context.getString(R.string.share_link_with_label, label)
            }

            binding.overflowMenu.setVisibility(View.GONE)
            binding.copyLink.setVisibility(View.VISIBLE)
            binding.detailText.visibility = View.VISIBLE

            return
        }

        if (ShareType.EMAIL == publicShare.shareType) {
            binding.name.text = publicShare.sharedWithDisplayName

            val emailDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_external_share, null)
            binding.icon.setImageDrawable(emailDrawable)
            binding.overflowMenu.setVisibility(View.VISIBLE)
            binding.copyLink.visibility = View.GONE
            binding.detailText.visibility = View.GONE
            return
        }

        val label = publicShare.label
        if (label.isNullOrEmpty()) {
            return
        }

        binding.name.text = context.getString(R.string.share_link_with_label, label)
    }

    private fun setPermissionName(
        binding: FileDetailsShareLinkShareItemBinding?,
        context: Context?,
        publicShare: OCShare?
    ) {
        if (binding == null || context == null) {
            return
        }

        val permissionName = SharePermissionManager.getPermissionName(context, publicShare)

        if (TextUtils.isEmpty(permissionName) || (isSecureFileDrop(publicShare) && encrypted)) {
            binding.quickPermissionLayout.permissionLayout.visibility = View.GONE
            return
        }

        binding.quickPermissionLayout.permissionName.text = permissionName

        TextViewUtils.isTextEllipsized(binding.quickPermissionLayout.permissionName, object : EllipsizeListener {
            override fun onResult(isEllipsized: Boolean) {
                if (isEllipsized) {
                    binding.quickPermissionLayout.permissionName.text =
                        SharePermissionManager.getShortPermissionName(context, permissionName)
                }
            }
        })
        setPermissionTypeIcon(permissionName)
        binding.quickPermissionLayout.permissionLayout.visibility = View.VISIBLE
    }

    private fun showHideCalendarIcon(expirationDate: Long) {
        binding?.quickPermissionLayout?.calendarPermissionIcon?.setVisibility(if (expirationDate > 0) View.VISIBLE else View.GONE)
    }

    private fun showHidePasswordIcon(isPasswordProtected: Boolean) {
        binding?.quickPermissionLayout?.passwordPermissionIcon?.setVisibility(if (isPasswordProtected) View.VISIBLE else View.GONE)
    }

    private fun setPermissionTypeIcon(permissionName: String?) {
        when (permissionName) {
            context?.resources?.getString(R.string.share_quick_permission_can_edit) -> {
                binding?.quickPermissionLayout?.permissionTypeIcon?.setImageResource(R.drawable.ic_sharing_edit)
                binding?.quickPermissionLayout?.permissionTypeIcon?.setVisibility(View.VISIBLE)
            }

            context?.resources?.getString(R.string.share_quick_permission_can_view) -> {
                binding?.quickPermissionLayout?.permissionTypeIcon?.setImageResource(R.drawable.ic_sharing_read_only)
                binding?.quickPermissionLayout?.permissionTypeIcon?.setVisibility(View.VISIBLE)
            }

            context?.resources?.getString(R.string.share_permission_secure_file_drop), context?.resources?.getString(R.string.share_quick_permission_can_upload) -> {
                binding?.quickPermissionLayout?.permissionTypeIcon?.setImageResource(R.drawable.ic_sharing_file_drop)
                binding?.quickPermissionLayout?.permissionTypeIcon?.setVisibility(View.VISIBLE)
            }

            else -> {
                binding?.quickPermissionLayout?.permissionTypeIcon?.setVisibility(View.GONE)
            }
        }
    }

    private fun setOnClickListeners(
        binding: FileDetailsShareLinkShareItemBinding?,
        listener: ShareeListAdapterListener,
        publicShare: OCShare
    ) {
        if (binding == null) {
            return
        }

        binding.overflowMenu.setOnClickListener {
            listener.showSharingMenuActionSheet(publicShare)
        }
        binding.shareByLinkContainer.setOnClickListener {
            listener.showPermissionsDialog(publicShare)
        }
        binding.detailText.setOnClickListener {
            listener.showSharingMenuActionSheet(publicShare)
        }
    }

    private fun configureCopyLink(
        binding: FileDetailsShareLinkShareItemBinding?,
        context: Context?,
        listener: ShareeListAdapterListener,
        publicShare: OCShare
    ) {
        if (binding == null || context == null) {
            return
        }

        if (MDMConfig.clipBoardSupport(context)) {
            binding.copyLink.setOnClickListener { v: View? -> listener.copyLink(publicShare) }
        } else {
            binding.copyLink.setVisibility(View.GONE)
        }
    }
}
