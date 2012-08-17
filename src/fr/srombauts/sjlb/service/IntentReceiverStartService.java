package fr.srombauts.sjlb.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import fr.srombauts.sjlb.db.SJLB;


/**
 * BroadcastReceiver chargé de lancer automatiquement le service de notification des nouveaux messages SJLB
 * 
 * Lance le service :
 * - sur réception de l'intent de fin de boot
 * - sur réception de l'intent d'alarme périodique
 * 
 * @author 10/06/2010 SRombauts
 */
public class IntentReceiverStartService extends BroadcastReceiver {
    public  static final String ACTION_REFRESH_ALARM = "fr.srombauts.sjlb.ACTION_REFRESH_ALARM";

    private static final String LOG_TAG = "StartServiceIntentReceiver";

    /**
     * Callback de réception des Intent
     */
    public void onReceive(Context context, Intent intent) {
        // Récupère l'action transmise par l'intent
        String action = intent.getAction();        
        Log.d(LOG_TAG, action);
        if (action == null ) {
            Log.e(LOG_TAG,"Action==null!");
        }
        else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            // lance l'alarme périodique si l'action correspond au boot
            startAlarm(context, LOG_TAG);
        }
        else if (ACTION_REFRESH_ALARM.equals(action)) {
            // lance le service si l'action correspond à l'alarme périodique
            StartService.refresh(context);
        }
    }

    /**
     * Lance l'alarme périodique, si pas déjà lancé
     */
    public static void startAlarm (Context context, String aLogTag) {
        // Récupère dans les préférences les valeurs de rafraîchissement  :
        SharedPreferences   prefs           = PreferenceManager.getDefaultSharedPreferences(context);
        final Boolean       bAutoUpdate     = prefs.getBoolean(SJLB.PREFS.AUTO_UPDATE,      true);
        if (bAutoUpdate) {
            final String    freqUpdate      = prefs.getString(SJLB.PREFS.UPDATE_FREQ,       "900");   // 15 min
            final long      freqUpdateMs    = Long.parseLong(freqUpdate) * 1000;
            
            final String ALARM_ACTION = IntentReceiverStartService.ACTION_REFRESH_ALARM;
            final Intent intentToFire = new Intent(ALARM_ACTION);

            final PendingIntent   mAlarmIntent    = PendingIntent.getBroadcast(context, 0, intentToFire, 0);
            final AlarmManager    mAlarmManager   = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

            Log.d(aLogTag, "startAlarm...");
            
            // Lancement de l'alarme périodique  :
            // - imprécise car plus économique, AlarmManager.INTERVAL_FIFTEEN_MINUTES == 900000
            // - et pas ELAPSED_REALTIME_WAKEUP car ainsi n'utilise que les réveils demandés par d'autres applis (gmail...)
            mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, freqUpdateMs, mAlarmIntent);
        } else {
            Log.d(aLogTag, "startAlarm(nop)");
        }
    }
}