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

import android.text.TextUtils
import com.nextcloud.common.SessionTimeOut
import com.nextcloud.common.defaultSessionTimeOut
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.network.WebdavEntry
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.httpclient.HttpStatus
import org.apache.jackrabbit.webdav.DavConstants
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod
import org.apache.jackrabbit.webdav.property.DavPropertyName
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet
import org.apache.jackrabbit.webdav.xml.Namespace

class ReadAlbumsRemoteOperation
@JvmOverloads
constructor(
    private val mAlbumRemotePath: String? = null,
    private val sessionTimeOut: SessionTimeOut = defaultSessionTimeOut
) : RemoteOperation<List<PhotoAlbumEntry>>() {
    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Deprecated("Deprecated in Java")
    @Suppress("TooGenericExceptionCaught")
    override fun run(client: OwnCloudClient): RemoteOperationResult<List<PhotoAlbumEntry>> {
        var propfind: PropFindMethod? = null
        var result: RemoteOperationResult<List<PhotoAlbumEntry>>
        var url = "${client.baseUri}/remote.php/dav/photos/${client.userId}/albums"
        if (!TextUtils.isEmpty(mAlbumRemotePath)) {
            url += WebdavUtils.encodePath(mAlbumRemotePath)
        }
        try {
            propfind = PropFindMethod(url, getAlbumPropSet(), DavConstants.DEPTH_1)
            val status =
                client.executeMethod(
                    propfind,
                    sessionTimeOut.readTimeOut,
                    sessionTimeOut.connectionTimeOut
                )
            val isSuccess = status == HttpStatus.SC_MULTI_STATUS || status == HttpStatus.SC_OK
            if (isSuccess) {
                val albumsList =
                    propfind.responseBodyAsMultiStatus.responses
                        .filter { it.status[0].statusCode == HttpStatus.SC_OK }
                        .map { res -> PhotoAlbumEntry(res) }
                result = RemoteOperationResult<List<PhotoAlbumEntry>>(true, propfind)
                result.resultData = albumsList
            } else {
                result = RemoteOperationResult<List<PhotoAlbumEntry>>(false, propfind)
                client.exhaustResponse(propfind.responseBodyAsStream)
            }
        } catch (e: Exception) {
            result = RemoteOperationResult<List<PhotoAlbumEntry>>(e)
            Log_OC.e(TAG, "Read album failed: ${result.logMessage}", result.exception)
        } finally {
            propfind?.releaseConnection()
        }

        return result
    }

    companion object {
        private val TAG: String = ReadAlbumsRemoteOperation::class.java.simpleName
        private const val PROPERTY_LAST_PHOTO = "last-photo"
        private const val PROPERTY_NB_ITEMS = "nbItems"
        private const val PROPERTY_LOCATION = "location"
        private const val PROPERTY_DATE_RANGE = "dateRange"
        private const val PROPERTY_COLLABORATORS = "collaborators"

        private fun getAlbumPropSet(): DavPropertyNameSet {
            val propertySet = DavPropertyNameSet()
            val ncNamespace: Namespace = Namespace.getNamespace("nc", WebdavEntry.NAMESPACE_NC)

            propertySet.add(DavPropertyName.create(PROPERTY_LAST_PHOTO, ncNamespace))
            propertySet.add(DavPropertyName.create(PROPERTY_NB_ITEMS, ncNamespace))
            propertySet.add(DavPropertyName.create(PROPERTY_LOCATION, ncNamespace))
            propertySet.add(DavPropertyName.create(PROPERTY_DATE_RANGE, ncNamespace))
            propertySet.add(DavPropertyName.create(PROPERTY_COLLABORATORS, ncNamespace))

            return propertySet
        }
    }
}