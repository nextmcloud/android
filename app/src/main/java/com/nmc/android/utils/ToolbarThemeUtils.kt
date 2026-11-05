package com.nmc.android.utils

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.appcompat.app.ActionBar
import com.owncloud.android.R
import com.owncloud.android.utils.StringUtils

object ToolbarThemeUtils {
    @JvmStatic
    fun setColoredTitle(context: Context, actionBar: ActionBar?, title: String) {
        if (actionBar != null) {
            val text: Spannable = StringUtils.getColorSpan(title, context.resources.getColor(R.color.fontAppbar, null))

            //bold the magenta from MagentaCLOUD title
            if (title.contains(context.resources.getString(R.string.app_name))) {
                val textToBold = context.resources.getString(R.string.splashScreenBold)
                val indexStart = title.indexOf(textToBold)
                val indexEnd = indexStart + textToBold.length
                text.setSpan(StyleSpan(Typeface.BOLD), indexStart, indexEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            actionBar.title = text
        }
    }
}