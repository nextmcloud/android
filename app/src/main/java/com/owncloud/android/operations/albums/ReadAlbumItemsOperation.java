/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations.albums;

import android.net.Uri;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavEntry;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.utils.FileStorageUtils;

import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import java.util.ArrayList;
import java.util.List;

public class ReadAlbumItemsOperation extends RemoteOperation<List<OCFile>> {

    private static final String TAG = ReadAlbumItemsOperation.class.getSimpleName();
    private final String albumPath;
    private ArrayList<OCFile> mFolderAndFiles;
    private final FileDataStorageManager fileDataStorageManager;

    public ReadAlbumItemsOperation(String albumPath, FileDataStorageManager fileDataStorageManager) {
        this.fileDataStorageManager = fileDataStorageManager;
        Log_OC.e(TAG, "Reading Album Operations");
        this.albumPath = albumPath;
    }

    protected RemoteOperationResult<List<OCFile>> run(OwnCloudClient client) {
        Log_OC.e(TAG, "Reading Album Operations running");
        RemoteOperationResult<List<OCFile>> result = null;
        PropFindMethod query = null;
        String url = client.getBaseUri() + "/remote.php/dav/photos/" + client.getUserId() + "/albums" + WebdavUtils.encodePath(albumPath);
        try {
            query = new PropFindMethod(url, WebdavUtils.getAllPropSet(), 1);
            int status = client.executeMethod(query);
            Log_OC.e(TAG, "Status: " + status);
            boolean isSuccess = status == 207 || status == 200;
            if (isSuccess) {
                MultiStatus dataInServer = query.getResponseBodyAsMultiStatus();
                this.readData(dataInServer, client);
                result = new RemoteOperationResult<>(true, query);
                if (result.isSuccess()) {
                    result.setResultData(this.mFolderAndFiles);
                }
            } else {
                client.exhaustResponse(query.getResponseBodyAsStream());
                result = new RemoteOperationResult<>(false, query);
            }
        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
        } finally {
            if (query != null) {
                query.releaseConnection();
            }

            if (result == null) {
                result = new RemoteOperationResult<>(new Exception("unknown error"));
                Log_OC.e(TAG, "Synchronized " + this.albumPath + ": failed");
            } else if (result.isSuccess()) {
                Log_OC.i(TAG, "Synchronized " + this.albumPath + ": " + result.getLogMessage());
            } else if (result.isException()) {
                Log_OC.e(TAG, "Synchronized " + this.albumPath + ": " + result.getLogMessage(), result.getException());
            } else {
                Log_OC.e(TAG, "Synchronized " + this.albumPath + ": " + result.getLogMessage());
            }

        }

        return result;
    }

    private void readData(MultiStatus remoteData, OwnCloudClient client) {
        String url = client.getBaseUri() + "/remote.php/dav/photos/" + client.getUserId();
        this.mFolderAndFiles = new ArrayList<>();

        // reading from 1 as 0th item will be just the root album path
        for (int i = 1; i < remoteData.getResponses().length; ++i) {
            WebdavEntry we = new WebdavEntry(remoteData.getResponses()[i], Uri.parse(url).getEncodedPath());
            RemoteFile remoteFile = new RemoteFile(we);
            OCFile ocFile = fileDataStorageManager.getFileByLocalId(remoteFile.getLocalId());
            if (ocFile == null) {
                ocFile = FileStorageUtils.fillOCFile(remoteFile);
            } else{
                // required: as OCFile will only contains file_name.png not with /albums/album_name/file_name
                // to fix this we have to get the remote path from remote file and assign to OCFile
                ocFile.setRemotePath(remoteFile.getRemotePath());
                ocFile.setDecryptedRemotePath(remoteFile.getRemotePath());
            }
            this.mFolderAndFiles.add(ocFile);
        }

    }

}
