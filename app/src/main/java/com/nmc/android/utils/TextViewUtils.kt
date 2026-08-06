/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nmc.android.utils

import android.content.res.Resources
import android.text.Layout
import android.util.DisplayMetrics
import android.view.ViewTreeObserver
import android.widget.TextView

interface EllipsizeListener {
    fun onResult(isEllipsized: Boolean)
}

object TextViewUtils {

    @JvmStatic
    fun isTextEllipsized(textView: TextView, listener: EllipsizeListener) {
        // check for devices density smaller than 320dpi
        // NMC-4347 fix for smaller devices
        if (Resources.getSystem().displayMetrics.densityDpi <= DisplayMetrics.DENSITY_XHIGH) {
            listener.onResult(true)
            return
        }

        textView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                textView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val layout: Layout? = textView.layout
                val isEllipsized = layout?.let {
                    for (i in 0 until it.lineCount) {
                        if (it.getEllipsisCount(i) > 0) return@let true
                    }
                    false
                } ?: false

                listener.onResult(isEllipsized)
            }
        })
    }
}
