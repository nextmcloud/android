package com.nmc.android.utils

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.DrawableCompat

object DrawableThemeUtils {
    @JvmStatic
    fun tintDrawable(drawable: Drawable, @ColorInt color: Int): Drawable {
        val wrap: Drawable = DrawableCompat.wrap(drawable)
        wrap.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color, BlendModeCompat.SRC_ATOP
        )
        return wrap
    }
}