package com.nmc.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.nextcloud.client.preferences.AppPreferences
import com.nmc.android.utils.makeLinks
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityLoginPrivacySettingsBinding
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import javax.inject.Inject

class LoginPrivacySettingsActivity : ToolbarActivity() {

    companion object {
        @JvmStatic
        fun openPrivacySettingsActivity(context: Context) {
            val intent = Intent(context, LoginPrivacySettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityLoginPrivacySettingsBinding

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginPrivacySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        resetPreferenceForPrivacy()
        //don't show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        updateActionBarTitleAndHomeButtonByString(resources.getString(R.string.privacy_settings))
        setUpPrivacyText()
        binding.privacyAcceptBtn.setOnClickListener {
            //on accept finish the activity
            //update the accept privacy action to preferences
            preferences.privacyPolicyAction = PrivacyUserAction.ACCEPT_ACTION
            openFileDisplayActivity()
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    private fun resetPreferenceForPrivacy() {
        preferences.privacyPolicyAction = PrivacyUserAction.NO_ACTION
    }

    private fun setUpPrivacyText() {
        val privacyText = String.format(
            resources.getString(R.string.login_privacy_settings_intro_text), resources
                .getString(R.string.login_privacy_policy), resources
                .getString(R.string.login_privacy_reject), resources
                .getString(R.string.login_privacy_settings)
        )
        binding.tvLoginPrivacyIntroText.text = privacyText

        //make links clickable
        binding.tvLoginPrivacyIntroText.makeLinks(
            Pair(resources.getString(R.string.login_privacy_policy), View.OnClickListener {
                //open privacy policy url
                val intent = Intent(this, ExternalSiteWebView::class.java)
                intent.putExtra(
                    ExternalSiteWebView.EXTRA_TITLE,
                    resources.getString(R.string.privacy_policy)
                )
                intent.putExtra(ExternalSiteWebView.EXTRA_URL, resources.getString(R.string.privacy_url))
                intent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
                intent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, -1)
                startActivity(intent)
            }), Pair(resources
                .getString(R.string.login_privacy_reject), View.OnClickListener {
                //disable data analysis option and close the activity
                preferences.setDataAnalysis(false)
                //update the reject privacy action to preferences
                preferences.privacyPolicyAction = PrivacyUserAction.REJECT_ACTION
                openFileDisplayActivity()
            }), Pair(resources
                .getString(R.string.login_privacy_settings), View.OnClickListener {
                //open privacy settings screen
                PrivacySettingsActivity.openPrivacySettingsActivity(this, true)
            })
        )
    }

    private fun openFileDisplayActivity() {
        //update the version code when user has accepted or rejected privacy policy
        //this will be used to help to check app up-gradation
        preferences.lastSeenVersionCode = BuildConfig.VERSION_CODE

        val i = Intent(this, FileDisplayActivity::class.java)
        i.action = FileDisplayActivity.RESTART
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(i)
        finish()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // user cannot close this screen without accepting or rejecting the privacy policy
        }
    }
}
