/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2012-2014 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 Jorge Antonio Diaz-Benito Soriano <jorge.diazbenitosoriano@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations.albums;

import android.util.Log;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.operations.common.SyncOperation;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.Status;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;

import java.io.IOException;

/**
 * Operation copying an {@link OCFile} to a different folder.
 *
 * @author David A. Velasco
 */
public class CopyFileToAlbumOperation extends SyncOperation {
    private static final String TAG = CopyFileToAlbumOperation.class.getSimpleName();

    private final String srcPath;
    private String targetParentPath;

    /**
     * Constructor
     *
     * @param srcPath          Remote path of the {@link OCFile} to move.
     * @param targetParentPath Path to the folder where the file will be copied into.
     */
    public CopyFileToAlbumOperation(String srcPath, String targetParentPath, FileDataStorageManager storageManager) {
        super(storageManager);

        this.srcPath = srcPath;
        this.targetParentPath = targetParentPath;
        if (!this.targetParentPath.endsWith(OCFile.PATH_SEPARATOR)) {
            this.targetParentPath += OCFile.PATH_SEPARATOR;
        }
    }

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        /// 1. check copy validity
        if (targetParentPath.startsWith(srcPath)) {
            return new RemoteOperationResult(ResultCode.INVALID_COPY_INTO_DESCENDANT);
        }
        OCFile file = getStorageManager().getFileByPath(srcPath);
        if (file == null) {
            return new RemoteOperationResult(ResultCode.FILE_NOT_FOUND);
        }

        /// 2. remote copy
        String targetPath = targetParentPath + file.getFileName();
        if (file.isFolder()) {
            targetPath += OCFile.PATH_SEPARATOR;
        }

        // auto rename, to allow copy
        if (targetPath.equals(srcPath)) {
            if (file.isFolder()) {
                targetPath = targetParentPath + file.getFileName();
            }
            targetPath = UploadFileOperation.getNewAvailableRemotePath(client, targetPath, null, false);

            if (file.isFolder()) {
                targetPath += OCFile.PATH_SEPARATOR;
            }
        }

        RemoteOperationResult result = performCopyOperation(targetPath, client);

        /// 3. local copy
        if (result.isSuccess()) {
            getStorageManager().copyLocalFile(file, targetPath);
        }
        // TODO handle ResultCode.PARTIAL_COPY_DONE in client Activity, for the moment

        return result;
    }

    private RemoteOperationResult performCopyOperation(String targetRemotePath, OwnCloudClient client) {
        if (targetRemotePath.equals(this.srcPath)) {
            return new RemoteOperationResult(ResultCode.OK);
        } else if (targetRemotePath.startsWith(this.srcPath)) {
            return new RemoteOperationResult(ResultCode.INVALID_COPY_INTO_DESCENDANT);
        } else {
            CopyMethod copyMethod = null;
            RemoteOperationResult result;

            try {
                copyMethod = new CopyMethod(client.getFilesDavUri(this.srcPath), "https://pre1.next.magentacloud.de/remote.php/dav/photos/" + client.getUserId() + "/albums" + WebdavUtils.encodePath(targetRemotePath), false);
                int status = client.executeMethod(copyMethod);
                if (status == 207) {
                    result = this.processPartialError(copyMethod);
                } else if (status == 412) {
                    result = new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
                    client.exhaustResponse(copyMethod.getResponseBodyAsStream());
                } else {
                    result = new RemoteOperationResult(this.isSuccess(status), copyMethod);
                    client.exhaustResponse(copyMethod.getResponseBodyAsStream());
                }

                Log.i(TAG, "Copy " + this.srcPath + " to " + targetRemotePath + ": " + result.getLogMessage());
            } catch (Exception e) {
                result = new RemoteOperationResult(e);
                Log.e(TAG, "Copy " + this.srcPath + " to " + targetRemotePath + ": " + result.getLogMessage(), e);
            } finally {
                if (copyMethod != null) {
                    copyMethod.releaseConnection();
                }

            }

            return result;
        }
    }

    private RemoteOperationResult processPartialError(CopyMethod copyMethod) throws IOException, DavException {
        MultiStatusResponse[] responses = copyMethod.getResponseBodyAsMultiStatus().getResponses();
        boolean failFound = false;

        for (int i = 0; i < responses.length && !failFound; ++i) {
            Status[] status = responses[i].getStatus();
            failFound = status != null && status.length > 0 && status[0].getStatusCode() > 299;
        }

        RemoteOperationResult result;
        if (failFound) {
            result = new RemoteOperationResult(ResultCode.PARTIAL_COPY_DONE);
        } else {
            result = new RemoteOperationResult(true, copyMethod);
        }

        return result;
    }

    protected boolean isSuccess(int status) {
        return status == 201 || status == 204;
    }
}
