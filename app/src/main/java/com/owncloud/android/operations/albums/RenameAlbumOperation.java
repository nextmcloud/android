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

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.common.SyncOperation;

import org.apache.jackrabbit.webdav.client.methods.MoveMethod;

public class RenameAlbumOperation extends SyncOperation {
    private static final String TAG = RenameAlbumOperation.class.getSimpleName();
    private final String oldAlbumName;
    private final String newAlbumName;

    public RenameAlbumOperation(String oldAlbumName, String newAlbumName, FileDataStorageManager storageManager) {
        super(storageManager);
        Log_OC.e(TAG, "Fetch albums remote operation");
        this.oldAlbumName = oldAlbumName;
        this.newAlbumName = newAlbumName;
    }

    public String getNewAlbumName() {
        return newAlbumName;
    }

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        MoveMethod move = null;
        String url = "https://pre1.next.magentacloud.de/remote.php/dav/photos/" + client.getUserId() + "/albums";
        try {
            if (!this.newAlbumName.equals(this.oldAlbumName)) {
                move = new MoveMethod(url + WebdavUtils.encodePath(oldAlbumName), url + WebdavUtils.encodePath(newAlbumName), true);
                client.executeMethod(move);
                result = new RemoteOperationResult(move.succeeded(), move);
                Log_OC.i(TAG, "Rename " + this.oldAlbumName + " to " + this.newAlbumName + ": " + result.getLogMessage());
                client.exhaustResponse(move.getResponseBodyAsStream());
                return result;
            }
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Rename " + this.oldAlbumName + " to " + this.newAlbumName + ": " + result.getLogMessage(), e);
            return result;
        } finally {
            if (move != null) {
                move.releaseConnection();
            }
        }

        return result;
    }

}
