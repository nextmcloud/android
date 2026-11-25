package com.nmc.android.utils

import android.content.Context
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.owncloud.android.R

object SwipeRefreshThemeUtils {
    @JvmStatic
    fun themeSwipeRefreshLayout(context: Context, swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.setColorSchemeColors(context.resources.getColor(R.color.primary, null))
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.refresh_layout_bg_color)
    }
}