/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nextcloud.android.common.ui.theme.utils.ColorRole;
import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.databinding.FileDetailsSharingMenuBottomSheetFragmentBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.fragment.util.SharePermissionManager;
import com.owncloud.android.utils.theme.ViewThemeUtils;

/**
 * File Details Sharing option menus {@link android.app.Dialog} styled as a bottom sheet for main actions.
 */
public class FileDetailSharingMenuBottomSheetDialog extends BottomSheetDialog {
    private FileDetailsSharingMenuBottomSheetFragmentBinding binding;
    private final FileDetailsSharingMenuBottomSheetActions actions;
    private final OCShare ocShare;
    private final ViewThemeUtils viewThemeUtils;
    private final boolean encrypted;

    public FileDetailSharingMenuBottomSheetDialog(FileActivity fileActivity,
                                                  FileDetailsSharingMenuBottomSheetActions actions,
                                                  OCShare ocShare,
                                                  ViewThemeUtils viewThemeUtils,
                                                  boolean encrypted) {
        super(fileActivity);
        this.actions = actions;
        this.ocShare = ocShare;
        this.viewThemeUtils = viewThemeUtils;
        this.encrypted = encrypted;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FileDetailsSharingMenuBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        updateUI();

        setupClickListener();

        setOnShowListener(d ->
                              BottomSheetBehavior.from((View) binding.getRoot().getParent())
                                  .setPeekHeight(binding.getRoot().getMeasuredHeight())
                         );
    }

    private void updateUI() {
        if (ocShare.isFolder()) {
            binding.menuShareOpenIn.setVisibility(View.GONE);
        } else {
            binding.menuShareOpenIn.setVisibility(View.VISIBLE);
        }

        if (ocShare.getShareType() == ShareType.PUBLIC_LINK) {
            if (MDMConfig.INSTANCE.sendFilesSupport(getContext())) {
                binding.menuShareSendNewEmail.setVisibility(View.GONE);
            }
        } else {
            binding.menuShareSendNewEmail.setVisibility(View.VISIBLE);
        }

        if (SharePermissionManager.INSTANCE.isSecureFileDrop(ocShare) && encrypted) {
            binding.menuShareAdvancedPermissions.setVisibility(View.GONE);
        }
    }

    private void setupClickListener() {
        binding.menuShareOpenIn.setOnClickListener(v -> {
            actions.openIn(ocShare);
            dismiss();
        });

        binding.menuShareAdvancedPermissions.setOnClickListener(v -> {
            actions.advancedPermissions(ocShare);
            dismiss();
        });

        binding.menuShareSendNewEmail.setOnClickListener(v -> {
            actions.sendNewEmail(ocShare);
            dismiss();
        });

        binding.menuShareUnshare.setOnClickListener(v -> {
            actions.unShare(ocShare);
            dismiss();
        });

        binding.menuShareSendLink.setOnClickListener(v -> {
            actions.sendLink(ocShare);
            dismiss();
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        binding = null;
    }
}
