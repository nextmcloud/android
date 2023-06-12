package com.nmc.android.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.owncloud.android.R

object FabThemeUtils {
    @JvmStatic
    fun colorFloatingActionButton(
        context: Context,
        button: FloatingActionButton
    ) {
        val primaryColor = context.resources.getColor(R.color.primary, null)
        val disableColor = context.resources.getColor(R.color.grey_0, null)

        val bgStates = arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
        )
        val bgColors = intArrayOf(
            primaryColor,
            disableColor
        )

        button.backgroundTintList = ColorStateList(bgStates, bgColors)

        val imageStates = arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
        )
        val imageColors = intArrayOf(
            Color.WHITE,
            disableColor
        )

        button.imageTintList = ColorStateList(imageStates, imageColors)
    }
}