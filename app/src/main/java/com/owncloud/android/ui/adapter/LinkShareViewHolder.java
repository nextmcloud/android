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

import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;
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
            binding.copyLink.setVisibility(View.GONE);
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

        }

        String permissionName = SharingMenuHelper.getPermissionName(context, publicShare);
        setPermissionName(publicShare, permissionName, listener);

        binding.copyLink.setOnClickListener(v -> listener.copyLink(publicShare));
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

        if (!TextUtils.isEmpty(permissionName) && !SharingMenuHelper.isSecureFileDrop(publicShare)) {
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
