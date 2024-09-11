package com.nmc.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.helpers.FileOperationsHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// TODO: 06/24/23 Migrate to FileUtil once Rotate PR is upstreamed and merged by NC
public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    private static final String SCANNED_FILE_PREFIX = "scan_";
    private static final int JPG_FILE_TYPE = 1;
    private static final int PNG_FILE_TYPE = 2;

    public static File saveJpgImage(Context context, Bitmap bitmap, String imageName, int quality) {
        return createFileAndSaveImage(context, bitmap, imageName, quality, JPG_FILE_TYPE);
    }

    public static File savePngImage(Context context, Bitmap bitmap, String imageName, int quality) {
        return createFileAndSaveImage(context, bitmap, imageName, quality, PNG_FILE_TYPE);
    }

    private static File createFileAndSaveImage(Context context, Bitmap bitmap, String imageName, int quality,
                                               int fileType) {
        File file = fileType == PNG_FILE_TYPE ? getPngImageName(context, imageName) : getJpgImageName(context,
                                                                                                      imageName);
        return saveImage(file, bitmap, quality, fileType);
    }

    private static File saveImage(File file, Bitmap bitmap, int quality, int fileType) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
            byte[] bitmapData = bos.toByteArray();

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bitmapData);
            fileOutputStream.flush();
            fileOutputStream.close();
            return file;
        } catch (Exception e) {
            Log_OC.e(TAG, " Failed to save image : " + e.getLocalizedMessage());
            return null;
        }
    }

    private static File getJpgImageName(Context context, String imageName) {
        File imageFile = getOutputMediaFile(context);
        if (!TextUtils.isEmpty(imageName)) {
            return new File(imageFile.getPath() + File.separator + imageName + ".jpg");
        } else {
            return new File(imageFile.getPath() + File.separator + "IMG_" + FileOperationsHelper.getCapturedImageName());
        }
    }

    private static File getPngImageName(Context context, String imageName) {
        File imageFile = getOutputMediaFile(context);
        if (!TextUtils.isEmpty(imageName)) {
            return new File(imageFile.getPath() + File.separator + imageName + ".png");
        } else {
            return new File(imageFile.getPath() + File.separator + "IMG_" + FileOperationsHelper.getCapturedImageName().replace(".jpg", ".png"));
        }
    }

    private static File getTextFileName(Context context, String fileName) {
        File txtFileName = getOutputMediaFile(context);
        if (!TextUtils.isEmpty(fileName)) {
            return new File(txtFileName.getPath() + File.separator + fileName + ".txt");
        } else {
            return new File(txtFileName.getPath() + File.separator + FileOperationsHelper.getCapturedImageName().replace(".jpg", ".txt"));
        }
    }

    private static File getPdfFileName(Context context, String fileName) {
        File pdfFileName = getOutputMediaFile(context);
        if (!TextUtils.isEmpty(fileName)) {
            return new File(pdfFileName.getPath() + File.separator + fileName + ".pdf");
        } else {
            return new File(pdfFileName.getPath() + File.separator + FileOperationsHelper.getCapturedImageName().replace(".pdf", ".txt"));
        }
    }

    public static String scannedFileName() {
        return SCANNED_FILE_PREFIX + new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(new Date());
    }

    public static File getOutputMediaFile(Context context) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    public static Bitmap convertFileToBitmap(File file) {
        String filePath = file.getPath();
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        return bitmap;
    }

    public static File writeTextToFile(Context context, String textToWrite, String fileName) {
        File file = getTextFileName(context, fileName);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(textToWrite);
            fileWriter.flush();
            fileWriter.close();
            return file;
        } catch (IOException e) {
            //e.printStackTrace();
            Log_OC.e(TAG, "Failed to write file : " + e.toString());
        }
        return null;

    }


    /**
     * delete all the files inside the pictures directory
     * this directory is getting used to store the scanned images temporarily till they uploaded to cloud
     * the scanned files after downloading will get deleted by UploadWorker but in case some files still there
     * then we have to delete it when user do logout from the app
     * @param context
     */
    public static void deleteFilesFromPicturesDirectory(Context context) {
        File getFileDirectory = getOutputMediaFile(context);
        if (getFileDirectory.isDirectory()) {
            File[] fileList = getFileDirectory.listFiles();
            if (fileList != null && fileList.length > 0) {
                for (File file : fileList) {
                    file.delete();
                }
            }
        }
    }

}