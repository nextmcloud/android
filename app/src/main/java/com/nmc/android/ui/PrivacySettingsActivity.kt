package com.nmc.android.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.nextcloud.client.preferences.AppPreferences
import com.nmc.android.utils.CheckableThemeUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityPrivacySettingsBinding
import com.owncloud.android.ui.activity.ToolbarActivity
import javax.inject.Inject

class PrivacySettingsActivity : ToolbarActivity() {

    companion object {
        private const val EXTRA_SHOW_SETTINGS = "show_settings_button"

        @JvmStatic
        fun openPrivacySettingsActivity(context: Context, isShowSettings: Boolean) {
            val intent = Intent(context, PrivacySettingsActivity::class.java)
            intent.putExtra(EXTRA_SHOW_SETTINGS, isShowSettings)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityPrivacySettingsBinding

    /**
     * variable to check if save settings button needs to be shown or not
     * currently we are showing only when user opens this activity from LoginPrivacySettingsActivity
     */
    private var isShowSettingsButton = false

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupActionBar()
        setUpViews()
        showHideSettingsButton()
    }

    private fun setupActionBar() {
        supportActionBar?.let {
            viewThemeUtils.platform.themeStatusBar(this)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
            //custom color for back arrow for NMC
            viewThemeUtils.files.themeActionBar(this, it, resources.getString(R.string.privacy_settings))
            it.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.bg_default, null)))
        }
    }

    private fun showHideSettingsButton() {
        isShowSettingsButton = intent.getBooleanExtra(EXTRA_SHOW_SETTINGS, false)
        binding.privacySaveSettingsBtn.visibility = if (isShowSettingsButton) View.VISIBLE else View.GONE
    }

    private fun setUpViews() {
        CheckableThemeUtils.tintSwitch(binding.switchDataCollection)
        CheckableThemeUtils.tintSwitch(binding.switchDataAnalysis)
        binding.switchDataAnalysis.isChecked = preferences.isDataAnalysisEnabled
        binding.switchDataAnalysis.setOnCheckedChangeListener { _, isChecked ->
            // if user is coming here from Settings then directly update data analysis flag
            // else it will be updated on save settings button click
            if (!isShowSettingsButton) {
                preferences.setDataAnalysis(isChecked)
            }
        }
        binding.privacySaveSettingsBtn.setOnClickListener {
            // update user selected data analysis flag and finish the activity
            preferences.setDataAnalysis(binding.switchDataAnalysis.isChecked)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
