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
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.xml.Namespace;

import java.io.IOException;

/**
 * class to update the comment
 * <p>
 * API : //PROPPATCH to dav/comments/files/{file_id}/{comment_id}
 */
public class UpdateCommentRemoteOperation extends RemoteOperation {

    private static final String TAG = UpdateCommentRemoteOperation.class.getSimpleName();

    private final long fileId;
    private final int commentId;
    private final String message;

    public UpdateCommentRemoteOperation(long fileId, int commentId, String message) {
        this.fileId = fileId;
        this.commentId = commentId;
        this.message = message;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        PropPatchMethod propPatchMethod = null;

        DavPropertySet newProps = new DavPropertySet();
        DavPropertyNameSet removeProperties = new DavPropertyNameSet();

        DefaultDavProperty<String> messageDavProperty = new DefaultDavProperty<>(GetCommentsRemoteOperation.EXTENDED_PROPERTY_MESSAGE, message,
                                                                                 Namespace.getNamespace(WebdavEntry.NAMESPACE_OC));
        newProps.add(messageDavProperty);

        String commentsPath = client.getCommentsUri(fileId) + "/" + commentId;

        try {
            propPatchMethod = new PropPatchMethod(commentsPath, newProps, removeProperties);
            int status = client.executeMethod(propPatchMethod);

            if (isSuccess(status)) {
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

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_OK
            || status == HttpStatus.SC_NO_CONTENT
            || status == HttpStatus.SC_MULTI_STATUS;
    }

}
