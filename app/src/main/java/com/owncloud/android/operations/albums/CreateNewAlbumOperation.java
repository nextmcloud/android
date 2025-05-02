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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

public class CreateNewAlbumOperation extends RemoteOperation<Void> {
    private static final String TAG = CreateNewAlbumOperation.class.getSimpleName();
    private final String newAlbumName;

    public CreateNewAlbumOperation(String newAlbumName) {
        Log_OC.e(TAG, "Fetch albums remote operation");
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
    protected RemoteOperationResult<Void> run(OwnCloudClient client) {
        MkColMethod mkCol = null;
        RemoteOperationResult<Void> result;
        try {
            mkCol = new MkColMethod("https://pre1.next.magentacloud.de/remote.php/dav/photos/" + client.getUserId() + "/albums" + WebdavUtils.encodePath(newAlbumName));
            client.executeMethod(mkCol);
            if (405 == mkCol.getStatusCode()) {
                result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS);
            } else {
                result = new RemoteOperationResult<>(mkCol.succeeded(), mkCol);
                result.setResultData(null);
            }

            Log_OC.d(TAG, "Create album " + newAlbumName + ": " + result.getLogMessage());
            client.exhaustResponse(mkCol.getResponseBodyAsStream());
        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Create album " + newAlbumName + ": " + result.getLogMessage(), e);
        } finally {
            if (mkCol != null) {
                mkCol.releaseConnection();
            }

        }

        return result;
    }

}
