/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.di

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.adjust.sdk.Adjust
import dagger.android.AndroidInjection

class ActivityInjector : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is Injectable) {
            AndroidInjection.inject(activity)
        }
        if (activity is FragmentActivity) {
            val fm = activity.supportFragmentManager
            fm.registerFragmentLifecycleCallbacks(FragmentInjector(), true)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        // unused atm
    }

    override fun onActivityResumed(activity: Activity) {
        // NMC Customization
        Adjust.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        // NMC Customization
        Adjust.onPause()
    }

    override fun onActivityStopped(activity: Activity) {
        // unused atm
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // unused atm
    }

    override fun onActivityDestroyed(activity: Activity) {
        // unused atm
    }
}
