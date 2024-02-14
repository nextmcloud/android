/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.utils.extensions

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.Serializable

@Suppress("TopLevelPropertyNaming")
private const val tag = "IntentExtension"

fun <T : Serializable?> Intent?.getSerializableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getSerializableExtra(key, type)
        } else {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            if (type.isInstance(this.getSerializableExtra(key))) {
                this.getSerializableExtra(key) as T
            } else {
                null
            }
        }
    } catch (e: ClassCastException) {
        Log_OC.e(tag, e.localizedMessage)
        null
    }
}

fun <T : Parcelable?> Intent?.getParcelableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getParcelableExtra(key, type)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelableExtra(key)
        }
    } catch (e: ClassCastException) {
        Log_OC.e(tag, e.localizedMessage)
        null
    }
}
