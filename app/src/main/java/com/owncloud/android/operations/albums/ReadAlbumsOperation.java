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

import android.text.TextUtils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReadAlbumsOperation extends RemoteOperation<List<ReadAlbumsOperation.PhotoAlbumEntry>> {

    private static final String TAG = ReadAlbumsOperation.class.getSimpleName();
    private String albumPath = null;

    public ReadAlbumsOperation() {
        Log_OC.e(TAG, "Fetch albums remote operation");
    }

    public ReadAlbumsOperation(String albumPath) {
        Log_OC.e(TAG, "Fetch albums remote operation");
        this.albumPath = albumPath;
    }

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult<List<ReadAlbumsOperation.PhotoAlbumEntry>> run(OwnCloudClient client) {
        Log_OC.e(TAG, "Fetch albums remote operation running");
        PropFindMethod propfind = null;
        RemoteOperationResult<List<ReadAlbumsOperation.PhotoAlbumEntry>> result;
        String url = "https://pre1.next.magentacloud.de/remote.php/dav/photos/" + client.getUserId() + "/albums/";
        if (!TextUtils.isEmpty(albumPath)) {
            url += WebdavUtils.encodePath(albumPath);
        }
        try {
            propfind = new PropFindMethod(url, getProp(), DavConstants.DEPTH_1);
            int status = client.executeMethod(propfind);
            Log_OC.e(TAG, "Fetch albums remote: " + status);
            boolean isSuccess = status == HttpStatus.SC_MULTI_STATUS || status == HttpStatus.SC_OK;
            if (isSuccess) {
                MultiStatus multiStatus = propfind.getResponseBodyAsMultiStatus();
                List<PhotoAlbumEntry> albumsList = new ArrayList<>();
                for (MultiStatusResponse response : multiStatus.getResponses()) {
                    Log_OC.e("Albums", "Response: " + response);
                    int st = response.getStatus()[0].getStatusCode();
                    if (st == HttpStatus.SC_OK) {
                        PhotoAlbumEntry entry = new PhotoAlbumEntry(response);
                        Log_OC.e("Albums", "Href: " + entry.getHref() + "\nLast Photo: " + entry.getLastPhoto() + "\nDate Range: " + entry.getDateRange() + "\nItems: "
                            + entry.getNbItems() + "\nLocation: " + entry.getLocation());
                        albumsList.add(entry);
                    }
                }
                result = new RemoteOperationResult(true, propfind);
                result.setResultData(albumsList);
            } else {
                Log_OC.e(TAG, "Fetch albums remote else: " + propfind.getResponseBodyAsStream());
                result = new RemoteOperationResult(false, propfind);
                client.exhaustResponse(propfind.getResponseBodyAsStream());
            }
        } catch (Exception var13) {
            Exception e = var13;
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Read file " + " failed: " + result.getLogMessage(), result.getException());
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

    public static class PhotoAlbumEntry {

        private final String href;
        private final long lastPhoto;
        private final int nbItems;
        private final String location;
        private final String dateRange;

        public PhotoAlbumEntry(MultiStatusResponse response) {
            this.href = response.getHref();

            DavPropertySet properties = response.getProperties(HttpStatus.SC_OK);

            this.lastPhoto = parseLong(getValue(properties, "last-photo"));
            this.nbItems = parseInt(getValue(properties, "nbItems"));
            this.location = getValue(properties, "location");
            this.dateRange = getValue(properties, "dateRange");
        }

        private String getValue(DavPropertySet props, String name) {
            DavPropertyName propName = DavPropertyName.create(name, Namespace.getNamespace("nc", "http://nextcloud.org/ns"));
            DavProperty<?> prop = props.get(propName);
            return prop != null && prop.getValue() != null ? prop.getValue().toString() : null;
        }

        private int parseInt(String value) {
            try {
                return value != null ? Integer.parseInt(value) : 0;
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private long parseLong(String value) {
            try {
                return value != null ? Long.parseLong(value) : 0L;
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        public String getHref() {
            return href;
        }

        public long getLastPhoto() {
            return lastPhoto;
        }

        public int getNbItems() {
            return nbItems;
        }

        public String getLocation() {
            return location;
        }

        public String getDateRange() {
            return dateRange;
        }

        public String getAlbumName() {
            String href = getHref();
            if (href == null || href.isEmpty()) {
                return null;
            }

            // Remove trailing slash if present
            if (href.endsWith("/")) {
                href = href.substring(0, href.length() - 1);
            }

            // Split and return last part
            String[] parts = href.split("/");
            return parts.length > 0 ? parts[parts.length - 1] : null;
        }

        public String getCreatedDate() {
            String jsonRange = getDateRange();

            if (jsonRange == null || jsonRange.isEmpty()) {
                Date date = new Date(System.currentTimeMillis()); // Convert to milliseconds
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                return sdf.format(date);
            }

            try {
                JSONObject obj = new JSONObject(jsonRange);
                long startTimestamp = obj.optLong("start", 0);

                if (startTimestamp > 0) {
                    Date date = new Date(startTimestamp * 1000L); // Convert to milliseconds
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                    return sdf.format(date);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Date date = new Date(System.currentTimeMillis()); // Convert to milliseconds
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                return sdf.format(date);
            }
            Date date = new Date(System.currentTimeMillis()); // Convert to milliseconds
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            return sdf.format(date);
        }


    }

}
