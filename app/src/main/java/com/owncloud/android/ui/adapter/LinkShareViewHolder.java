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

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

class LinkShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareLinkShareItemBinding binding;
    private Context context;
    private ViewThemeUtils viewThemeUtils;
    private boolean isTextFile;

    public LinkShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public LinkShareViewHolder(FileDetailsShareLinkShareItemBinding binding,
                               Context context,
                               final ViewThemeUtils viewThemeUtils,
                               boolean isTextFile) {
        this(binding.getRoot());
        this.binding = binding;
        this.context = context;
        this.viewThemeUtils = viewThemeUtils;
        this.isTextFile = isTextFile;
    }

    public void bind(OCShare publicShare, ShareeListAdapterListener listener) {
        if (ShareType.EMAIL == publicShare.getShareType()) {
            binding.name.setText(publicShare.getSharedWithDisplayName());
            binding.icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                                                                      R.drawable.ic_external_share,
                                                                      null));
            binding.overflowMenu.setVisibility(View.VISIBLE);
            binding.copyLink.setVisibility(View.GONE);
            binding.detailText.setVisibility(View.GONE);
        } else {
            if (!TextUtils.isEmpty(publicShare.getLabel())) {
                String text = String.format(context.getString(R.string.share_link_with_label), publicShare.getLabel());
                binding.name.setText(text);
            } else {
                if (SharingMenuHelper.isSecureFileDrop(publicShare)) {
                    binding.name.setText(context.getResources().getString(R.string.share_permission_secure_file_drop));
                } else {
                    binding.name.setText(R.string.share_link);
                }
            }

            binding.overflowMenu.setVisibility(View.GONE);
            binding.copyLink.setVisibility(View.VISIBLE);
            binding.detailText.setVisibility(View.VISIBLE);

        }

        String permissionName = SharingMenuHelper.getPermissionName(context, publicShare);
        setPermissionName(publicShare, permissionName, listener);
        showHideCalendarIcon(publicShare.getExpirationDate());
        showHidePasswordIcon(publicShare.isPasswordProtected());

        binding.copyLink.setOnClickListener(v -> listener.copyLink(publicShare));
        binding.overflowMenu.setOnClickListener(v -> listener.showSharingMenuActionSheet(publicShare));
        binding.detailText.setOnClickListener(v -> listener.showSharingMenuActionSheet(publicShare));
    }

    private void setPermissionName(OCShare publicShare, String permissionName, ShareeListAdapterListener listener) {
        if (!TextUtils.isEmpty(permissionName) && !SharingMenuHelper.isSecureFileDrop(publicShare)) {
            binding.shareByLinkContainer.setOnClickListener(v -> listener.showPermissionsDialog(publicShare));
            binding.quickPermissionLayout.permissionName.setText(permissionName);

            setPermissionTypeIcon(permissionName);

            binding.quickPermissionLayout.permissionLayout.setVisibility(View.VISIBLE);
        } else {
            binding.quickPermissionLayout.permissionLayout.setVisibility(View.GONE);
        }
    }

    private void showHideCalendarIcon(long expirationDate) {
        binding.quickPermissionLayout.calendarPermissionIcon.setVisibility(expirationDate > 0 ? View.VISIBLE : View.GONE);
    }

    private void showHidePasswordIcon(boolean isPasswordProtected) {
        binding.quickPermissionLayout.passwordPermissionIcon.setVisibility(isPasswordProtected ? View.VISIBLE : View.GONE);
    }

    private void setPermissionTypeIcon(String permissionName) {
        if (permissionName.equalsIgnoreCase(context.getResources().getString(R.string.share_quick_permission_can_edit))) {
            binding.quickPermissionLayout.permissionTypeIcon.setImageResource(R.drawable.ic_sharing_edit);
            binding.quickPermissionLayout.permissionTypeIcon.setVisibility(View.VISIBLE);
        } else if (permissionName.equalsIgnoreCase(context.getResources().getString(R.string.share_quick_permission_can_view))) {
            binding.quickPermissionLayout.permissionTypeIcon.setImageResource(R.drawable.ic_sharing_read_only);
            binding.quickPermissionLayout.permissionTypeIcon.setVisibility(View.VISIBLE);
        } else if (permissionName.equalsIgnoreCase(context.getResources().getString(R.string.share_permission_secure_file_drop))
            || permissionName.equalsIgnoreCase(context.getResources().getString(R.string.share_quick_permission_can_upload))) {
            binding.quickPermissionLayout.permissionTypeIcon.setImageResource(R.drawable.ic_sharing_file_drop);
            binding.quickPermissionLayout.permissionTypeIcon.setVisibility(View.VISIBLE);
        } else {
            binding.quickPermissionLayout.permissionTypeIcon.setVisibility(View.GONE);
        }
    }
}
