/**
 * ownCloud Android client application
 *
 * @author TSI-mc Copyright (C) 2021 TSI-mc
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations.comments;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavEntry;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.share_download_limit.DownloadLimitXMLParser;
import com.owncloud.android.operations.share_download_limit.ShareDownloadLimitUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class GetCommentsRemoteOperation extends RemoteOperation {

    private static final String TAG = GetCommentsRemoteOperation.class.getSimpleName();

    public static final String EXTENDED_PROPERTY_ACTOR_DISPLAY_NAME = "actorDisplayName";
    private static final int CODE_PROP_NOT_FOUND = 404;

    private final String fileId;
    private final int limit, offset;

    public GetCommentsRemoteOperation(String fileId, int limit, int offset) {
        this.fileId = fileId;
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        PropFindMethod propfind = null;

        try {
            propfind = new PropFindMethod(client.getCommentsUri(fileId));
            int status = client.executeMethod(propfind);

            if (status == HttpStatus.SC_MULTI_STATUS || status == HttpStatus.SC_OK) {
                MultiStatus dataInServer = propfind.getResponseBodyAsMultiStatus();
                Namespace ocNamespace = Namespace.getNamespace(WebdavEntry.NAMESPACE_OC);

                for (MultiStatusResponse resp : dataInServer.getResponses()) {

                    int status1 = resp.getStatus()[0].getStatusCode();
                    if (status1 == CODE_PROP_NOT_FOUND) {
                        status = resp.getStatus()[1].getStatusCode();
                    }
                    DavPropertySet propSet = resp.getProperties(status1);

                    DavProperty<?> prop = propSet.get(EXTENDED_PROPERTY_ACTOR_DISPLAY_NAME, ocNamespace);
                    if (prop != null) {
                        String ownerDisplayName = (String) prop.getValue();
                        Log_OC.e(TAG, "Response : " + ownerDisplayName);

                    } else {
                        String ownerDisplayName = "";
                    }
                }

            }

            if (status == HttpStatus.SC_NOT_FOUND) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.FILE_NOT_FOUND);
            }

        } catch (Exception e) {
            Log_OC.e(TAG, "Error while retrieving eTag");
        } finally {
            if (propfind != null) {
                propfind.releaseConnection();
            }
        }

        // TODO: 10/13/22 update return
        return new RemoteOperationResult(RemoteOperationResult.ResultCode.ETAG_CHANGED);
    }

}
