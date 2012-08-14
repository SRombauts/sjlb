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
    private static final String  LOG_TAG = "SJLBService";

    /**
     * Lancement de l'alarme périodique : fait dans le constructeur une fois pour toute (et non pas à chaque onStartCommand)
     */
    public ServiceSJLB() {
        super("ServiceSJLB");
        Log.d(LOG_TAG, "ServiceSJLB");
    }    

    /**
     * Exécution de la tache de rafraîchissement
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "onHandleIntent(" + intent + ")");

        // TODO SRombauts : implémenter en fonction de l'Intent :
        // - signaler la liste des messages lus localement,
        // - poster un éventuel nouveau message
        // - lancer une récupération de la liste des messages non lus
        AsynchTaskRefresh Asynch = new AsynchTaskRefresh(this);
        Asynch.doInBackground();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO SRombauts : à implémenter
        Log.e(LOG_TAG, "onBind");
        return null;
    }
}