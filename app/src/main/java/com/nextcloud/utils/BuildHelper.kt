/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

object BuildHelper {
    const val GPLAY: String = "gplay"

    // NMC Customization to have it false always
    fun isFlavourGPlay(): Boolean = false
}
