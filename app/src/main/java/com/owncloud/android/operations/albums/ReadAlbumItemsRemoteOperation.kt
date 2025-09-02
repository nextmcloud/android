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
package com.owncloud.android.operations.albums

import androidx.core.net.toUri
import com.nextcloud.common.SessionTimeOut
import com.nextcloud.common.defaultSessionTimeOut
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.network.WebdavEntry
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.RemoteFile
import org.apache.commons.httpclient.HttpStatus
import org.apache.jackrabbit.webdav.DavConstants
import org.apache.jackrabbit.webdav.MultiStatus
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod

class ReadAlbumItemsRemoteOperation
@JvmOverloads
constructor(
    private val mRemotePath: String,
    private val sessionTimeOut: SessionTimeOut = defaultSessionTimeOut
) : RemoteOperation<List<RemoteFile>>() {
    @Deprecated("Deprecated in Java")
    @Suppress("TooGenericExceptionCaught")
    override fun run(client: OwnCloudClient): RemoteOperationResult<List<RemoteFile>> {
        var result: RemoteOperationResult<List<RemoteFile>>? = null
        var query: PropFindMethod? = null
        val url = "${client.baseUri}/remote.php/dav/photos/${client.userId}/albums${
            WebdavUtils.encodePath(
                mRemotePath
            )
        }"
        try {
            // remote request
            query =
                PropFindMethod(
                    url,
                    WebdavUtils.getAllPropSet(), // PropFind Properties
                    DavConstants.DEPTH_1
                )
            val status =
                client.executeMethod(
                    query,
                    sessionTimeOut.readTimeOut,
                    sessionTimeOut.connectionTimeOut
                )

            // check and process response
            val isSuccess = (status == HttpStatus.SC_MULTI_STATUS || status == HttpStatus.SC_OK)

            result =
                if (isSuccess) {
                    // get data from remote folder
                    val dataInServer = query.responseBodyAsMultiStatus
                    val mFolderAndFiles = readAlbumData(dataInServer, client)

                    // Result of the operation
                    RemoteOperationResult<List<RemoteFile>>(true, query).apply {
                        // Add data to the result
                        resultData = mFolderAndFiles
                    }
                } else {
                    // synchronization failed
                    client.exhaustResponse(query.responseBodyAsStream)
                    RemoteOperationResult(false, query)
                }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
        } finally {
            query?.releaseConnection()

            result = result ?: RemoteOperationResult<List<RemoteFile>>(Exception("unknown error")).also {
                Log_OC.e(TAG, "Synchronized $mRemotePath: failed")
            }
            if (result.isSuccess) {
                Log_OC.i(TAG, "Synchronized $mRemotePath : ${result.logMessage}")
            } else if (result.isException) {
                Log_OC.e(TAG, "Synchronized $mRemotePath : ${result.logMessage}", result.exception)
            } else {
                Log_OC.e(TAG, "Synchronized $mRemotePath : ${result.logMessage}")
            }
        }

        return result
    }

    companion object {
        private val TAG: String = ReadAlbumItemsRemoteOperation::class.java.simpleName

        private fun readAlbumData(remoteData: MultiStatus, client: OwnCloudClient): List<RemoteFile> {
            val baseUrl = "${client.baseUri}/remote.php/dav/photos/${client.userId}"
            val encodedPath = baseUrl.toUri().encodedPath ?: return emptyList()

            val files = mutableListOf<RemoteFile>()
            val responses = remoteData.responses

            // reading from 1 as 0th item will be just the root album path
            for (i in 1..<responses.size) {
                val entry = WebdavEntry(responses[i], encodedPath)
                val remoteFile = RemoteFile(entry)
                files.add(remoteFile)
            }

            return files
        }
    }
}