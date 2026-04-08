/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.albums

import com.owncloud.android.lib.common.network.WebdavEntry.Companion.NAMESPACE_NC
import com.owncloud.android.lib.common.network.WebdavEntry.Companion.SHAREES_ID
import com.owncloud.android.lib.common.network.WebdavEntry.Companion.SHAREES_SHARE_TYPE
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.utils.DisplayUtils
import org.apache.commons.httpclient.HttpStatus
import org.apache.jackrabbit.webdav.MultiStatusResponse
import org.apache.jackrabbit.webdav.property.DavPropertyName
import org.apache.jackrabbit.webdav.property.DavPropertySet
import org.apache.jackrabbit.webdav.xml.Namespace
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Element
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class PhotoAlbumEntry(
    // required for providing album share link url
    baseUri: String,
    response: MultiStatusResponse
) {
    val href: String
    val lastPhoto: Long
    val nbItems: Int
    val location: String?
    private val dateRange: String?
    var collaborators = arrayOf<Collaborators>()
        private set

    private var shareBaseUri: String? = null

    companion object {
        private const val DATE_PATTERN = "MMM yyyy"
        private const val MILLIS = 1000L
        private const val PROPERTY_LAST_PHOTO = "last-photo"
        private const val PROPERTY_NB_ITEMS = "nbItems"
        private const val PROPERTY_LOCATION = "location"
        private const val PROPERTY_DATE_RANGE = "dateRange"
        private const val PROPERTY_COLLABORATORS = "collaborators"
        const val COLLABORATOR_LABEL = "label"
    }

    init {

        // will be used to provide full share link for album
        shareBaseUri = "$baseUri/apps/photos/public/"

        href = response.href

        val properties = response.getProperties(HttpStatus.SC_OK)

        this.lastPhoto = parseLong(parseString(properties, PROPERTY_LAST_PHOTO))
        this.nbItems = parseInt(parseString(properties, PROPERTY_NB_ITEMS))
        this.location = parseString(properties, PROPERTY_LOCATION)
        this.dateRange = parseString(properties, PROPERTY_DATE_RANGE)
        parseCollaborators(properties)
    }

    private fun parseString(
        props: DavPropertySet,
        name: String
    ): String? {
        val propName = DavPropertyName.create(name, Namespace.getNamespace("nc", NAMESPACE_NC))
        val prop = props[propName]
        return if (prop != null && prop.value != null) prop.value.toString() else null
    }

    private fun parseInt(value: String?): Int =
        try {
            value?.toInt() ?: 0
        } catch (_: NumberFormatException) {
            0
        }

    private fun parseLong(value: String?): Long =
        try {
            value?.toLong() ?: 0L
        } catch (_: NumberFormatException) {
            0L
        }

    private fun parseCollaborators(properties: DavPropertySet) {
        val collaboratorsProp = properties[PROPERTY_COLLABORATORS, Namespace.getNamespace("nc", NAMESPACE_NC)]
        if (collaboratorsProp != null && collaboratorsProp.value != null) {
            if (collaboratorsProp.value is ArrayList<*>) {
                val list = collaboratorsProp.value as ArrayList<*>
                val tempList: MutableList<Collaborators> = ArrayList()
                for (i in list.indices) {
                    val element = list[i] as Element
                    val collaborator = createCollaborators(element)
                    tempList.add(collaborator)
                }
                collaborators = tempList.toTypedArray()
            } else {
                // single item or empty
                val element = collaboratorsProp.value as Element
                val collaborator = createCollaborators(element)
                collaborators = arrayOf(collaborator)
            }
        }
    }

    private fun createCollaborators(element: Element): Collaborators {
        val id = extractId(element)
        val label = extractLabel(element)
        val shareType = extractShareType(element)
        return Collaborators(id, label, shareType, "$shareBaseUri$id")
    }

    private fun extractLabel(element: Element): String {
        val displayName = element.getElementsByTagName(COLLABORATOR_LABEL).item(0)
        return if (displayName != null && displayName.firstChild != null) {
            displayName.firstChild.nodeValue
        } else {
            ""
        }
    }

    private fun extractId(element: Element): String {
        val userId = element.getElementsByTagName(SHAREES_ID).item(0)
        return if (userId != null && userId.firstChild != null) {
            userId.firstChild.nodeValue
        } else {
            ""
        }
    }

    private fun extractShareType(element: Element): ShareType {
        val shareType = element.getElementsByTagName(SHAREES_SHARE_TYPE).item(0)
        if (shareType != null && shareType.firstChild != null) {
            val value = shareType.firstChild.nodeValue.toInt()
            return ShareType.fromValue(value)
        }
        return ShareType.NO_SHARED
    }

    val albumName: String
        get() {
            // NMC-4610 fix
            // use decoder to show correct path
            return URLDecoder.decode(
                href
                    .removeSuffix("/")
                    .substringAfterLast("/")
                    .takeIf { it.isNotEmpty() } ?: "", StandardCharsets.UTF_8.name())
        }

    val createdDate: String
        get() {
            val defaultDate = DisplayUtils.getDateByPattern(System.currentTimeMillis(), DATE_PATTERN)

            return try {
                val obj = JSONObject(dateRange ?: return defaultDate)
                val startTimestamp = obj.optLong("start", 0)
                if (startTimestamp > 0) {
                    DisplayUtils.getDateByPattern(startTimestamp * MILLIS, DATE_PATTERN)
                } else {
                    defaultDate
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                defaultDate
            }
        }
}

data class Collaborators(val id: String?, val label: String?, val type: ShareType?, val shareLink: String?)