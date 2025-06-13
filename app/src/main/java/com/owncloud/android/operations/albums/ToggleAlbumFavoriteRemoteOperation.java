/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.albums;

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.xml.Namespace;

import java.io.IOException;

public class ToggleAlbumFavoriteRemoteOperation extends RemoteOperation {
    private boolean makeItFavorited;
    private String filePath;

    public ToggleAlbumFavoriteRemoteOperation(boolean makeItFavorited, String filePath) {
        this.makeItFavorited = makeItFavorited;
        this.filePath = filePath;
    }

    protected RemoteOperationResult run(OwnCloudClient client) {
        Log_OC.e("Toogle Album Fav", "File: "+filePath +" -- isFav: "+makeItFavorited);
        RemoteOperationResult result;
        PropPatchMethod propPatchMethod = null;
        DavPropertySet newProps = new DavPropertySet();
        DavPropertyNameSet removeProperties = new DavPropertyNameSet();
        if (this.makeItFavorited) {
            DefaultDavProperty<String> favoriteProperty = new DefaultDavProperty("oc:favorite", "1", Namespace.getNamespace("http://owncloud.org/ns"));
            newProps.add(favoriteProperty);
        } else {
            removeProperties.add("oc:favorite", Namespace.getNamespace("http://owncloud.org/ns"));
        }

        String webDavUrl = client.getDavUri().toString()+"/photos/";
        String encodedPath = (client.getUserId() + Uri.encode(this.filePath)).replace("%2F", "/");
        String fullFilePath = webDavUrl + encodedPath;

        try {
            propPatchMethod = new PropPatchMethod(fullFilePath, newProps, removeProperties);
            int status = client.executeMethod(propPatchMethod);
            boolean isSuccess = status == 207 || status == 200;
            if (isSuccess) {
                result = new RemoteOperationResult(true, status, propPatchMethod.getResponseHeaders());
            } else {
                client.exhaustResponse(propPatchMethod.getResponseBodyAsStream());
                result = new RemoteOperationResult(false, status, propPatchMethod.getResponseHeaders());
            }
        } catch (IOException e) {
            result = new RemoteOperationResult(e);
        } finally {
            if (propPatchMethod != null) {
                propPatchMethod.releaseConnection();
            }

        }

        return result;
    }
}