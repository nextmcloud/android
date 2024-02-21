/*
 * Nextcloud Android client application
 *
 * @author ZetaTom
 * @author Tobias Kaminsky
 * Copyright (C) 2023 ZetaTom
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.preferences.AppPreferences
import com.nmc.android.marketTracking.AdjustSdkUtils
import com.nmc.android.marketTracking.TealiumSdkUtils
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nmc.android.utils.DialogThemeUtils
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

// NMC Customization: We don't need two option for logout. On logout directly logout the user locally from the app
class AccountRemovalDialog : DialogFragment(), Injectable {

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var appPreferences: AppPreferences

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = requireArguments().getParcelableArgument(KEY_USER, User::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.delete_account)
            .setMessage(resources.getString(R.string.delete_account_warning, user!!.accountName))
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton(R.string.common_ok) { _: DialogInterface?, _: Int ->
                // track adjust and tealium events on logout confirmed
                AdjustSdkUtils.trackEvent(AdjustSdkUtils.EVENT_TOKEN_SETTINGS_LOGOUT, appPreferences)
                TealiumSdkUtils.trackEvent(TealiumSdkUtils.EVENT_SETTINGS_LOGOUT, appPreferences)
                backgroundJobManager.startAccountRemovalJob(
                    user!!.accountName,
                    false
                )
            }
            .setNegativeButton(R.string.common_cancel, null)

        // NMC customization
        DialogThemeUtils.colorMaterialAlertDialogBackground(requireActivity(), builder)

        return builder.create()
    }

    companion object {
        private const val KEY_USER = "USER"

        @JvmStatic
        fun newInstance(user: User) = AccountRemovalDialog().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_USER, user)
            }
        }
    }
}
