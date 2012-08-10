package fr.srombauts.sjlb.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import fr.srombauts.sjlb.db.SJLB;


/**
 * Récupération et encapsulation des préférence
 * @author 27/08/2010 SRombauts
*/
public class PrefsInterface {
    public Boolean  mbSwitchScreenDirection;

    // Interdiction du constructeur par défaut
    @SuppressWarnings("unused")
    private PrefsInterface () {
    }

    /**
     * Constructeur récupérant les préférences de comportement de l'interface utilisateur
     *
     * @param[in] aContext Contexte de l'activité/du service, nécessaire pour récupérer les préférences de l'application
     */
    public PrefsInterface (Context aContext) {
        SharedPreferences   Prefs                   = PreferenceManager.getDefaultSharedPreferences(aContext);
                            mbSwitchScreenDirection = Prefs.getBoolean(SJLB.PREFS.SWITCH_SCREEN_DIRECTION, true);
    }
    
    /**
     * Accesseur simple à la configuration de la direction dans laquelle il faut bouger le doigt pour changer d'écran
     *
     * @param[in] aContext Contexte de l'activité/du service, nécessaire pour récupérer les préférences de l'application
     */
    public static boolean inverseSwitchScreenDirection (Context aContext) {
        SharedPreferences   Prefs = PreferenceManager.getDefaultSharedPreferences(aContext);
        return Prefs.getBoolean(SJLB.PREFS.SWITCH_SCREEN_DIRECTION, true);
    }
}