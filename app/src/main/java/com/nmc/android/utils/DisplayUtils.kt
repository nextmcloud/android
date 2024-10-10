package com.nmc.android.utils

import android.content.res.Configuration
import com.owncloud.android.MainApp
import com.owncloud.android.R

object DisplayUtils {

    @JvmStatic
    fun isShowDividerForList(): Boolean = isTablet() || isLandscapeOrientation()

    @JvmStatic
    fun isTablet(): Boolean = MainApp.getAppContext().resources.getBoolean(R.bool.isTablet)

    @JvmStatic
    fun isLandscapeOrientation(): Boolean =
        MainApp.getAppContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}