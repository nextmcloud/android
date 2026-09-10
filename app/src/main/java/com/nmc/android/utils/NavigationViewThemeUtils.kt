package com.nmc.android.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.navigation.NavigationView
import com.nextcloud.android.common.ui.util.buildColorStateList
import com.owncloud.android.R

object NavigationViewThemeUtils {
    @JvmStatic
    @JvmOverloads
    fun colorNavigationView(
        context: Context,
        navigationView: NavigationView,
        colorIcons: Boolean = true
    ) {
        if (navigationView.itemBackground != null) {
            navigationView.itemBackground?.setTintList(
                buildColorStateList(
                    android.R.attr.state_checked to context.resources.getColor(R.color.nav_selected_bg_color, null),
                    -android.R.attr.state_checked to Color.TRANSPARENT
                )
            )
        }
        navigationView.background.setTintList(
            ColorStateList.valueOf(
                context.resources.getColor(
                    R.color.nav_bg_color,
                    null
                )
            )
        )

        val colorStateList =
            buildColorStateList(
                android.R.attr.state_checked to context.resources.getColor(R.color.nav_txt_selected_color, null),
                -android.R.attr.state_checked to context.resources.getColor(R.color.nav_txt_unselected_color, null),
            )

        navigationView.itemTextColor = colorStateList
        if (colorIcons) {
            navigationView.itemIconTintList = colorStateList
        }
    }
}