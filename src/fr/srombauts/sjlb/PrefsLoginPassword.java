package fr.srombauts.sjlb;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * Récupération et encapsulation du couple login/password renseigné dans les préférences
*/
class PrefsLoginPassword {
    public String mLogin;
    public String mPasswordMD5;

    // Interdiction du constructeur par défaut
    @SuppressWarnings("unused")
    private PrefsLoginPassword () {
    }

    /**
     * Constructeur utilisant les préférences pour récupérer login/password et hasher ce dernier
     *
     * @param[in] aContext Contexte de l'activité/du servicé, nécessaire pour récupérer les préférences de l'application
     */
    public PrefsLoginPassword (Context aContext) throws LoginPasswordException {
        try {
            
            // Récupère le login/mot de passe dans les préférences :
            SharedPreferences   Prefs       = PreferenceManager.getDefaultSharedPreferences(aContext);
                                mLogin      = Prefs.getString(SJLB.PREFS.LOGIN,    "");
            String              password    = Prefs.getString(SJLB.PREFS.PASSWORD, "");
        
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
           
                // Compléte au besoin par des 0
                while (mPasswordMD5.length() < 32) {
                    mPasswordMD5 = "0" + mPasswordMD5;
                }
            }
            else
            {
				// Lève une exception dédiée lorsque login/mdp ne sont pas renseignés !
				throw new LoginPasswordException ();
            }
        } catch (NoSuchAlgorithmException e) {  
            e.printStackTrace();  
        }
    }
    
    /**
     * Regarde si les préférences contienent login/mdp
     *
     * @param[in] aContext Contexte de l'activité/du servicé, nécessaire pour récupérer les préférences de l'application
     *
     * @return boolean true si login ET mdp sont renseignés
     */
    static public boolean AreFilled (Context aContext) {
            // Récupère le login/mot de passe dans les préférences :
            SharedPreferences   Prefs       = PreferenceManager.getDefaultSharedPreferences(aContext);
            String              login       = Prefs.getString(SJLB.PREFS.LOGIN,    "");
            String              password    = Prefs.getString(SJLB.PREFS.PASSWORD, "");
            
            return (   (false == login.contentEquals(""))
                    && (false == password.contentEquals("")) );
    }
}