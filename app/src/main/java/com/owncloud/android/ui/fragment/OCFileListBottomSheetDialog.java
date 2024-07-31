/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.nextcloud.client.account.User;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.documentscan.AppScanOptionalFeature;
import com.nextcloud.utils.EditorUtils;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileListActionsBottomSheetCreatorBinding;
import com.owncloud.android.databinding.FileListActionsBottomSheetFragmentBinding;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.Creator;
import com.owncloud.android.lib.common.DirectEditing;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ThemeUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;
import com.nmc.android.utils.ScanBotSdkUtils;

/**
 * FAB menu {@link android.app.Dialog} styled as a bottom sheet for main actions.
 */
public class OCFileListBottomSheetDialog extends BottomSheetDialog implements Injectable {

    private FileListActionsBottomSheetFragmentBinding binding;
    private final OCFileListBottomSheetActions actions;
    private final FileActivity fileActivity;
    private final DeviceInfo deviceInfo;
    private final User user;
    private final OCFile file;
    private final ThemeUtils themeUtils;
    private final ViewThemeUtils viewThemeUtils;
    private final EditorUtils editorUtils;

    private final AppScanOptionalFeature appScanOptionalFeature;


    public OCFileListBottomSheetDialog(FileActivity fileActivity,
                                       OCFileListBottomSheetActions actions,
                                       DeviceInfo deviceInfo,
                                       User user,
                                       OCFile file,
                                       ThemeUtils themeUtils,
                                       ViewThemeUtils viewThemeUtils,
                                       EditorUtils editorUtils,
                                       AppScanOptionalFeature appScanOptionalFeature) {
        super(fileActivity);
        this.actions = actions;
        this.fileActivity = fileActivity;
        this.deviceInfo = deviceInfo;
        this.user = user;
        this.file = file;
        this.themeUtils = themeUtils;
        this.viewThemeUtils = viewThemeUtils;
        this.editorUtils = editorUtils;
        this.appScanOptionalFeature = appScanOptionalFeature;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FileListActionsBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewThemeUtils.platform.colorImageView(binding.menuIconUploadFiles);
        viewThemeUtils.platform.colorImageView(binding.menuIconUploadFromApp);
        viewThemeUtils.platform.colorImageView(binding.menuIconDirectCameraUpload);
        viewThemeUtils.platform.colorImageView(binding.menuIconScanDocUpload);
        viewThemeUtils.platform.colorImageView(binding.menuIconMkdir);
        viewThemeUtils.platform.colorImageView(binding.menuIconAddFolderInfo);

        binding.addToCloud.setText(getContext().getResources().getString(R.string.add_to_cloud,
                                                                         themeUtils.getDefaultDisplayNameForRootFolder(getContext())));

        OCCapability capability = fileActivity.getStorageManager().getCapability(user.getAccountName());
        if (capability.getRichDocuments().isTrue() &&
            capability.getRichDocumentsDirectEditing().isTrue() &&
            capability.getRichDocumentsTemplatesAvailable().isTrue() &&
            !file.isEncrypted()) {
            binding.templates.setVisibility(View.VISIBLE);
        }

        String json = new ArbitraryDataProviderImpl(getContext())
            .getValue(user, ArbitraryDataProvider.DIRECT_EDITING);

        if (!json.isEmpty() &&
            !file.isEncrypted()) {
            DirectEditing directEditing = new Gson().fromJson(json, DirectEditing.class);

            if (!directEditing.getCreators().isEmpty()) {
                binding.creatorsContainer.setVisibility(View.VISIBLE);

                for (Creator creator : directEditing.getCreators().values()) {
                    FileListActionsBottomSheetCreatorBinding creatorViewBinding =
                        FileListActionsBottomSheetCreatorBinding.inflate(getLayoutInflater());

                    View creatorView = creatorViewBinding.getRoot();

                    creatorViewBinding.creatorName.setText(
                        String.format(fileActivity.getString(R.string.editor_placeholder),
                                      fileActivity.getString(R.string.create_new),
                                      creator.getName()));

                    creatorViewBinding.creatorThumbnail.setImageDrawable(
                        MimeTypeUtil.getFileTypeIcon(creator.getMimetype(),
                                                     creator.getExtension(),
                                                     creatorViewBinding.creatorThumbnail.getContext(),
                                                     viewThemeUtils));

                    creatorView.setOnClickListener(v -> {
                        actions.showTemplate(creator, creatorViewBinding.creatorName.getText().toString());
                        dismiss();
                    });

                    binding.creators.addView(creatorView);
                }
            }
        }

        if (!deviceInfo.hasCamera(getContext())) {
            binding.menuDirectCameraUpload.setVisibility(View.GONE);
            binding.menuScanDocument.setVisibility(View.GONE);
        }

        //check if scanbot sdk licence is valid or not
        //hide the view if license is not valid
        if(!ScanBotSdkUtils.isScanBotLicenseValid(fileActivity)){
           // binding.menuScanDocument.setVisibility(View.GONE);
        }

        if (capability.getEndToEndEncryption().isTrue() && OCFile.ROOT_PATH.equals(file.getRemotePath())) {
            binding.menuEncryptedMkdir.setVisibility(View.VISIBLE);
        } else {
            binding.menuEncryptedMkdir.setVisibility(View.GONE);
        }

        // create rich workspace
        if (editorUtils.isEditorAvailable(user,
                                          MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN) &&
            file != null && !file.isEncrypted()) {
            // richWorkspace
            // == "": no info set -> show button
            // == null: disabled on server side -> hide button
            // != "": info set -> hide button
            if (file.getRichWorkspace() == null || !"".equals(file.getRichWorkspace())) {
                binding.menuCreateRichWorkspace.setVisibility(View.GONE);
                binding.menuCreateRichWorkspaceDivider.setVisibility(View.GONE);
            } else {
                binding.menuCreateRichWorkspace.setVisibility(View.VISIBLE);
                binding.menuCreateRichWorkspaceDivider.setVisibility(View.VISIBLE);
            }
        } else {
            binding.menuCreateRichWorkspace.setVisibility(View.GONE);
            binding.menuCreateRichWorkspaceDivider.setVisibility(View.GONE);
        }

        setupClickListener();
    }

