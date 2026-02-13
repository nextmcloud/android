/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.albums

import com.owncloud.android.lib.common.network.WebdavEntry
import com.owncloud.android.utils.DisplayUtils
import org.apache.commons.httpclient.HttpStatus
import org.apache.jackrabbit.webdav.MultiStatusResponse
import org.apache.jackrabbit.webdav.property.DavPropertyName
import org.apache.jackrabbit.webdav.property.DavPropertySet
import org.apache.jackrabbit.webdav.xml.Namespace
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class PhotoAlbumEntry(
    response: MultiStatusResponse
) {
    val href: String
    val lastPhoto: Long
    val nbItems: Int
    val location: String?
    private val dateRange: String?

    companion object {
        private const val DATE_PATTERN = "MMM yyyy"
        private const val MILLIS = 1000L
        private const val PROPERTY_LAST_PHOTO = "last-photo"
        private const val PROPERTY_NB_ITEMS = "nbItems"
        private const val PROPERTY_LOCATION = "location"
        private const val PROPERTY_DATE_RANGE = "dateRange"
        private const val PROPERTY_COLLABORATORS = "collaborators"
    }

    init {

        href = response.href

        val properties = response.getProperties(HttpStatus.SC_OK)

        this.lastPhoto = parseLong(parseString(properties, PROPERTY_LAST_PHOTO))
        this.nbItems = parseInt(parseString(properties, PROPERTY_NB_ITEMS))
        this.location = parseString(properties, PROPERTY_LOCATION)
        this.dateRange = parseString(properties, PROPERTY_DATE_RANGE)
    }

    private fun parseString(
        props: DavPropertySet,
        name: String
    ): String? {
        val propName = DavPropertyName.create(name, Namespace.getNamespace("nc", WebdavEntry.NAMESPACE_NC))
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