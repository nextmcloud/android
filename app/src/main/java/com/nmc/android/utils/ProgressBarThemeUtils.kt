/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nmc.android.utils

import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat

/**
 * theming progress and seek bar for NMC
 */
object ProgressBarThemeUtils {

    @JvmStatic
    fun themeHorizontalSeekBar(seekBar: SeekBar, @ColorInt color: Int) {
        themeHorizontalProgressBar(seekBar, color)
        seekBar.thumb.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
    }

    @JvmStatic
    fun themeHorizontalProgressBar(progressBar: ProgressBar?, @ColorInt color: Int) {
        progressBar?.indeterminateDrawable?.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
        progressBar?.progressDrawable?.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
    }
}