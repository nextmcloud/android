package com.owncloud.android.ui

import android.content.Context
import android.content.pm.PackageManager
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.StringUtils

class AppVersionPreference : Preference {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun getView(convertView: View?, parent: ViewGroup?): View {
        val v = super.getView(convertView, parent)
        updatePreferenceView(v.findViewById(R.id.title), v.findViewById(R.id.summary))
        return v
    }

    private fun updatePreferenceView(title: TextView, summary: TextView) {
        val appVersion = appVersion
        val titleColor: Int = context.resources.getColor(R.color.fontAppbar, null)
        title.text = StringUtils.getColorSpan(
            context.getString(R.string.app_name),
            titleColor
        )
        summary.text = String.format(context.getString(R.string.about_version), appVersion)
    }

    private val appVersion: String
        get() {
            var temp: String
            try {
                val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
                temp = pkg.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                temp = ""
                Log_OC.e(TAG, "Error while showing about dialog", e)
            }
            return temp
        }

    companion object {
        private val TAG = AppVersionPreference::class.java.simpleName
    }
}