package fr.srombauts.sjlb;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;


/**
 * Service en tâche de fond, chargé de lancer périodirquement la tâche de rafraichissement
 * @author srombauts
 */
public class RefreshService extends Service {
    private static final String  LOG_TAG = "SJLBService";

    private RefreshTask     mRefreshTask    = null;

    private AlarmManager    mAlarmManager   = null;
    private PendingIntent   mAlarmIntent    = null;    

    /**
     * Lancement de l'alarme périodique
     */
    public void onCreate() {
        String ALARM_ACTION = StartServiceIntentReceiver.ACTION_REFRESH_ALARM;
        Intent intentToFire = new Intent(ALARM_ACTION);
        mAlarmIntent    = PendingIntent.getBroadcast(this, 0, intentToFire, 0);
        mAlarmManager   = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        // Lancement de l'alarme périodique  :
        // - imprécise car plus économique,
        // - et pas ELAPSED_REALTIME_WAKEUP car ainsi n'utilise que les réveils  demandés par d'autres applis (gmail...)
        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, AlarmManager.INTERVAL_FIFTEEN_MINUTES, mAlarmIntent);
    }    

    /**
     * Exécution de la tache de rafraichissement
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");
        
        // Refresh : création et lancement d'une nouvelle tâche de fond ssi jamais créée ou précédente terminée 
        if (   (mRefreshTask == null)
            || (mRefreshTask.getStatus() == AsyncTask.Status.FINISHED))
        {
            // lance un rafraichissement de la liste des messages non lus
            mRefreshTask = new RefreshTask(this);
            mRefreshTask.execute((Void[]) null);
        }
        
        // Le service peut être arrêter dès que le rafraichissement est terminé, l'alarme le réveillera régulièrement
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
