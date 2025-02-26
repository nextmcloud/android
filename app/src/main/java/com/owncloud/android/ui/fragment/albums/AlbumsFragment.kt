/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nextcloud.client.di.Injectable
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.fragment.ExtendedListFragment

class AlbumsFragment : ExtendedListFragment(), Injectable {

    companion object {
        val TAG: String = AlbumsFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log_OC.i(TAG, "onCreateView() start")
        val v = super.onCreateView(inflater, container, savedInstanceState)

        setSwipeEnabled(false) // Disable pull-to-refresh

        Log_OC.i(TAG, "onCreateView() end")
        return v
    }
}