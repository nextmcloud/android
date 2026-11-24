/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui

import android.content.Context
import android.content.res.ColorStateList
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

@Suppress("DEPRECATION")
class ThemeableSwitchPreference : SwitchPreference {
    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    /**
     * Do not delete constructor. These are used.
     */
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        MainApp.getAppComponent().inject(this)
        setWidgetLayoutResource(R.layout.themeable_switch)
    }

    @Deprecated("Deprecated in Java")
    override fun onBindView(view: View) {
        super.onBindView(view)
        val checkable = view.findViewById<View>(R.id.switch_widget)

        // NMC Customization
        // region start
        val states = arrayOf<IntArray?>(
            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),  // enabled and checked
            intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),  // enabled and unchecked
            intArrayOf(-android.R.attr.state_enabled) // disabled
        )

        val thumbColorCheckedEnabled = ResourcesCompat.getColor(
            checkable.context.resources,
            R.color.switch_thumb_checked_enabled,
            checkable.context.theme
        )
        val thumbColorUncheckedEnabled = ResourcesCompat.getColor(
            checkable.context.resources,
            R.color.switch_thumb_unchecked_enabled,
            checkable.context.theme
        )
        val thumbColorDisabled =
            ResourcesCompat.getColor(
                checkable.context.resources,
                R.color.switch_thumb_disabled,
                checkable.context.theme
            )

        val thumbColors = intArrayOf(
            thumbColorCheckedEnabled,
            thumbColorUncheckedEnabled,
            thumbColorDisabled
        )

        val thumbColorStateList = ColorStateList(states, thumbColors)

        val trackColorCheckedEnabled = ResourcesCompat.getColor(
            checkable.context.resources,
            R.color.switch_track_checked_enabled,
            checkable.context.theme
        )
        val trackColorUncheckedEnabled = ResourcesCompat.getColor(
            checkable.context.resources,
            R.color.switch_track_unchecked_enabled,
            checkable.context.theme
        )
        val trackColorDisabled = ResourcesCompat.getColor(
            checkable.context.resources,
            R.color.switch_track_disabled,
            checkable.context.theme
        )

        val trackColors = intArrayOf(
            trackColorCheckedEnabled,
            trackColorUncheckedEnabled,
            trackColorDisabled
        )
        val trackColorStateList = ColorStateList(states, trackColors)
        // region end

        if (checkable is MaterialSwitch) {
            checkable.setChecked(isChecked)
            checkable.thumbTintList = thumbColorStateList
            checkable.trackTintList = trackColorStateList
        }
    }
}
