package com.nmc.android.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import com.nextcloud.client.preferences.AppPreferences;
import com.nmc.android.interfaces.OnDocScanListener;
import com.nmc.android.interfaces.OnFragmentChangeListener;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ActivityScanBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderIfNotExistOperation;
import com.owncloud.android.ui.activity.FileActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import io.scanbot.sdk.ScanbotSDK;

public class ScanActivity extends FileActivity implements OnFragmentChangeListener, OnDocScanListener {

    protected static final String FRAGMENT_SCAN_TAG = "SCAN_FRAGMENT_TAG";
    protected static final String FRAGMENT_EDIT_SCAN_TAG = "EDIT_SCAN_FRAGMENT_TAG";
    protected static final String FRAGMENT_CROP_SCAN_TAG = "CROP_SCAN_FRAGMENT_TAG";
    protected static final String FRAGMENT_SAVE_SCAN_TAG = "SAVE_SCAN_FRAGMENT_TAG";

    //default path to upload the scanned document
    //if user doesn't select any location then this will be the default location
    public static final String DEFAULT_UPLOAD_SCAN_PATH = OCFile.ROOT_PATH + "Scans" + OCFile.PATH_SEPARATOR;

    protected static final String TAG = "ScanActivity";
    private static final String EXTRA_REMOTE_PATH = "com.nmc.android.ui.scan_activity.extras.remote_path";

    private ActivityScanBinding binding;
    private ScanbotSDK scanbotSDK;

    public static final List<Bitmap> originalScannedImages = new ArrayList<>();//list with original bitmaps
    public static final List<Bitmap> filteredImages = new ArrayList<>();//list with bitmaps applied filters
    public static final List<Integer> scannedImagesFilterIndex = new ArrayList<>();//list to maintain the state of
    // applied filter index when device rotated

    @Inject AppPreferences appPreferences;

    private String remotePath;
    //flag to avoid checking folder existence whenever user goes to save fragment
    //we will make it true when the operation finishes first time
    private boolean isFolderCheckOperationFinished;

