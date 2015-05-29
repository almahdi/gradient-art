package me.krrr.wallpaper

import android.os.Bundle
import android.preference.PreferenceActivity

class Preference extends PreferenceActivity {

    override def onCreate(saved: Bundle) {
        super.onCreate(saved)
        addPreferencesFromResource(R.xml.preference)
    }
}
