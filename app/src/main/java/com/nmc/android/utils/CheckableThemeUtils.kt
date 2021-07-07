package com.nmc.android.utils

import android.content.res.ColorStateList
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
import com.owncloud.android.R

object CheckableThemeUtils {
    @JvmStatic
    fun tintCheckbox(vararg checkBoxes: AppCompatCheckBox) {
        for (checkBox in checkBoxes) {
            val checkEnabled = ResourcesCompat.getColor(
                checkBox.context.resources,
                R.color.checkbox_checked_enabled,
                checkBox.context.theme
            )
            val checkDisabled = ResourcesCompat.getColor(
                checkBox.context.resources,
                R.color.checkbox_checked_disabled,
                checkBox.context.theme
            )
            val uncheckEnabled = ResourcesCompat.getColor(
                checkBox.context.resources,
                R.color.checkbox_unchecked_enabled,
                checkBox.context.theme
            )
            val uncheckDisabled = ResourcesCompat.getColor(
                checkBox.context.resources,
                R.color.checkbox_unchecked_disabled,
                checkBox.context.theme
            )

            val states = arrayOf(
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_enabled, -android.R.attr.state_checked)
            )
            val colors = intArrayOf(
                checkEnabled,
                checkDisabled,
                uncheckEnabled,
                uncheckDisabled
            )
            checkBox.buttonTintList = ColorStateList(states, colors)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun tintSwitch(switchView: SwitchCompat, color: Int = 0, colorText: Boolean = false) {
        if (colorText) {
            switchView.setTextColor(color)
        }

        val states = arrayOf(
            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_enabled)
        )

        val thumbColorCheckedEnabled = ResourcesCompat.getColor(
            switchView.context.resources,
            R.color.switch_thumb_checked_enabled,
            switchView.context.theme
        )
        val thumbColorUncheckedEnabled =
            ResourcesCompat.getColor(
                switchView.context.resources,
                R.color.switch_thumb_unchecked_enabled,
                switchView.context.theme
            )
        val thumbColorDisabled =
            ResourcesCompat.getColor(
                switchView.context.resources,
                R.color.switch_thumb_disabled,
                switchView.context.theme
            )

        val thumbColors = intArrayOf(
            thumbColorCheckedEnabled,
            thumbColorUncheckedEnabled,
            thumbColorDisabled
        )
        val thumbColorStateList = ColorStateList(states, thumbColors)

        val trackColorCheckedEnabled = ResourcesCompat.getColor(
            switchView.context.resources,
            R.color.switch_track_checked_enabled,
            switchView.context.theme
        )
        val trackColorUncheckedEnabled =
            ResourcesCompat.getColor(
                switchView.context.resources,
                R.color.switch_track_unchecked_enabled,
                switchView.context.theme
            )
        val trackColorDisabled =
            ResourcesCompat.getColor(
                switchView.context.resources,
                R.color.switch_track_disabled,
                switchView.context.theme
            )

        val trackColors = intArrayOf(
            trackColorCheckedEnabled,
            trackColorUncheckedEnabled,
            trackColorDisabled
        )

        val trackColorStateList = ColorStateList(states, trackColors)

        switchView.thumbTintList = thumbColorStateList
        switchView.trackTintList = trackColorStateList
    }
}