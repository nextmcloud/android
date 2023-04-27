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

import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.fragment.util.SharePermissionManager;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

class LinkShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareLinkShareItemBinding binding;
    private Context context;
    private ViewThemeUtils viewThemeUtils;
    private boolean encrypted;
    private boolean isTextFile;

    public LinkShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public LinkShareViewHolder(FileDetailsShareLinkShareItemBinding binding,
                               Context context,
                               final ViewThemeUtils viewThemeUtils,
                               boolean encrypted,
                               boolean isTextFile) {
        this(binding.getRoot());
        this.binding = binding;
        this.context = context;
        this.viewThemeUtils = viewThemeUtils;
        this.encrypted = encrypted;
        this.isTextFile = isTextFile;
    }

    public void bind(OCShare publicShare, ShareeListAdapterListener listener, int position) {
        if (ShareType.EMAIL == publicShare.getShareType()) {
            binding.name.setText(publicShare.getSharedWithDisplayName());
            binding.icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                                                                      R.drawable.ic_external_share,
                                                                      null));
            binding.overflowMenu.setVisibility(View.VISIBLE);
            binding.copyLink.setVisibility(View.GONE);
            binding.detailText.setVisibility(View.GONE);
        } else {
            String label = publicShare.getLabel();

            if (!TextUtils.isEmpty(label)) {
                binding.name.setText(context.getString(R.string.share_link_with_label, label));
            } else if (SharePermissionManager.INSTANCE.isFileRequest(publicShare)) {
                binding.name.setText(R.string.share_permission_file_drop);
            } else if (SharePermissionManager.INSTANCE.isSecureFileDrop(publicShare) && encrypted) {
                binding.name.setText(R.string.share_permission_secure_file_drop);
            } else {
                int textRes = (position == 0) ? R.string.share_link : R.string.share_link_with_label;
                Object arg = (position == 0) ? null : String.valueOf(position);
                binding.name.setText((position == 0) ? context.getString(textRes)
                                         : context.getString(textRes, arg));
            }

            binding.overflowMenu.setVisibility(View.GONE);
            binding.copyLink.setVisibility(View.VISIBLE);
            binding.detailText.setVisibility(View.VISIBLE);

        }

        setPermissionName(publicShare, SharePermissionManager.getPermissionName(context, publicShare));
        showHideCalendarIcon(publicShare.getExpirationDate());
        showHidePasswordIcon(publicShare.isPasswordProtected());

        binding.overflowMenu.setOnClickListener(v -> listener.showSharingMenuActionSheet(publicShare));
        if (!SharePermissionManager.INSTANCE.isSecureFileDrop(publicShare) && !encrypted) {
            binding.shareByLinkContainer.setOnClickListener(v -> listener.showPermissionsDialog(publicShare));
        }

        if (MDMConfig.INSTANCE.clipBoardSupport(context)) {
            binding.copyLink.setOnClickListener(v -> listener.copyLink(publicShare));
        } else {
            binding.copyLink.setVisibility(View.GONE);
        }
        binding.detailText.setOnClickListener(v -> listener.showSharingMenuActionSheet(publicShare));
    }

    private void setPermissionName(OCShare publicShare, String permissionName) {
        if (TextUtils.isEmpty(permissionName) || (SharePermissionManager.INSTANCE.isSecureFileDrop(publicShare) && encrypted)) {
            binding.quickPermissionLayout.permissionLayout.setVisibility(View.GONE);
            return;
        }

        binding.quickPermissionLayout.permissionName.setText(permissionName);
        setPermissionTypeIcon(permissionName);
        binding.quickPermissionLayout.permissionLayout.setVisibility(View.VISIBLE);
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
