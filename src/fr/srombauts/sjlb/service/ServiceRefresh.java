package fr.srombauts.sjlb.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import fr.srombauts.sjlb.db.SJLB;


/**
 * Service en tâche de fond, chargé de lancer périodiquement la tâche de rafraîchissement
 * @author SRombauts
 */
// TODO SRombauts : utiliser à la place un IntentService gérant une file d'Intent
public class ServiceRefresh extends Service {
    private static final String  LOG_TAG = "SJLBService";

    private AsynchTaskRefresh   mRefreshTask    = null;

    /**
     * Lancement de l'alarme périodique : fait ici une fois pour toute (et non pas à chaque onStartCommand)
     */
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate");
        
        // Récupère dans les préférences les valeurs de rafraîchissement  :
        SharedPreferences   prefs           = PreferenceManager.getDefaultSharedPreferences(this);
        final Boolean       bAutoUpdate     = prefs.getBoolean(SJLB.PREFS.AUTO_UPDATE,      true);
        if (bAutoUpdate)
        {
            final String    freqUpdate      = prefs.getString(SJLB.PREFS.UPDATE_FREQ,       "900");   // 15 min
            final long      freqUpdateMs    = Long.parseLong(freqUpdate) * 1000;
            
            final String ALARM_ACTION = IntentReceiverStartService.ACTION_REFRESH_ALARM;
            final Intent intentToFire = new Intent(ALARM_ACTION);

            final PendingIntent   mAlarmIntent    = PendingIntent.getBroadcast(this, 0, intentToFire, 0);
            final AlarmManager    mAlarmManager   = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

            // Lancement de l'alarme périodique  :
            // - imprécise car plus économique, /*AlarmManager.INTERVAL_FIFTEEN_MINUTES == 900000*/
            // - et pas ELAPSED_REALTIME_WAKEUP car ainsi n'utilise que les réveils demandés par d'autres applis (gmail...)
            mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, freqUpdateMs, freqUpdateMs, mAlarmIntent);
        }
    }    

    /**
     * Exécution de la tache de rafraîchissement
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand(" + intent + "," + flags + "," + startId + ")");
        
        // Refresh : création et lancement d'une nouvelle tâche de fond ssi jamais créée ou précédente terminée 
        if (   (mRefreshTask == null)
            || (mRefreshTask.getStatus() == AsyncTask.Status.FINISHED))
        {
            // lance un rafraîchissement de la liste des messages non lus
            mRefreshTask = new AsynchTaskRefresh(this);
            mRefreshTask.execute((Void) null);
        }
        
        // Le service peut être arrêter dès que le rafraîchissement est terminé, l'alarme le réveillera régulièrement
        return Service.START_NOT_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");

        // Refresh : création et lancement d'une nouvelle tâche de fond ssi jamais créée ou précédente terminée 
        if (mRefreshTask != null)
        {
            if (mRefreshTask.getStatus() == AsyncTask.Status.RUNNING) {
                mRefreshTask.cancel(true);
            }
            mRefreshTask = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }
}
