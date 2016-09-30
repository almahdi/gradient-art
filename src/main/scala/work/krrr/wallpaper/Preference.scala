package work.krrr.wallpaper

import android.os.Bundle
import android.preference.PreferenceActivity

//noinspection ScalaDeprecation, must be deprecated to support old device
class Preference extends PreferenceActivity {
    override def onCreate(saved: Bundle) {
        super.onCreate(saved)
        addPreferencesFromResource(R.xml.preference)
    }
}
