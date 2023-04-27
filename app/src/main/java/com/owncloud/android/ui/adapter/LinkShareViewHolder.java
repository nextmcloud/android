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
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.View;

import com.nextcloud.android.lib.resources.files.FileDownloadLimit;
import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.datamodel.quickPermission.QuickPermissionType;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.fragment.util.SharePermissionManager;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;
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
            binding.copyLink.setVisibility(View.GONE);
        } else {
            String label = publicShare.getLabel();

            if (!TextUtils.isEmpty(label)) {
                binding.name.setText(context.getString(R.string.share_link_with_label, label));
            } else if (SharePermissionManager.INSTANCE.isFileRequest(publicShare)) {
                binding.name.setText(R.string.share_permission_file_request);
            } else if (SharePermissionManager.INSTANCE.isSecureFileDrop(publicShare) && encrypted) {
                binding.name.setText(R.string.share_permission_secure_file_drop);
            } else {
                int textRes = (position == 0) ? R.string.share_link : R.string.share_link_with_label;
                Object arg = (position == 0) ? null : String.valueOf(position);
                binding.name.setText((position == 0) ? context.getString(textRes)
                                         : context.getString(textRes, arg));
            }
        }

        FileDownloadLimit downloadLimit = publicShare.getFileDownloadLimit();
        if (downloadLimit != null && downloadLimit.getLimit() > 0) {
            int remaining = downloadLimit.getLimit() - downloadLimit.getCount();
            String text = context.getResources().getQuantityString(R.plurals.share_download_limit_description, remaining, remaining);

            binding.subline.setText(text);
            binding.subline.setVisibility(View.VISIBLE);
        } else {
            binding.subline.setVisibility(View.GONE);
        }

        QuickPermissionType quickPermissionType = SharePermissionManager.INSTANCE.getSelectedType(publicShare, encrypted);
        setPermissionName(publicShare, quickPermissionType.getText(context), listener);

        binding.overflowMenu.setOnClickListener(v -> listener.showSharingMenuActionSheet(publicShare));
    }

    private void setPermissionName(OCShare publicShare, String permissionName, ShareeListAdapterListener listener) {
        ColorStateList colorStateList = new ColorStateList(
            new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{android.R.attr.state_enabled},
            },
            new int[]{
                ResourcesCompat.getColor(context.getResources(), R.color.share_disabled_txt_color,
                                         null),
                ResourcesCompat.getColor(context.getResources(), R.color.primary,
                                         null)
            }
        );
        TextViewCompat.setCompoundDrawableTintList(binding.permissionName, colorStateList);
        binding.permissionName.setTextColor(colorStateList);

        if (!TextUtils.isEmpty(permissionName) && !SharePermissionManager.INSTANCE.isSecureFileDrop(publicShare) && !encrypted) {
            if (permissionName.equalsIgnoreCase(context.getResources().getString(R.string.share_permission_read_only)) && !isTextFile) {
                binding.permissionName.setEnabled(false);
            } else {
                binding.permissionName.setEnabled(true);
                binding.shareByLinkContainer.setOnClickListener(v -> listener.showPermissionsDialog(publicShare));
            }
            binding.permissionName.setText(permissionName);
            binding.permissionName.setVisibility(View.VISIBLE);
        } else {
            binding.permissionName.setVisibility(View.GONE);
        }
    }
}
