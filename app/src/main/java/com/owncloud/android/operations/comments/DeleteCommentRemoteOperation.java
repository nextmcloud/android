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
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;

/**
 * class to delete the comment
 * <p>
 * API : //DELETE to dav/comments/files/{file_id}/{comment_id}
 */
public class DeleteCommentRemoteOperation extends RemoteOperation {

    private static final String TAG = DeleteCommentRemoteOperation.class.getSimpleName();

    private final long fileId;
    private final int commentId;

    public DeleteCommentRemoteOperation(long fileId, int commentId) {
        this.fileId = fileId;
        this.commentId = commentId;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        int status;

        DeleteMethod deleteMethod = null;

        try {
            //Delete Method
            deleteMethod = new DeleteMethod(client.getCommentsUri(fileId) + "/" + commentId);

            status = client.executeMethod(deleteMethod);

            if (isSuccess(status)) {
                result = new RemoteOperationResult<>(true, status, deleteMethod.getResponseHeaders());
                return result;
            } else {
                result = new RemoteOperationResult<>(false, deleteMethod);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Exception while deleting comment", e);

        } finally {
            if (deleteMethod != null) {
                deleteMethod.releaseConnection();
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
