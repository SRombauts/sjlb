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
    public void onResponseListener(Intent intent);
}
