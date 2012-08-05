package fr.srombauts.sjlb;

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


/**
 * Service en tâche de fond, chargé de lancer périodiquement la tâche de rafraîchissement
 * @author SRombauts
 */
// TODO SRombauts : utiliser à la place un IntentService gérant une file d'Intent
public class ServiceRefresh extends Service {
    private static final String  LOG_TAG = "SJLBService";

    private AsynchTaskRefresh   mRefreshTask    = null;

    private AlarmManager        mAlarmManager   = null;
    private PendingIntent       mAlarmIntent    = null;    

    /**
     * Lancement de l'alarme périodique
     */
    public void onCreate() {
        // Récupère dans les préférences les valeurs de rafraîchissement  :
        SharedPreferences   prefs           = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean             bAutoUpdate     = prefs.getBoolean(SJLB.PREFS.AUTO_UPDATE,      true);
        String              freqUpdate      = prefs.getString(SJLB.PREFS.UPDATE_FREQ,       "900");   // 15 min
        long                freqUpdateMs    = Long.parseLong(freqUpdate) * 1000;
        
        String ALARM_ACTION = IntentReceiverStartService.ACTION_REFRESH_ALARM;
        Intent intentToFire = new Intent(ALARM_ACTION);
        mAlarmIntent    = PendingIntent.getBroadcast(this, 0, intentToFire, 0);
        mAlarmManager   = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        // Lancement de l'alarme périodique  :
        // - imprécise car plus économique, /*AlarmManager.INTERVAL_FIFTEEN_MINUTES == 900000*/
        // - et pas ELAPSED_REALTIME_WAKEUP car ainsi n'utilise que les réveils  demandés par d'autres applis (gmail...)
        if (false == bAutoUpdate)
        {
            // Si pas d'update automatique, l'alarme n'est pas répétée
            freqUpdateMs = 0;
        }
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, freqUpdateMs, mAlarmIntent);
    }    

    /**
     * Exécution de la tache de rafraîchissement
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");
        
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
    }

    @Override
    public IBinder onBind(Intent intent) {
         return null;
    }
}
