package com.owncloud.android.ui

import android.content.Context
import android.graphics.Typeface
import android.preference.PreferenceCategory
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.owncloud.android.R

class PreferenceCustomCategory : PreferenceCategory {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(
        context: Context?, attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle)

    override fun onBindView(view: View) {
        super.onBindView(view)
        val titleView = view.findViewById<TextView>(android.R.id.title)
        titleView.setTextColor(context.resources.getColor(R.color.text_color))
        titleView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            context.resources.getDimensionPixelSize(R.dimen.txt_size_16sp).toFloat()
        )
        titleView.setTypeface(null, Typeface.BOLD)
    }
}