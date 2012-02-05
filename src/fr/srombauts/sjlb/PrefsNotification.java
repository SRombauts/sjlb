package fr.srombauts.sjlb;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * Récupération et encapsulation des préférences de notification
 * @author srombauts
*/
class PrefsNotification {
    public Boolean  mbSound;
    public Boolean  mbVibrate;
    public Boolean  mbLight;

    // Interdiction du constructeur par défaut
    @SuppressWarnings("unused")
    private PrefsNotification () {
    }

    // Constructeur utilisant les préférences
    public PrefsNotification (Context aContext) {
        // Récupère les options de notification dans les préférences :
        SharedPreferences   Prefs       = PreferenceManager.getDefaultSharedPreferences(aContext);
                            mbSound		= Prefs.getBoolean(SJLB.PREFS.NOTIFICATION_SOUND,   true);
							mbVibrate	= Prefs.getBoolean(SJLB.PREFS.NOTIFICATION_VIBRATE, true);
							mbLight		= Prefs.getBoolean(SJLB.PREFS.NOTIFICATION_LIGHT,   true);
    }
}