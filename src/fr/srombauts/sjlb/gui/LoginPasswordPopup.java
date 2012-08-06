package fr.srombauts.sjlb.gui;

import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.R.string;
import fr.srombauts.sjlb.db.SJLB;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class LoginPasswordPopup {
    private static final String LOG_TAG                 = "LoginPasswordPopup";
    
    private Activity            mActivity;
 
    public LoginPasswordPopup(Activity context) {
        mActivity = context;
    }
 
    public void show() {
        // Récupère le login/mot de passe dans les préférences :
        final SharedPreferences prefs       = PreferenceManager.getDefaultSharedPreferences(mActivity);
        String                  login       = prefs.getString(SJLB.PREFS.LOGIN,    "");
        String                  password    = prefs.getString(SJLB.PREFS.PASSWORD, "");

        if (   (true == login.contentEquals(""))
            || (true == password.contentEquals("")) ) {
            Log.w(LOG_TAG, "login/password are not filled");

            final String title   = mActivity.getString(R.string.loginscreen_title);
            final String message = mActivity.getString(R.string.loginscreen_message);
 
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
 
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
            builder.create().show();
        } else {
            Log.i(LOG_TAG, "login/password are filled");
        }
    }
 
}