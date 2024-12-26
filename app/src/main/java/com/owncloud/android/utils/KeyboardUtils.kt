/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022-2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.view.Window
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import javax.inject.Inject

class KeyboardUtils @Inject constructor() {

    fun showKeyboardForEditText(window: Window?, editText: EditText) {
        if (window != null) {
            editText.requestFocus()
            WindowCompat.getInsetsController(window, editText).show(WindowInsetsCompat.Type.ime())
        }
    }

    fun hideKeyboardFrom(context: Context, view: View) {
        view.clearFocus()
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
