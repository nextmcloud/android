package com.nmc.android.utils

import android.content.res.ColorStateList
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.CompoundButtonCompat
import com.owncloud.android.MainApp
import com.owncloud.android.R

object CheckableThemeUtils {
    @JvmStatic
    fun tintCheckbox(vararg checkBoxes: AppCompatCheckBox) {
        for (checkBox in checkBoxes) {
            val checkEnabled = MainApp.getAppContext().resources.getColor(R.color.checkbox_checked_enabled)
            val checkDisabled = MainApp.getAppContext().resources.getColor(R.color.checkbox_checked_disabled)
            val uncheckEnabled = MainApp.getAppContext().resources.getColor(R.color.checkbox_unchecked_enabled)
            val uncheckDisabled = MainApp.getAppContext().resources.getColor(R.color.checkbox_unchecked_disabled)

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
            val checkColorStateList = ColorStateList(states, colors)
            CompoundButtonCompat.setButtonTintList(checkBox, checkColorStateList)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun tintSwitch(switchView: SwitchCompat, color: Int = 0, colorText: Boolean = false) {
        if (colorText) {
            switchView.setTextColor(color)
        }
        val thumbColorCheckedEnabled = MainApp.getAppContext().resources.getColor(R.color.switch_thumb_checked_enabled)
        val thumbColorUncheckedEnabled =
            MainApp.getAppContext().resources.getColor(R.color.switch_thumb_unchecked_enabled)
        val thumbColorCheckedDisabled =
            MainApp.getAppContext().resources.getColor(R.color.switch_thumb_checked_disabled)
        val thumbColorUncheckedDisabled =
            MainApp.getAppContext().resources.getColor(R.color.switch_thumb_unchecked_disabled)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_enabled, -android.R.attr.state_checked)
        )
        val thumbColors = intArrayOf(
            thumbColorCheckedEnabled,
            thumbColorCheckedDisabled,
            thumbColorUncheckedEnabled,
            thumbColorUncheckedDisabled
        )
        val thumbColorStateList = ColorStateList(states, thumbColors)
        val trackColorCheckedEnabled = MainApp.getAppContext().resources.getColor(R.color.switch_track_checked_enabled)
        val trackColorUncheckedEnabled =
            MainApp.getAppContext().resources.getColor(R.color.switch_track_unchecked_enabled)
        val trackColorCheckedDisabled =
            MainApp.getAppContext().resources.getColor(R.color.switch_track_checked_disabled)
        val trackColorUncheckedDisabled =
            MainApp.getAppContext().resources.getColor(R.color.switch_track_unchecked_disabled)

        val trackColors = intArrayOf(
            trackColorCheckedEnabled,
            trackColorCheckedDisabled,
            trackColorUncheckedEnabled,
            trackColorUncheckedDisabled
        )
        val trackColorStateList = ColorStateList(states, trackColors)

        // setting the thumb color
        DrawableCompat.setTintList(switchView.thumbDrawable, thumbColorStateList)

        // setting the track color
        DrawableCompat.setTintList(switchView.trackDrawable, trackColorStateList)
    }
}