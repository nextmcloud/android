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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavEntry;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.RemoteFile;

import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.xml.Namespace;

public class ReadAlbumsOperation extends RemoteOperation {



    public ReadAlbumsOperation() {
        Log_OC.e("ReadAlbumsOperation", "Fetch albums remote operation");

    }

    /**
     * Performs the operation.
     *
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        Log_OC.e("ReadAlbumsOperation", "Fetch albums remote operation running");
        PropFindMethod propfind = null;
        RemoteOperationResult result = null;

        try {
            propfind = new PropFindMethod("https://pre1.next.magentacloud.de/remote.php/dav/photos/120049010000000202817426/albums/", getProp(), 1);
            int status = client.executeMethod(propfind);
            Log_OC.e("ReadAlbumsOperation", "Fetch albums remote: "+status);
            boolean isSuccess = status == 207 || status == 200;
            if (isSuccess) {
                MultiStatus resp = propfind.getResponseBodyAsMultiStatus();
                Log_OC.e("Albums","Response: "+resp.getResponses()[0]);
                WebdavEntry we = new WebdavEntry(resp.getResponses()[0], Uri.parse("https://pre1.next.magentacloud.de/remote.php/dav/photos/120049010000000202817426/albums/").getEncodedPath());

                Log_OC.e("Albums","WebDavEntry: "+ we);

                RemoteFile remoteFile = new RemoteFile(we);

                Log_OC.e("Albums","remoteFile: "+ remoteFile);
               /* ArrayList<Object> files = new ArrayList();
                files.add(remoteFile);*/
                result = new RemoteOperationResult(true, propfind);
                Log_OC.e("Albums","Result: "+result);

                //  result.setData(files);
            } else {
                Log_OC.e("ReadAlbumsOperation", "Fetch albums remote else: "+propfind.getResponseBodyAsStream());
                result = new RemoteOperationResult(false, propfind);
                client.exhaustResponse(propfind.getResponseBodyAsStream());
            }
        } catch (Exception var13) {
            Exception e = var13;
            result = new RemoteOperationResult(e);
            Log_OC.e("ReadAlbumsOPeration", "Read file "  + " failed: " + result.getLogMessage(), result.getException());
        } finally {
            if (propfind != null) {
                propfind.releaseConnection();
            }

        }

        return result;
    }

    public static DavPropertyNameSet getProp() {
        DavPropertyNameSet propertySet = new DavPropertyNameSet();
        Namespace ncNamespace = Namespace.getNamespace("nc", "http://nextcloud.org/ns");

        // Add requested properties
        propertySet.add(DavPropertyName.create("last-photo", ncNamespace));
        propertySet.add(DavPropertyName.create("nbItems", ncNamespace));
        propertySet.add(DavPropertyName.create("location", ncNamespace));
        propertySet.add(DavPropertyName.create("dateRange", ncNamespace));
        propertySet.add(DavPropertyName.create("collaborators", ncNamespace));

        return propertySet;
    }
}