    public static void openScanActivity(Context context, String remotePath, int requestCode) {
        Intent intent = new Intent(context, ScanActivity.class);
        intent.putExtra(EXTRA_REMOTE_PATH, remotePath);
        ((AppCompatActivity) context).startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate and set the layout view
        binding = ActivityScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        remotePath = getIntent().getStringExtra(EXTRA_REMOTE_PATH);
        originalScannedImages.clear();
        filteredImages.clear();
        scannedImagesFilterIndex.clear();
        initScanbotSDK();
        setupToolbar();
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getDelegate().getSupportActionBar();

        if (actionBar != null) {
            //required for NMC
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.bg_default, null)));
            actionBar.setDisplayHomeAsUpEnabled(true);
            //NMC Customization
            viewThemeUtils.files.themeActionBar(this, actionBar, false);
        }
    }

    public String getRemotePath() {
        return remotePath;
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        createScanFragment(savedInstanceState);
    }

    private void createScanFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            ScanDocumentFragment scanDocumentFragment = ScanDocumentFragment.newInstance(TAG);
            onReplaceFragment(scanDocumentFragment, FRAGMENT_SCAN_TAG, false);
        } else {
            getSupportFragmentManager().findFragmentByTag(FRAGMENT_SCAN_TAG);
        }
    }

    @Override
    public void onReplaceFragment(Fragment fragment, String tag, boolean addToBackStack) {
        //only during replacing save scan fragment
        if (tag.equalsIgnoreCase(FRAGMENT_SAVE_SCAN_TAG)) {
            checkAndCreateFolderIfRequired();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.scan_frame_container, fragment, tag);
        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressHandle();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        onBackPressHandle();
    }

    private void onBackPressHandle() {
        Fragment editScanFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_EDIT_SCAN_TAG);
        Fragment cropScanFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_CROP_SCAN_TAG);
        Fragment saveScanFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_SAVE_SCAN_TAG);
        if (cropScanFragment != null || saveScanFragment != null) {
            int index = 0;
            if (cropScanFragment instanceof CropScannedDocumentFragment) {
                index = ((CropScannedDocumentFragment) cropScanFragment).getScannedDocIndex();
            }
            onReplaceFragment(EditScannedDocumentFragment.newInstance(index), FRAGMENT_EDIT_SCAN_TAG, false);
        } else if (editScanFragment != null) {
            createScanFragment(null);
        } else {
            super.onBackPressed();
        }
    }

    private void initScanbotSDK() {
        scanbotSDK = new ScanbotSDK(this);
    }

    public ScanbotSDK getScanbotSDK() {
        return scanbotSDK;
    }

    @Override
    public void addScannedDoc(Bitmap file) {
        if (file != null) {
            originalScannedImages.add(file);
            filteredImages.add(file);
            scannedImagesFilterIndex.add(0);//no filter by default
        }
    }

    @Override
    public List<Bitmap> getScannedDocs() {
        return filteredImages;
    }

    @Override
    public boolean removedScannedDoc(Bitmap file, int index) {
        //removed the filter applied index also when scanned document is removed
        if (scannedImagesFilterIndex.size() > 0 && scannedImagesFilterIndex.size() > index) {
            scannedImagesFilterIndex.remove(index);
        }
        if (originalScannedImages.size() > 0 && file != null) {
            originalScannedImages.remove(index);
        }
        if (filteredImages.size() > 0 && file != null) {
            filteredImages.remove(index);
            return true;
        }
        return false;
    }

    @Override
    public Bitmap replaceScannedDoc(int index, Bitmap newFile, boolean isFilterApplied) {
        //only update the original bitmap if no filter is applied
        if (!isFilterApplied && originalScannedImages.size() > 0 && newFile != null && index >= 0 && originalScannedImages.size() - 1 >= index) {
            originalScannedImages.set(index, newFile);
        }
        if (filteredImages.size() > 0 && newFile != null && index >= 0 && filteredImages.size() - 1 >= index) {
            return filteredImages.set(index, newFile);
        }
        return null;
    }

    @Override
    public void replaceFilterIndex(int index, int filterIndex) {
        if (scannedImagesFilterIndex.size() > 0 && scannedImagesFilterIndex.size() > index) {
            scannedImagesFilterIndex.set(index, filterIndex);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragmentList) {
            fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void checkAndCreateFolderIfRequired() {
        //if user is coming from sub-folder then we should not check for existence as folder will be available
        if (!TextUtils.isEmpty(remotePath) && !remotePath.equals(OCFile.ROOT_PATH)) {
            return;
        }

        //no need to do any operation if its already finished earlier
        if (isFolderCheckOperationFinished) {
            return;
        }

        String lastRemotePath = appPreferences.getUploadScansLastPath();

        //create the default scan folder if it doesn't exist or if user has not selected any other folder
        if (lastRemotePath.equalsIgnoreCase(ScanActivity.DEFAULT_UPLOAD_SCAN_PATH)) {
            getFileOperationsHelper().createFolderIfNotExist(lastRemotePath, false);
            return;
        }

        //if last saved remote path is not root path then we have to check if the folder exist or not
        if (!lastRemotePath.equals(OCFile.ROOT_PATH)) {
            getFileOperationsHelper().createFolderIfNotExist(lastRemotePath, true);
        }
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);
        if (operation instanceof CreateFolderIfNotExistOperation) {
            //we are only handling callback when we are checking if folder exist or not to update the UI
            //in case the folder doesn't exist (user has deleted)
            if (!result.isSuccess() && result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
                Fragment saveScanFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_SAVE_SCAN_TAG);
                if (saveScanFragment != null && saveScanFragment.isVisible()) {
                    //update the root path in preferences as well
                    //so that next time folder issue won't come
                    appPreferences.setUploadScansLastPath(OCFile.ROOT_PATH);
                    //if folder doesn't exist then we have to set the remote path as root i.e. fallback mechanism
                    ((SaveScannedDocumentFragment) saveScanFragment).setRemoteFilePath(OCFile.ROOT_PATH);
                }
            }
            isFolderCheckOperationFinished = true;
        }
    }
}