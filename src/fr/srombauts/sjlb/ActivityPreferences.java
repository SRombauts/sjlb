package fr.srombauts.sjlb;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ActivityPreferences extends PreferenceActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }
}
