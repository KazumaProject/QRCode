package com.kazumaproject7.qrcodescanner.ui.setting

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kazumaproject7.qrcodescanner.BuildConfig
import com.kazumaproject7.qrcodescanner.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        when (context?.resources?.configuration?.uiMode?.and(
            Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.black)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.off_white)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                window.statusBarColor = ContextCompat.getColor(requireContext(),R.color.off_white)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val openLicensePreference = findPreference<Preference>("license_preference_key")
        openLicensePreference?.setOnPreferenceClickListener {
            findNavController().navigate(
                SettingsFragmentDirections.actionSettingsFragmentToOpenSourceLicenseFragment()
            )
            return@setOnPreferenceClickListener true
        }
        val appVersionPreference = findPreference<Preference>("about_version_key")
        appVersionPreference?.summary = "Version: ${BuildConfig.VERSION_NAME}"
    }
}