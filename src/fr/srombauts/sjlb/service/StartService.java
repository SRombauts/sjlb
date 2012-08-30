package fr.srombauts.sjlb.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Fonctions utilitaires permettant de transmettre une consigne au service.
 * @author SRombauts
 */
public class StartService {
    final static String LOG_TAG = "StartService";

    /**
     * Transmet au service une consigne (un Intent) de refresh des données.
     * 
     * Lance le service s'il n'est pas déjà lancé, sinon ne fait qu'ajouter le refresh à sa fifo d'Intent
     */
    public static void refresh (Context context) {
        Intent intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.service.ServiceSJLB");
        intentService.setAction(ServiceSJLB.ACTION_REFRESH);
        ComponentName cname = context.startService(intentService);
        if (cname == null) {
            Log.e(LOG_TAG, "refresh: SJLB Service was not started");
        }
    }

    /**
     * Transmet au service une consigne (un Intent) de post d'un nouveau message du forum.
     * 
     * Lance le service s'il n'est pas déjà lancé, sinon ne fait qu'ajouter le message à sa fifo d'Intent
     * 
     * @param aSubjectId    ID du sujet dans lequel poster le message
     * @param aText         Contenu du message à poster
     */
    public static void newMsg (Context context, long aSubjectId, String aText) {
        Intent intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.service.ServiceSJLB");
        intentService.setAction(ServiceSJLB.ACTION_NEW_MSG);
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_SUBJ_ID,  Long.toString(aSubjectId));
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_TEXT,     aText);
        ComponentName cname = context.startService(intentService);
        if (cname == null) {
            Log.e(LOG_TAG, "sendMsg: SJLB Service was not started");
        }
    }
    
    /**
     * Transmet au service une consigne (un Intent) de post d'un nouveau message du forum.
     * 
     * Lance le service s'il n'est pas déjà lancé, sinon ne fait qu'ajouter le message à sa fifo d'Intent
     * 
     * @param aMsgId    ID du message à mettre à jour
     * @param aText     Nouveau contenu du message à modifier
     * @param aEditText Raison de l'édition du message
     */
    public static void editMsg (Context context, long aMsgId, String aText, String aEditText) {
        Intent intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.service.ServiceSJLB");
        intentService.setAction(ServiceSJLB.ACTION_EDIT_MSG);
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_MSG_ID,   Long.toString(aMsgId));
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_TEXT,     aText);
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_EDIT_TEXT,aEditText);
        ComponentName cname = context.startService(intentService);
        if (cname == null) {
            Log.e(LOG_TAG, "sendMsg: SJLB Service was not started");
        }
    }
    
    /**
     * Transmet au service une consigne (un Intent) de delete d'un message.
     * 
     * Lance le service s'il n'est pas déjà lancé, sinon ne fait qu'ajouter l'id du message à effacer à sa fifo d'Intent
     * 
     * @param aMsgId    ID du message à supprimer
     */
    public static void delMsg (Context context, long aMsgId) {
        Intent intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.service.ServiceSJLB");
        intentService.setAction(ServiceSJLB.ACTION_DEL_MSG);
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_MSG_ID,  Long.toString(aMsgId));
        ComponentName cname = context.startService(intentService);
        if (cname == null) {
            Log.e(LOG_TAG, "sendMsg: SJLB Service was not started");
        }
    }

    /**
     * Transmet au service une consigne (un Intent) de post d'un nouveau PM.
     * 
     * Lance le service s'il n'est pas déjà lancé, sinon ne fait qu'ajouter le PM à sa fifo d'Intent
     * 
     * @param aDestId   ID du membre destinataire du PM
     * @param aText     Contenu du PM à poster
     */
    public static void newPM (Context context, long aDestId, String aText) {
        Intent intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.service.ServiceSJLB");
        intentService.setAction(ServiceSJLB.ACTION_NEW_PM);
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_DEST_ID,  Long.toString(aDestId));
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_TEXT,     aText);
        ComponentName cname = context.startService(intentService);
        if (cname == null) {
            Log.e(LOG_TAG, "sendMsg: SJLB Service was not started");
        }
    }

    /**
     * Transmet au service une consigne (un Intent) de delete d'un PM.
     * 
     * Lance le service s'il n'est pas déjà lancé, sinon ne fait qu'ajouter l'id du PM à effacer à sa fifo d'Intent
     * 
     * @param aPmId ID du PM à supprimer
     */
    public static void delPM (Context context, long aPmId) {
        Intent intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.service.ServiceSJLB");
        intentService.setAction(ServiceSJLB.ACTION_DEL_PM);
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_PM_ID,  Long.toString(aPmId));
        ComponentName cname = context.startService(intentService);
        if (cname == null) {
            Log.e(LOG_TAG, "sendMsg: SJLB Service was not started");
        }
    }
    /**
     * Transmet au service une consigne (un Intent) de delete de tous les PM.
     * 
     * Lance le service s'il n'est pas déjà lancé, sinon ne fait qu'ajouter la demande d'affacement de tous les pm à sa fifo d'Intent
     */
    public static void delAllPM (Context context) {
        Intent intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.service.ServiceSJLB");
        intentService.setAction(ServiceSJLB.ACTION_DEL_PM);
        intentService.putExtra(ServiceSJLB.START_INTENT_EXTRA_PM_ID,  "all");
        ComponentName cname = context.startService(intentService);
        if (cname == null) {
            Log.e(LOG_TAG, "sendMsg: SJLB Service was not started");
        }
    }
}
