package com.nmc.android.utils

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.owncloud.android.R

object DialogThemeUtils {
    fun colorMaterialAlertDialogBackground(context: Context, dialogBuilder: MaterialAlertDialogBuilder) {
        val materialShapeDrawable = MaterialShapeDrawable(
            context,
            null,
            R.attr.alertDialogStyle,
            R.style.MaterialAlertDialog_MaterialComponents
        )
        materialShapeDrawable.initializeElevationOverlay(context)
        materialShapeDrawable.fillColor =
            ColorStateList.valueOf(context.resources.getColor(R.color.alert_bg_color, null))

        // dialogCornerRadius first appeared in Android Pie
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val radius: Float =
                context.resources.getDimension(com.nextcloud.android.common.ui.R.dimen.dialogBorderRadius)
            materialShapeDrawable.setCornerSize(radius)
        }
        dialogBuilder.background = materialShapeDrawable
    }
}