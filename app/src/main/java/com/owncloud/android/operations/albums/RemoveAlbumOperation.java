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

import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

public class RemoveAlbumOperation extends SyncOperation {
    private static final String TAG = RemoveAlbumOperation.class.getSimpleName();
    private final String albumName;

    public RemoveAlbumOperation(String albumName, FileDataStorageManager storageManager) {
        super(storageManager);
        Log_OC.e(TAG, "Fetch albums remote operation");
        this.albumName = albumName;
    }

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        DeleteMethod delete = null;

        try {
            delete = new DeleteMethod(client.getBaseUri()  + "/remote.php/dav/photos/" + client.getUserId() + "/albums" + WebdavUtils.encodePath(albumName));
            int status = client.executeMethod(delete);
            delete.getResponseBodyAsString();
            result = new RemoteOperationResult(delete.succeeded() || status == 404, delete);
            Log_OC.i(TAG, "Remove " + this.albumName + ": " + result.getLogMessage());
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Remove " + this.albumName + ": " + result.getLogMessage(), e);
        } finally {
            if (delete != null) {
                delete.releaseConnection();
            }

        }

        return result;
    }

}
