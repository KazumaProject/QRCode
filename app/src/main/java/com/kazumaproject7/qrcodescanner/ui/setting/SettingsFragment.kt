package com.kazumaproject7.qrcodescanner.ui.setting

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.kazumaproject7.qrcodescanner.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

    }
}