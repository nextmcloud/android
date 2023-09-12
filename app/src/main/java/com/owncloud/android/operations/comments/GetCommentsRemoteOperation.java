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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.xml.Namespace;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * class to fetch the list of comments for the given fileId
 * <p>
 * API : //PROPFIND to dav/comments/files/{file_id}
 */
public class GetCommentsRemoteOperation extends RemoteOperation {

    private static final String TAG = GetCommentsRemoteOperation.class.getSimpleName();

    private static final String EXTENDED_PROPERTY_ID = "id";
    protected static final String EXTENDED_PROPERTY_MESSAGE = "message";
    private static final String EXTENDED_PROPERTY_ACTOR_DISPLAY_NAME = "actorDisplayName";
    private static final String EXTENDED_PROPERTY_ACTOR_ID = "actorId";
    private static final String EXTENDED_PROPERTY_ACTOR_TYPE = "actorType";
    private static final String EXTENDED_PROPERTY_CREATION_DATE_TIME = "creationDateTime";
    private static final String EXTENDED_PROPERTY_IS_UNREAD = "isUnread";
    private static final String EXTENDED_PROPERTY_OBJECT_ID = "objectId";
    private static final String EXTENDED_PROPERTY_OBJECT_TYPE = "objectType";
    private static final String EXTENDED_PROPERTY_VERB = "verb";

    private static final int CODE_PROP_SUCCESS = 200;
    private static final int CODE_PROP_NOT_FOUND = 404;

    private final long fileId;
    private final int limit, offset;

    // TODO: 10/15/22 Add pagination
    public GetCommentsRemoteOperation(long fileId, int limit, int offset) {
        this.fileId = fileId;
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        PropFindMethod propFind = null;
        RemoteOperationResult result = null;
        try {
            propFind = new PropFindMethod(client.getCommentsUri(fileId));
            int status = client.executeMethod(propFind);

            if (status == HttpStatus.SC_MULTI_STATUS || status == HttpStatus.SC_OK) {
                MultiStatus dataInServer = propFind.getResponseBodyAsMultiStatus();

                result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.OK);
                result.setResultData(parseComments(dataInServer));

            }

            if (status == HttpStatus.SC_NOT_FOUND) {
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.FILE_NOT_FOUND);
            }

        } catch (Exception e) {
            Log_OC.e(TAG, "Error while retrieving comments");
            result = new RemoteOperationResult(e);
        } finally {
            if (propFind != null) {
                propFind.releaseConnection();
            }
        }

        return result;

    }

    private List<Comments> parseComments(MultiStatus dataInServer) {
        List<Comments> commentsList = new ArrayList<>();

        Namespace ocNamespace = Namespace.getNamespace(WebdavEntry.NAMESPACE_OC);

        for (MultiStatusResponse statusResponse : dataInServer.getResponses()) {

            int status = statusResponse.getStatus()[0].getStatusCode();
            if (status == CODE_PROP_NOT_FOUND) {
                status = statusResponse.getStatus()[1].getStatusCode();
            }

            if (status != CODE_PROP_SUCCESS) {
                continue;
            }

            DavPropertySet propSet = statusResponse.getProperties(status);

            if (propSet == null) {
                continue;
            }

            String path = statusResponse.getHref();

            // OC id property <oc:id>
            DavProperty<?> prop = propSet.get(EXTENDED_PROPERTY_ID, ocNamespace);
            int commentId = 0;
            if (prop != null) {
                String id = (String) prop.getValue();
                if (id != null) {
                    commentId = Integer.parseInt(id);
                }
            }

            //don't look for other elements if commentId is missing or zero
            if (commentId == 0) continue;

            // OC message property <oc:message>
            prop = propSet.get(EXTENDED_PROPERTY_MESSAGE, ocNamespace);
            String message = "";
            if (prop != null) {
                message = (String) prop.getValue();
            }

            // OC actorId property <oc:actorId>
            prop = propSet.get(EXTENDED_PROPERTY_ACTOR_ID, ocNamespace);
            String actorId = "";
            if (prop != null) {
                actorId = (String) prop.getValue();
            }

            // OC actorDisplayName property <oc:actorDisplayName>
            prop = propSet.get(EXTENDED_PROPERTY_ACTOR_DISPLAY_NAME, ocNamespace);
            String actorDisplayName = "";
            if (prop != null) {
                actorDisplayName = (String) prop.getValue();
            }

            // OC actorType property <oc:actorType>
            prop = propSet.get(EXTENDED_PROPERTY_ACTOR_TYPE, ocNamespace);
            String actorType = "";
            if (prop != null) {
                actorType = (String) prop.getValue();
            }

            // OC creationDateTime property <oc:creationDateTime>
            prop = propSet.get(EXTENDED_PROPERTY_CREATION_DATE_TIME, ocNamespace);
            Date creationDateTime = null;
            if (prop != null) {
                creationDateTime = WebdavUtils.parseResponseDate((String) prop.getValue());
            }

            // OC isUnread property <oc:isUnread>
            prop = propSet.get(EXTENDED_PROPERTY_IS_UNREAD, ocNamespace);
            boolean isUnread = false;
            if (prop != null) {
                String value = (String) prop.getValue();
                if (value != null) {
                    isUnread = Boolean.parseBoolean(value);
                }
            }

            // OC objectId property <oc:objectId>
            prop = propSet.get(EXTENDED_PROPERTY_OBJECT_ID, ocNamespace);
            String objectId = "";
            if (prop != null) {
                objectId = (String) prop.getValue();
            }

            // OC objectType property <oc:objectType>
            prop = propSet.get(EXTENDED_PROPERTY_OBJECT_TYPE, ocNamespace);
            String objectType = "";
            if (prop != null) {
                objectType = (String) prop.getValue();
            }

            // OC verb property <oc:verb>
            prop = propSet.get(EXTENDED_PROPERTY_VERB, ocNamespace);
            String verb = "";
            if (prop != null) {
                verb = (String) prop.getValue();
            }

            Comments comments = new Comments(path, commentId, message, actorId,
                                             actorDisplayName, actorType, creationDateTime,
                                             isUnread, objectId, objectType, verb);

            commentsList.add(comments);
        }

        return commentsList;
    }

}
