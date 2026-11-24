/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.signup

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.utils.RawResourceReader
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.AccountSetupWebviewBinding
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.ui.NextcloudWebViewClient
import com.owncloud.android.utils.ProxyUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.lang.ref.WeakReference
import java.util.Locale

class SignupWebViewLogin(
    private val activity: FragmentActivity,
    private val viewThemeUtils: ViewThemeUtils,
    private val useGenericUserAgent: Boolean,
    private val baseURL: String,
    private val currentBinding: () -> AccountSetupWebviewBinding?,
    private val loginDataPrefix: String,
    private val onLoginDataUrl: (String) -> Unit
) {

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT")
    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    fun start(binding: AccountSetupWebviewBinding) {
        viewThemeUtils.platform.colorCircularProgressBar(
            binding.loginWebviewProgressBar,
            ColorRole.ON_PRIMARY_CONTAINER
        )
        binding.loginWebview.visibility = View.GONE
        binding.loginWebview.settings.run {
            allowFileAccess = false
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = if (useGenericUserAgent) MainApp.getUserAgent() else getWebLoginUserAgent()
            saveFormData = false
            savePassword = false
        }

        val headers = mapOf(RemoteOperation.OCS_API_HEADER to RemoteOperation.OCS_API_HEADER_VALUE)
        val url = baseURL.ifEmpty {
            activity.resources.getString(R.string.webview_login_url)
        }
        ProxyUtil(WeakReference<Context>(activity)).applyProxy {
            currentBinding()?.loginWebview?.loadUrl(url, headers)
        }
        setWebViewClient(binding)
    }

    private fun setWebViewClient(binding: AccountSetupWebviewBinding) {
        binding.loginWebview.webViewClient = object : NextcloudWebViewClient(activity.supportFragmentManager) {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                val url = request.url.toString()

                // NMC-3773 fix: close activity if access_denied error comes
                if (url.endsWith("&error=access_denied")) {
                    activity.finish()
                    return true
                }

                // NMC-4351 fix: recreate activity to show login screen when timeout occurs
                if (url.contains("error=interaction_required")
                    && url.contains("error_description=transaction%20timeout")
                ) {
                    activity.recreate()
                    return true
                }

                if (!url.startsWith(loginDataPrefix)) {
                    return false
                }
                onLoginDataUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // scroll to top when url loads
                // because directly loading Telekom login page it scrolls down automatically
                view?.scrollTo(0, 0);

                showLoadedWebView()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                showLoadedWebView()

                val customError = RawResourceReader.readText(activity.resources, R.raw.custom_error)
                if (customError.isNotEmpty()) {
                    currentBinding()?.loginWebview?.loadData(customError, HTML_MIME_TYPE, null)
                }
            }
        }
    }

    private fun showLoadedWebView() {
        currentBinding()?.run {
            loginWebviewProgressBar.visibility = View.GONE
            loginWebview.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val HTML_MIME_TYPE = "text/html; charset=UTF-8"

        private fun getWebLoginUserAgent(): String {
            return Build.MANUFACTURER.substring(0, 1).uppercase(Locale.getDefault()) +
                Build.MANUFACTURER.substring(1).lowercase(Locale.getDefault()) + " " + Build.MODEL + " (Android)"
        }
    }
}
