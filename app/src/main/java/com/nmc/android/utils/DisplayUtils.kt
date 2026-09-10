package com.nmc.android.utils

import android.content.Context
import android.content.res.Configuration
import com.owncloud.android.R

object DisplayUtils {

    @JvmStatic
    fun isShowDividerForList(context: Context): Boolean = isTablet(context) || isLandscapeOrientation(context)

    @JvmStatic
    fun isTablet(context: Context): Boolean = context.resources.getBoolean(R.bool.isTablet)

    @JvmStatic
    fun isLandscapeOrientation(context: Context): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}