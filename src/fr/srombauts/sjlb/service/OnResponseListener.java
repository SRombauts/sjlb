package fr.srombauts.sjlb.service;

import android.content.Intent;

/**
 * Listener à implémenter pour utiliser le ResponseReceiver (gestionnaire d'abonnements aux réponses du service SJLB)
 * 
 * @see ResponseReceiver
 * 
 * @author SRombauts
 */
public interface OnResponseListener {

    /**
     * Sur réception d'une réponse du service SJLB
     * 
     * @param aIntent Informations sur le type d'action traitée et le résultat obtenu
     */
    public void onServiceResponse(Intent aIntent);
}
