package fr.srombauts.sjlb.model;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import fr.srombauts.sjlb.db.SJLB;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * Récupération et encapsulation du couple login/password renseigné dans les préférences
*/
public class PrefsLoginPassword {
    public String mLogin;
    public String mPasswordMD5;

    // Interdiction du constructeur par défaut
    @SuppressWarnings("unused")
    private PrefsLoginPassword () {
    }

    /**
     * Constructeur utilisant les préférences pour récupérer login/password et hasher ce dernier
     *
     * @param[in] aContext Contexte de l'activité/du service, nécessaire pour récupérer les préférences de l'application
     */
    public PrefsLoginPassword (Context aContext) throws LoginPasswordEmptyException {
        try {
            
            // Récupère le login/mot de passe dans les préférences :
            SharedPreferences   prefs       = PreferenceManager.getDefaultSharedPreferences(aContext);
                                mLogin      = prefs.getString(SJLB.PREFS.LOGIN,    "");
            String              password    = prefs.getString(SJLB.PREFS.PASSWORD, "");
        
            if (   (false == mLogin.contentEquals(""))
                && (false == password.contentEquals("")) )
            {
                // Génère le hash MD5 du mot de passe
                mPasswordMD5 = "";
                MessageDigest digest = java.security.MessageDigest.getInstance("MD5");  
                digest.update(password.getBytes());  
                byte[] messageDigest = digest.digest();
                BigInteger number = new BigInteger(1,messageDigest);
                mPasswordMD5 = number.toString(16);
           
                // Complète au besoin par des 0
                while (mPasswordMD5.length() < 32) {
                    mPasswordMD5 = "0" + mPasswordMD5;
                }
            }
            else
            {
				// Lève une exception dédiée lorsque login/mdp ne sont pas renseignés !
				throw new LoginPasswordEmptyException ();
            }
        } catch (NoSuchAlgorithmException e) {  
            e.printStackTrace();  
        }
    }
    
    /**
     * Regarde si les préférences contiennent login/mdp
     *
     * @param[in] aContext Contexte de l'activité/du service, nécessaire pour récupérer les préférences de l'application
     *
     * @return boolean true si login ET mdp sont renseignés
     */
    static public boolean AreFilled (Context aContext) {
            // Récupère le login/mot de passe dans les préférences :
            SharedPreferences   prefs       = PreferenceManager.getDefaultSharedPreferences(aContext);
            String              login       = prefs.getString(SJLB.PREFS.LOGIN,    "");
            String              password    = prefs.getString(SJLB.PREFS.PASSWORD, "");
            
            return (   (false == login.contentEquals(""))
                     && (false == password.contentEquals("")) );
    }
    
    /**
     * Supprime le mot de passe des préférences (sur erreur de login remonté par le serveur)
     *
     * @param[in] aContext Contexte de l'activité/du service, nécessaire pour récupérer les préférences de l'application
     */
    static public void InvalidatePassword (Context aContext) {
        // Récupère l'interface d'édition des préférences
        SharedPreferences.Editor PrefsEditor = PreferenceManager.getDefaultSharedPreferences(aContext).edit();
        PrefsEditor.remove(SJLB.PREFS.PASSWORD);
        PrefsEditor.commit();
    }
}