package fr.srombauts.sjlb.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


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
        // lance le service si l'action correspond à l'un des intents attendus
        else if (   ("android.intent.action.BOOT_COMPLETED".equals(action))
                 || (ACTION_REFRESH_ALARM.equals(action)) )
        {
            startService(context, LOG_TAG);
        }
    }

    /**
     * Lance le service de rafraîchissement, si pas déjà lancé
     */
    // TODO SRombauts : rendre privée et encapsuler par des méthodes publiques précisant la demande (refresh, send...) 
    public static void startService (Context context, String aLogTag) {
        Intent  intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.service.ServiceRefresh");
        ComponentName cname = context.startService(intentService);
        if (cname == null)
            Log.e(aLogTag, "SJLB Service was not started");
        else
            Log.d(aLogTag, "SJLB Service started");
    }
}