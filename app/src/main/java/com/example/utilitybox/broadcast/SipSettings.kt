package com.example.utilitybox.broadcast

import android.os.Bundle
import android.preference.PreferenceActivity
import com.example.utilitybox.R

class SipSettings: PreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }
}
