package fr.srombauts.sjlb.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


/**
 * Service chargé de traiter toutes les requêtes réseau en tâche de fond,
 * en particulier la tâche de rafraîchissement périodique (polling),
 * mais aussi l'envoi l'édition ou la suppression de messages.
 * 
 * @author SRombauts
 */
public class ServiceSJLB extends IntentService {
    private static final String LOG_TAG = "SJLBService";
    
    /**
     * Actions que les activités peuvent demander au service
    */
    public static final String  ACTION_REFRESH      = "fr.srombauts.sjlb.ACTION_REFRESH";
    public static final String  ACTION_SEND_MSG     = "fr.srombauts.sjlb.ACTION_SEND_MSG";
    public static final String  ACTION_EDIT_MSG     = "fr.srombauts.sjlb.ACTION_EDIT_MSG";
    public static final String  ACTION_DEL_MSG      = "fr.srombauts.sjlb.ACTION_DEL_MSG";
    public static final String  ACTION_SEND_PM      = "fr.srombauts.sjlb.ACTION_SEND_PM";
    public static final String  ACTION_DEL_PM       = "fr.srombauts.sjlb.ACTION_DEL_PM";

    /**
     * Constructeur : nomme le thread de travail
     */
    public ServiceSJLB() {
        super("ServiceSJLB");
        Log.d(LOG_TAG, "ServiceSJLB");
    }    

    /**
     * Exécution d'une des actions demandées
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "onHandleIntent(" + intent + ")");
        
        // TODO SRombauts : implémenter en fonction de l'Intent :
        // - signaler la liste des messages lus localement,
        // - poster un éventuel nouveau message
        // - lancer une récupération de la liste des messages non lus
        final String action = intent.getAction();
        Log.d(LOG_TAG, "action = " + intent.getAction());
        if (   (action == ACTION_REFRESH)
            || (action == null) ) {
            // TODO SRombauts : mettre au propre
            TaskRefresh Refresh = new TaskRefresh(this);
            Refresh.doInBackground();
            
            Log.e(LOG_TAG, "notification?");
            Refresh.notifyUser();
        }
        
        // TODO SRombauts : test d'un moyen d'interragir avec l'IHM
        // Toast notification INTERDIT car on tourne dans un thread en tache de fond
        // Toast.makeText(this, "test", Toast.LENGTH_SHORT).show();
        //Intent intentRefresh = new Intent();
        //this.sendBroadcast(intentRefresh);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO SRombauts : à implémenter
        Log.e(LOG_TAG, "onBind");
        return null;
    }
}