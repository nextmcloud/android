package com.nmc.android.utils

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.SearchView
import com.owncloud.android.R

object SearchViewThemeUtils {
    fun themeSearchView(context: Context, searchView: SearchView) {
        val fontColor = context.resources.getColor(R.color.fontAppbar, null)
        val editText: AppCompatAutoCompleteTextView =
            searchView.findViewById(com.google.android.material.R.id.search_src_text)
        editText.textSize = 16F
        editText.setTextColor(fontColor)
        editText.highlightColor = context.resources.getColor(R.color.et_highlight_color, null)
        editText.setHintTextColor(context.resources.getColor(R.color.fontSecondaryAppbar, null))
        val closeButton: ImageView = searchView.findViewById(com.google.android.material.R.id.search_close_btn)
        closeButton.setColorFilter(fontColor)
        val searchButton: ImageView = searchView.findViewById(com.google.android.material.R.id.search_button)
        searchButton.setImageResource(R.drawable.ic_search)
        searchButton.setColorFilter(fontColor)
    }
}