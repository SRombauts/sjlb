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
    public static final String  ACTION_NEW_MSG      = "fr.srombauts.sjlb.ACTION_NEW_MSG";
    public static final String  ACTION_EDIT_MSG     = "fr.srombauts.sjlb.ACTION_EDIT_MSG";
    public static final String  ACTION_DEL_MSG      = "fr.srombauts.sjlb.ACTION_DEL_MSG";
    public static final String  ACTION_NEW_PM       = "fr.srombauts.sjlb.ACTION_NEW_PM";
    public static final String  ACTION_DEL_PM       = "fr.srombauts.sjlb.ACTION_DEL_PM";

    public static final String  ACTION_RESPONSE     = "fr.srombauts.sjlb.ACTION_RESPONSE";
    
    public static final String  START_INTENT_EXTRA_MSG_ID       = "MessageId";
    public static final String  START_INTENT_EXTRA_SUBJ_ID      = "SubjectId";
    public static final String  START_INTENT_EXTRA_TEXT         = "Text";
    public static final String  START_INTENT_EXTRA_EDIT_TEXT    = "EditText";
    public static final String  START_INTENT_EXTRA_DEST_ID      = "DestId";
    public static final String  START_INTENT_EXTRA_PM_ID        = "PmId";
    
    public static final String  RESPONSE_INTENT_EXTRA_TYPE      = "Type";
    public static final String  RESPONSE_INTENT_EXTRA_RESULT    = "Result";


    private API mAPI = new API(this);
    
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

        boolean bSuccess = false;
        
        // TODO SRombauts : implémenter en fonction de l'Intent :
        // - signaler la liste des messages lus localement,
        // - poster un éventuel nouveau message
        // - lancer une récupération de la liste des messages non lus
        final String action = intent.getAction();
        if (action.equals(ACTION_REFRESH)) {
            bSuccess = mAPI.refresh();
        }
        else if (action.equals(ACTION_NEW_MSG)) {
            final String msgSubjId  = intent.getExtras().getString(START_INTENT_EXTRA_SUBJ_ID);
            final String msgText    = intent.getExtras().getString(START_INTENT_EXTRA_TEXT);
            bSuccess = mAPI.newMsg(msgSubjId, msgText);
        }
        else if (action.equals(ACTION_EDIT_MSG)) {
            final String msgId      = intent.getExtras().getString(START_INTENT_EXTRA_MSG_ID);
            final String msgText    = intent.getExtras().getString(START_INTENT_EXTRA_TEXT);
            final String msgEditText= intent.getExtras().getString(START_INTENT_EXTRA_EDIT_TEXT);
            bSuccess = mAPI.editMsg(msgId, msgText, msgEditText);
        }
        else if (action.equals(ACTION_DEL_MSG)) {
            final String msgId      = intent.getExtras().getString(START_INTENT_EXTRA_MSG_ID);
            bSuccess = mAPI.delMsg(msgId);
        }
        else if (action.equals(ACTION_NEW_PM)) {
            final String pmDestId   = intent.getExtras().getString(START_INTENT_EXTRA_DEST_ID);
            final String pmText     = intent.getExtras().getString(START_INTENT_EXTRA_TEXT);
            bSuccess = mAPI.newPM(pmDestId, pmText);
        }
        else if (action.equals(ACTION_DEL_PM)) {
            final String pmId       = intent.getExtras().getString(START_INTENT_EXTRA_PM_ID);
            bSuccess = mAPI.delPM(pmId);
        }
        else {
            Log.e(LOG_TAG, "FIXME action=" + action);
        }

        // Formulation d'un intent de réponse à destination de l'activité ayant la main
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_RESPONSE);
        //broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_INTENT_EXTRA_TYPE,    action);    // le type de réponse indique l'action à laquelle on répond 
        broadcastIntent.putExtra(RESPONSE_INTENT_EXTRA_RESULT,  bSuccess);  // résultat de l'action
        Log.d(LOG_TAG, "sendBroadcast(" + broadcastIntent + ", " + action + ", " + bSuccess + ")");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO SRombauts : à implémenter
        Log.e(LOG_TAG, "onBind");
        return null;
    }
}