package com.owncloud.android.operations.comments

/**
 * response from the Get comments api
 * <?xml version="1.0" encoding="UTF-8"?>
 * <d:multistatus xmlns:d="DAV:" xmlns:nc="http://nextcloud.org/ns" xmlns:oc="http://owncloud.org/ns" xmlns:s="http://sabredav.org/ns">
 *    <d:response>
 *       <d:href>/remote.php/dav/comments/files/581625/</d:href>
 *       <d:propstat>
 *          <d:prop>
 *             <d:resourcetype>
 *                <d:collection />
 *             </d:resourcetype>
 *             <oc:readMarker>Wed, 05 Oct 2022 07:54:20 GMT</oc:readMarker>
 *          </d:prop>
 *          <d:status>HTTP/1.1 200 OK</d:status>
 *       </d:propstat>
 *    </d:response>
 *    <d:response>
 *       <d:href>/remote.php/dav/comments/files/581625/99</d:href>
 *       <d:propstat>
 *          <d:prop>
 *             <d:resourcetype />
 *             <oc:id>99</oc:id>
 *             <oc:parentId>0</oc:parentId>
 *             <oc:topmostParentId>0</oc:topmostParentId>
 *             <oc:childrenCount>0</oc:childrenCount>
 *             <oc:message>Cghjgrrg</oc:message>
 *             <oc:verb>comment</oc:verb>
 *             <oc:actorType>users</oc:actorType>
 *             <oc:actorId>120049010000000010088671</oc:actorId>
 *             <oc:creationDateTime>Wed, 05 Oct 2022 07:54:20 GMT</oc:creationDateTime>
 *             <oc:latestChildDateTime />
 *             <oc:objectType>files</oc:objectType>
 *             <oc:objectId>581625</oc:objectId>
 *             <oc:referenceId />
 *             <oc:reactions />
 *             <oc:actorDisplayName>Dev.Kumar</oc:actorDisplayName>
 *             <oc:mentions />
 *             <oc:isUnread>false</oc:isUnread>
 *          </d:prop>
 *          <d:status>HTTP/1.1 200 OK</d:status>
 *       </d:propstat>
 *    </d:response>
 * </d:multistatus>
 */

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Comments(val path: String,
                    val commentId: Int,
                    val message: String,
                    val actorId: String,
                    val actorDisplayName: String,
                    val actorType: String,
                    val creationDateTime: Date? = null,
                    val isUnread: Boolean = false,
                    val objectId: String,
                    val objectType: String,
                    val verb: String) : Parcelable