    private void setupClickListener() {
        binding.menuCreateRichWorkspace.setOnClickListener(v -> {
            actions.createRichWorkspace();
            dismiss();
        });

        binding.menuMkdir.setOnClickListener(v -> {
            actions.createFolder();
            dismiss();
        });

        binding.menuEncryptedMkdir.setOnClickListener(v -> {
            actions.createEncryptedFolder();
            dismiss();
        });

        binding.menuUploadFromApp.setOnClickListener(v -> {
            actions.uploadFromApp();
            dismiss();
        });

        binding.menuDirectCameraUpload.setOnClickListener(v -> {
            actions.directCameraUpload();
            dismiss();
        });

        if (appScanOptionalFeature.isAvailable()) {
            binding.menuScanDocUpload.setOnClickListener(v -> {
                actions.scanDocUpload();
                dismiss();
            });
        } else {
            binding.menuScanDocUpload.setVisibility(View.GONE);
        }

        binding.menuScanDocument.setOnClickListener(v -> {
            actions.scanDocument();
            dismiss();
        });

        binding.menuUploadFiles.setOnClickListener(v -> {
            actions.uploadFiles();
            dismiss();
        });

        binding.menuNewDocument.setOnClickListener(v -> {
            actions.newDocument();
            dismiss();
        });

        binding.menuNewSpreadsheet.setOnClickListener(v -> {
            actions.newSpreadsheet();
            dismiss();
        });

        binding.menuNewPresentation.setOnClickListener(v -> {
            actions.newPresentation();
            dismiss();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding = null;
    }
}
