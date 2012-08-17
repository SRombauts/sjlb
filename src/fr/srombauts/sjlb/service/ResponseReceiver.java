package fr.srombauts.sjlb.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * BroadcastReceiver gestionnaire des abonnements aux réponses du service SJLB
 * 
 * Destiné à ce que les Activity rafraîchissent leur affichage dès la réponse reçue. 
 * 
 * @author SRombauts
 */
public class ResponseReceiver extends BroadcastReceiver {
    OnResponseListener mOnServiceResponseListener = null;
    
    /**
     * Enregistre le BroadcastReceiver sur ACTION_RESPONSE et l'associe à un Listener de l'Activity qui souhaite être notifié
     * 
     * @param aResponseListener Contexte implémentant le listener recevant les notifications
     */
    public ResponseReceiver(OnResponseListener aResponseListener) {
        mOnServiceResponseListener = aResponseListener;
        IntentFilter filter = new IntentFilter(ServiceSJLB.ACTION_RESPONSE);
        ((Context)aResponseListener).registerReceiver(this, filter);
    }
    
    /**
     * Désenregistre le BroadcastReceiver et le OnResponseListener
     * 
     * @param aContext  Contexte de désenregistrement du BroadcastReceiver
     */
    public void unregister(Context aContext) {
        aContext.unregisterReceiver(this);
        mOnServiceResponseListener = null;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
         mOnServiceResponseListener.onServiceResponse(intent);
    }
}
