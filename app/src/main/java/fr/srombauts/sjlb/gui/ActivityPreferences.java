package fr.srombauts.sjlb.gui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import fr.srombauts.sjlb.R;

public class ActivityPreferences extends PreferenceActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }
}
