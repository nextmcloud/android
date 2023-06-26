/*
 * Nextcloud Android client application
 *
 *  @author ZetaTom
 *  @author Álvaro Brey
 *  Copyright (C) 2023 ZetaTom
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
