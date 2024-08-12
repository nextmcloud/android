package com.nmc.android.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nmc.android.interfaces.OnDocScanListener;
import com.nmc.android.utils.ScanBotSdkUtils;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ItemScannedDocBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import io.scanbot.sdk.ScanbotSDK;
import io.scanbot.sdk.process.FilterOperation;
import io.scanbot.sdk.process.ImageFilterType;
import io.scanbot.sdk.process.RotateOperation;

public class ScanPagerFragment extends Fragment {

    private static final String ARG_SCANNED_DOC_PATH = "scanned_doc_path";

    public ScanPagerFragment() {
    }

    public static ScanPagerFragment newInstance(int i) {

        Bundle args = new Bundle();
        args.putInt(ARG_SCANNED_DOC_PATH, i);

        ScanPagerFragment fragment = new ScanPagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private ItemScannedDocBinding binding;

    private ScanbotSDK scanbotSDK;
    private Bitmap originalBitmap;
    private Bitmap previewBitmap;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());

    private long lastRotationEventTs = 0L;
    private int rotationDegrees = 0;
    private int index;

    private OnDocScanListener onDocScanListener;
    private AlertDialog applyFilterDialog;
    private int selectedFilter = 0;

    //flag to check if applying filter is in progress or not
    private boolean isFilterApplyInProgress = false;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            onDocScanListener = (OnDocScanListener) context;
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SCANNED_DOC_PATH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (requireActivity() instanceof ScanActivity) {
            scanbotSDK = ((ScanActivity) requireActivity()).getScanbotSDK();
        }
        binding = ItemScannedDocBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //File file = new File(scannedDocPath);
        //originalBitmap = FileUtils.convertFileToBitmap(file);
        // previewBitmap = ScanBotSdkUtils.resizeForPreview(originalBitmap);
        // loadImage();
        setUpBitmap();
    }

    private void setUpBitmap() {
        executorService.execute(() -> {
            if (index >= 0 && index < ScanActivity.filteredImages.size()) {
                originalBitmap = onDocScanListener.getScannedDocs().get(index);
                previewBitmap = ScanBotSdkUtils.resizeForPreview(originalBitmap);
            }
            if (index >= 0 && index < ScanActivity.scannedImagesFilterIndex.size()) {
                selectedFilter = ScanActivity.scannedImagesFilterIndex.get(index);
            }
            handler.post(() -> loadImage());
        });
    }

    private void loadImage() {
        if (binding != null) {
            if (previewBitmap != null) {
                binding.editScannedImageView.setImageBitmap(previewBitmap);
            } else if (originalBitmap != null) {
                binding.editScannedImageView.setImageBitmap(originalBitmap);
            }
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;

        super.onDestroyView();

        if (applyFilterDialog != null && applyFilterDialog.isShowing()) {
            applyFilterDialog.dismiss();
        }
    }

    public void rotate() {
        if (System.currentTimeMillis() - lastRotationEventTs < 350) {
            return;
        }
        rotationDegrees += 90;
        binding.editScannedImageView.rotateClockwise();
        lastRotationEventTs = System.currentTimeMillis();
        executorService.execute(() -> {
            Bitmap rotatedBitmap = scanbotSDK.imageProcessor().processBitmap(originalBitmap,
                                                                       new ArrayList<>(Collections.singletonList(new RotateOperation(rotationDegrees))), false);
            onDocScanListener.replaceScannedDoc(index, rotatedBitmap, false);
        });
    }

    public void showApplyFilterDialog() {
        String[] filterArray = getResources().getStringArray(R.array.edit_scan_filter_values);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.edit_scan_filter_dialog_title)
            .setSingleChoiceItems(filterArray,
                                  selectedFilter,
                                  (dialog, which) -> {
                                      selectedFilter = which;
                                      onDocScanListener.replaceFilterIndex(index, selectedFilter);
                                      if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_none))) {
                                          applyFilter(ImageFilterType.NONE);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_pure_binarized))) {
                                          applyFilter(ImageFilterType.PURE_BINARIZED);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_color_enhanced))) {
                                          applyFilter(ImageFilterType.COLOR_ENHANCED, ImageFilterType.EDGE_HIGHLIGHT);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_color_document))) {
                                          applyFilter(ImageFilterType.COLOR_DOCUMENT);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_grey))) {
                                          applyFilter(ImageFilterType.GRAYSCALE);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_b_n_w))) {
                                          applyFilter(ImageFilterType.BLACK_AND_WHITE);
                                      }

                                      dialog.dismiss();
                                  })
            .setOnCancelListener(dialog -> {
            });
        applyFilterDialog = builder.create();
        applyFilterDialog.show();
    }

    private void applyFilter(ImageFilterType... imageFilterType) {
        binding.editScanImageProgressBar.setVisibility(View.VISIBLE);
        isFilterApplyInProgress = true;
        executorService.execute(() -> {
            if (imageFilterType[0] != ImageFilterType.NONE) {
                List<FilterOperation> filterOperationList = new ArrayList<>();
                for (ImageFilterType filters : imageFilterType) {
                    filterOperationList.add(new FilterOperation(filters));
                }
                previewBitmap = scanbotSDK.imageProcessor().processBitmap(originalBitmap, filterOperationList, false);
            } else {
                previewBitmap = ScanActivity.originalScannedImages.get(index);
            }
            onDocScanListener.replaceScannedDoc(index, previewBitmap, true);
            handler.post(() -> {
                isFilterApplyInProgress = false;
                binding.editScanImageProgressBar.setVisibility(View.GONE);
                loadImage();
            });
        });
    }

    //scan should not be saved till filter is applied
    public boolean isFilterApplyInProgress() {
        return isFilterApplyInProgress;
    }
}
