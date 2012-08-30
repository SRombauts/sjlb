package fr.srombauts.sjlb.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ParseException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import fr.srombauts.sjlb.ApplicationSJLB;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.ContentProviderFile;
import fr.srombauts.sjlb.db.ContentProviderMsg;
import fr.srombauts.sjlb.db.ContentProviderPM;
import fr.srombauts.sjlb.db.ContentProviderSubj;
import fr.srombauts.sjlb.db.ContentProviderUser;
import fr.srombauts.sjlb.gui.ActivityMain;
import fr.srombauts.sjlb.gui.ActivityPrivateMessages;
import fr.srombauts.sjlb.model.AttachedFile;
import fr.srombauts.sjlb.model.ForumMessage;
import fr.srombauts.sjlb.model.ForumSubject;
import fr.srombauts.sjlb.model.LoginPasswordEmptyException;
import fr.srombauts.sjlb.model.PrefsLoginPassword;
import fr.srombauts.sjlb.model.PrefsNotification;
import fr.srombauts.sjlb.model.PrivateMessage;
import fr.srombauts.sjlb.model.User;


/**
 * Appelée par le service en tâche de fond pour récupérer les données utiles depuis le fichier XML "API.php"
 *
 * Ce un fichier XML est quasi-vide tant qu'il n'y a pas de nouveautés à récupérées,
 * de manière à ne pas charger le réseau inutilement.
 *
 * Liste des arguments à fournir en entrée, selon le type de requête :
Obligatoire pour toute requête :
"login"              : login (pseudo) de l'utilisateur
"password"           : password (encrypté en MD5) de l'utilisateur
"date_first_msg"     : date du plus vieux message déjà récupéré
"date_last_msg"      : date du plus récent message récupéré
"id_last_pm"         : id de pm le plus élevé déjà récupéré (dernier pm reçu)
"date_last_user"     : valeur du champ 'DateDerniereMaj' la plus récente (dernière maj du user reçue)
"date_last_msg_suppr": date du dernier message supprimé
"date_last_pm_suppr" : date du dernier pm supprimé

Post d'un nouveau message :
"id_subj_new" : id du sujet où poster le nouveau message
"msg_new"     : texte du message (doit être non vide)

Edition d'un message existant :
"id_msg_edit" : id du message à éditer
"msg_edit"    : nouveau texte du message (doit être non vide)
"raison_edit" : texte optionnel expliquant la raison de l'édition

Suppression d'un message existant :
"id_msg_del" : id du message à supprimer

Post d'un nouveau PM :
"id_dest_pm" : id du destinataire du pm
"pm_new"     : texte du pm

Suppression d'un PM existant :
"id_pm_del" : id du pm à supprimer
 
Effacement des flags "nouveaux messages" :
"msg_lus" : liste d'id de messages lus sur l'application mobile, séparés par des virgules 

Transmission des informations sur le terminal mobile et la version de l'application utilisée :
"model"
"brand"
"android"
"api"
"appli"

 
 * Voici un exemple de fichier XML produit pour
 * - 3 id de messages flagués unread sur le site 
 * - 1 message
 * - avec 1 fichier attaché
 * - provenant de 1 sujet,
 * - et 1 message privé
 * - et 1 maj des info d'un utilisateur :
 
<?xml version="1.0" encoding="utf-8"?>
<sjlb>
<sujet id="127" id_categorie="1" id_groupe="2" derniere_date="1345462107">Infos boulot, taf, exams... berk :(</sujet>
<unread id="20530"/>
<unread id="20531"/>
<unread id="20532"/>
<msg_suppr id="17670" date="1345645432"/>
<pm_suppr id="6473" date="1345645432"/>
<msg id="20501" id_auteur="10" date="1345462107" date_edit="0" id_sujet="127" unread="0">Je confirme !!!

Laura<fichier>ImageXxx.png</fichier></msg>
<pm id="6457" date="1345405384" id_auteur="2" id_destinataire="3">As tu eu ma réponse ?</pm>
<user id="8" pseudo="Amélie" nom="Amélie Rombauts" DateDerniereMaj="1345132117"><address>1 rue des roses
92260 Fontenay-aux-Roses</address><notes>RER sortie 2 - rue robert marchand (il faut traverser les voies). 
Se mettre vers l'avant de la rame, genre 3ème wagon, c'est l'idéal.</notes></user>
</sjlb>

 *
 * @author 14/06/2010 SRombauts
 */
public class API {
    private static final String  LOG_TAG                    = "NetworkAPI";

    public static final int     NOTIFICATION_NEW_PM_ID      = 1;
    public static final int     NOTIFICATION_NEW_MSG_ID     = 2;

    static final private String API_URI                     = "http://www.sjlb.fr/Android/API.php";
    
    ////////////////////////////////////////////////
    // Paramètres POST :
    // Paramètres obligatoire pour toute requête :
    static final private String PARAM_LOGIN                 = "login";              // login (pseudo) de l'utilisateur
    static final private String PARAM_PASSWORD              = "password";           // password (encrypté en MD5) de l'utilisateur
    static final private String PARAM_DATE_FIRST_MSG        = "date_first_msg";     // date du plus vieux message déjà récupéré
    static final private String PARAM_DATE_LAST_MSG         = "date_last_msg";      // date du plus récent message récupéré
    static final private String PARAM_ID_LAST_PM            = "id_last_pm";         // id de pm le plus élevé déjà récupéré (dernier pm reçu)
    static final private String PARAM_DATE_LAST_USER        = "date_last_user";     // valeur du champ 'DateDerniereMaj' la plus récente (dernière maj du user reçue)
    static final private String PARAM_DATE_LAST_MSG_SUPPR   = "date_last_msg_suppr";// date du dernier message supprimé
    static final private String PARAM_DATE_LAST_PM_SUPPR    = "date_last_pm_suppr"; // date du dernier pm supprimé
    // Post d'un nouveau message :
    static final private String PARAM_MSG_NEW_ID_SUBJ       = "id_subj_new";    // id du sujet où poster le nouveau message
    static final private String PARAM_MSG_NEW_TEXT          = "msg_new";     // texte du message (doit être non vide)

    // Edition d'un message existant :
    static final private String PARAM_MSG_EDIT_ID           = "id_msg_edit"; // id du message à éditer
    static final private String PARAM_MSG_EDIT_TEXT         = "msg_edit";    // nouveau texte du message (doit être non vide)
    static final private String PARAM_MSG_EDIT_RAISON       = "raison_edit"; // texte optionnel expliquant la raison de l'édition

    // Suppression d'un message existant :
    static final private String PARAM_MSG_DEL_ID            = "id_msg_del"; // id du message à supprimer

    // Post d'un nouveau PM :
    static final private String PARAM_PM_NEW_DEST_ID        = "id_dest_pm"; // id du destinataire du nouveau pm
    static final private String PARAM_PM_NEW_TEXT           = "pm_new";     // texte du pm à créer

    // Suppression d'un PM existant :
    static final private String PARAM_PM_DEL_ID             = "id_pm_del";  // id du pm à supprimer
     
    // Effacement des flags "nouveaux messages" :
    static final private String PARAM_LIST_MSG_LUS          = "msg_lus";    // liste d'id de messages lus sur l'application mobile, séparés par des virgules

    // Paramètres d'information sur le terminal
    static final private String PARAM_PHONE_MODEL           = "model";  // modèle du terminal : systématique pour les logs serveur
    static final private String PARAM_BUILD_BRAND           = "brand";
    static final private String PARAM_VERSION_ANDROID       = "android";
    static final private String PARAM_API_LEVEL             = "api";
    static final private String PARAM_VERSION_APPLI         = "appli";

    /////////////////////////////
    // Réponse XML
    // Noeuds
    static final private String NODE_NAME_BAD_LOGIN         = "login_error";
    static final private String NODE_NAME_FORUM_UNREAD      = "unread";
    static final private String NODE_NAME_FORUM_SUPPR       = "msg_suppr";
    static final private String NODE_NAME_PRIVATE_SUPPR     = "pm_suppr";
    static final private String NODE_NAME_PRIVATE_MSG       = "pm";
    static final private String NODE_NAME_FORUM_MSG         = "msg";
    static final private String NODE_NAME_FORUM_FILE        = "fichier";
    static final private String NODE_NAME_FORUM_SUBJ        = "sujet";
    static final private String NODE_NAME_USER              = "user";
    static final private String NODE_NAME_FORUM_ADDRESS     = "address";
    static final private String NODE_NAME_FORUM_NOTES       = "notes";
    // Attributs "pm"
    static final private String ATTR_NAME_PRIVATE_MSG_ID        = "id";
    static final private String ATTR_NAME_PRIVATE_MSG_DATE      = "date";
    static final private String ATTR_NAME_PRIVATE_MSG_AUTHOR_ID = "id_auteur";
    static final private String ATTR_NAME_PRIVATE_MSG_DEST_ID   = "id_destinataire";
    static final private String ATTR_NAME_PRIVATE_MSG_DATE_SUPPR= "date";
    // Attributs "sujets"
    static final private String ATTR_NAME_FORUM_SUBJ_ID             = "id";
    static final private String ATTR_NAME_FORUM_SUBJ_CAT_ID         = "id_categorie";
    static final private String ATTR_NAME_FORUM_SUBJ_GROUP_ID       = "id_groupe";
    static final private String ATTR_NAME_FORUM_SUBJ_DERNIERE_DATE  = "derniere_date";
    // Attributs "messages"
    static final private String ATTR_NAME_FORUM_MSG_ID          = "id";
    static final private String ATTR_NAME_FORUM_MSG_AUTHOR_ID   = "id_auteur";
    static final private String ATTR_NAME_FORUM_MSG_DATE        = "date";
    static final private String ATTR_NAME_FORUM_MSG_DATE_EDIT   = "date_edit";
    static final private String ATTR_NAME_FORUM_MSG_DATE_SUPPR  = "date";
    static final private String ATTR_NAME_FORUM_MSG_SUBJECT_ID  = "id_sujet";
    static final private String ATTR_NAME_FORUM_MSG_UNREAD      = "unread";
    // Attributs "users"
    static final private String ATTR_NAME_USER_ID               = "id";
    static final private String ATTR_NAME_USER_PSEUDO           = "pseudo";
    static final private String ATTR_NAME_USER_NAME             = "nom";
    static final private String ATTR_NAME_USER_DATE_MAJ         = "DateDerniereMaj";
    static final private String ATTR_NAME_USER_IS_ACTIVE        = "is_active";

    private ServiceSJLB             mContext        = null;
    
    private ContentProviderPM       mPMDBAdapter    = null;
    private ContentProviderSubj     mSubjDBAdapter  = null;
    private ContentProviderMsg      mMsgDBAdapter   = null;
    private ContentProviderFile     mFileDBAdapter  = null;
    private ContentProviderUser     mUserDBAdapter  = null;
    
    /**
     * Constructeur utilisé pour mémorisée la référence sur le service appelant
     * @param context
     */
    public API(ServiceSJLB context) {
        mContext        = context;
                                
        mPMDBAdapter    = new ContentProviderPM(context);
        mSubjDBAdapter  = new ContentProviderSubj(context);
        mMsgDBAdapter   = new ContentProviderMsg(context);
        mFileDBAdapter  = new ContentProviderFile(context);
        mUserDBAdapter  = new ContentProviderUser(context);
    }

    public boolean refresh() {
        Log.d(LOG_TAG, "refresh");
        return sendAndFetchNewContent(null, null, null, null, null, null);
    }

    public boolean newMsg(String aSubjectId, String aText) {
        Log.d(LOG_TAG, "newMsg(" + aSubjectId + ", " + aText + ")");
        return sendAndFetchNewContent(null, aSubjectId, aText, null, null, null);
    }
    
    public boolean editMsg(String aMessageId, String aText, String aEditText) {
        Log.d(LOG_TAG, "editMsg(" + aMessageId + ", " + aText + ", " + aEditText + ")");
        return sendAndFetchNewContent(aMessageId, null, aText, aEditText, null, null);
    }
    
    public boolean delMsg(String aMessageId) {
        Log.d(LOG_TAG, "delMsg(" + aMessageId + ")");
        return sendAndFetchNewContent(aMessageId, null, null, null, null, null);
    }

    public boolean newPM(String aDestId, String aText) {
        Log.d(LOG_TAG, "newPM(" + aDestId + ", " + aText + ")");
        return sendAndFetchNewContent(null, null, aText, null, aDestId, null);
    }

    public boolean delPM(String aPmId) {
        Log.d(LOG_TAG, "delPM(" + aPmId + ")");
        return sendAndFetchNewContent(null, null, null, null, null, aPmId);
    }

    /**
     * Récupération et parse de la liste XML des messages non lus, des pm non récupérés, et des infos mises à jours des utilisateurs 
     * 
     * Ce travail s'exécute en tâche de fond, et n'a donc pas le droit d'effectuer d'actions sur la GUI
     * 
     * Récupère la liste des messages flaggés comme "non lus" sur le site SJLB, c'est à dire :
     * - les nouveaux messages (bien qu'il puisent avoir déjà été récupérés par l'application mobile)
     * - les messages modifiés (qui ont déjà été récupéré, mais dont le contenu a changé entre temps)
     * 
     * Paramètres optionnels à utiliser (en combinaisons intelligentes) pour envoyer des requêtes au serveur SJLB :
     * @param aMessageId    
     * @param aSubjectId    
     * @param aText         
     * @param aEditText     
     * @param aDestId       
     * @param aPmId         
     * @return bSuccess     true si tout s'est bien passé
     */
    private boolean sendAndFetchNewContent(String aMessageId,
                                           String aSubjectId,
                                           String aText,
                                           String aEditText,
                                           String aDestId,
                                           String aPmId) {
        boolean bSuccess    = false;

        int     nbNewPM     = 0;    // Nombre d'ID de PM inconnus (issu de la liste XML de nouveaux PM)
        int     nbUnreadMsg = 0;    // Nombre de messages non lus par l'utilisateur (issu directement de la liste XML fournie par le site SJLB)
        int     nbNewMsg    = 0;    // Nombre de messages mis à jour ou ajoutés lors de cette opération de synchro avec le site SJLB
        
        try {
            Log.i(LOG_TAG, "fetchNewContent"
                    +  "(aMessageId=" + aMessageId
                    + ", aSubjectId=" + aSubjectId
                    + ", aText="      + aText
                    + ", aEditText=" + aEditText
                    + ", aDestId=" + aDestId
                    + ", aPmId=" + aPmId
                    + ")...");
            
            // Utilise les préférences pour récupérer le login/mot de passe :
            PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);

            // Récupère la date du message le plus vieux (le premier) et du plus récent (le dernier) connus
            long dateFirstMsg   = mMsgDBAdapter.getDateFirstMsg();
            long dateLastMsg    = mMsgDBAdapter.getDateLastMsg();
            
            // idem id du PM le plus récent et date de modif d'un utilisateur la plus récente
            long idLastPM           = mPMDBAdapter.getIdLastPM();
            long dateLastUpdateUser = mUserDBAdapter.getDateLastUpdateUser();
            
            // Récupère dans les préférences les dates de suppression du dernier message et du dernier PM supprimé 
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final long dateLastMsgSuppr  = prefs.getLong(PARAM_DATE_LAST_MSG_SUPPR, 0);
            final long dateLastPmSuppr   = prefs.getLong(PARAM_DATE_LAST_PM_SUPPR, 0);
            
            // Établi la liste des messages lus localement, à transmettre au site SJLB pour qu'il se mette à jour
            String strMsgLus = mMsgDBAdapter.getListMsgUnreadLocaly ();
            
            // Instancie un client HTTP et un header de requête "POST"  
            HttpClient  httpClient  = new DefaultHttpClient();  
            HttpPost    httpPost    = new HttpPost(API_URI);  
               
            // Prépare les 6 paramètres systématiques 
            // "login"          : login (pseudo) de l'utilisateur
            // "password"       : password (encrypté en MD5) de l'utilisateur
            // "date_first_msg" : date du plus vieux message déjà récupéré
            // "date_last_msg"  : date du plus récent message récupéré
            // "id_last_pm"     : id de pm le plus élevé déjà récupéré (dernier pm reçu)
            // "date_last_user" : valeur du champ 'DateDerniereMaj' la plus récente (dernière maj du user reçue)

            // commence par les 2 infos de login et le password
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(12);
            nameValuePairs.add(new BasicNameValuePair(PARAM_LOGIN,      loginPassword.getLogin()));  
            nameValuePairs.add(new BasicNameValuePair(PARAM_PASSWORD,   loginPassword.getPasswordMD5()));
            // et si disponibles (ie après la première fois) les 2 dates du plus vieux et du plus récent message
            nameValuePairs.add(new BasicNameValuePair(PARAM_DATE_FIRST_MSG, Long.toString(dateFirstMsg)));
            nameValuePairs.add(new BasicNameValuePair(PARAM_DATE_LAST_MSG,  Long.toString(dateLastMsg)));
            // idem id du PM le plus récent et date de modif d'un utilisateur la plus récente
            nameValuePairs.add(new BasicNameValuePair(PARAM_ID_LAST_PM,     Long.toString(idLastPM)));
            nameValuePairs.add(new BasicNameValuePair(PARAM_DATE_LAST_USER, Long.toString(dateLastUpdateUser)));

            // gestion des suppressions de msg et de pm 
            nameValuePairs.add(new BasicNameValuePair(PARAM_DATE_LAST_MSG_SUPPR,    Long.toString(dateLastMsgSuppr)));
            nameValuePairs.add(new BasicNameValuePair(PARAM_DATE_LAST_PM_SUPPR,     Long.toString(dateLastPmSuppr)));

            // puis l'éventuelle liste des messages lus localement
            if (0 < strMsgLus.length()) {
                nameValuePairs.add(new BasicNameValuePair(PARAM_LIST_MSG_LUS, strMsgLus));
            }

            Log.i(LOG_TAG, "fetchNewContent (" + dateFirstMsg + "," + dateLastMsg + "," + idLastPM + "," + dateLastUpdateUser + "," + dateLastMsgSuppr + "," + dateLastPmSuppr + " {" + strMsgLus + "} )");

            // y ajoute les 5 informations de version de l'équipement et de l'application...
            final String phoneModel     = prefs.getString(PARAM_PHONE_MODEL, "");
            final String buildBrand     = prefs.getString(PARAM_BUILD_BRAND, "");
            final String versionAndroid = prefs.getString(PARAM_VERSION_ANDROID, "");
            final int    apiLevel       = prefs.getInt(PARAM_API_LEVEL, 0);
            final int    versionAppli   = prefs.getInt(PARAM_VERSION_APPLI, 0);
            final int    versionCode    = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES).versionCode;
            if (   (false == phoneModel.equals(Build.MODEL))
                || (false == buildBrand.equals(Build.BRAND))
                || (false == versionAndroid.equals(Build.VERSION.RELEASE))
                || (apiLevel != Build.VERSION.SDK_INT)
                || (versionAppli != versionCode)
               )
            {
                Log.i(LOG_TAG, "SEND device: " + Build.MODEL + " (" + Build.MANUFACTURER + "/" + Build.BRAND + ") " + Build.VERSION.RELEASE + " (api_level=" + Build.VERSION.SDK_INT + ") versionCode=" + versionCode);
                // ne renvoie tout que s'il y a du nouveau !
                nameValuePairs.add(new BasicNameValuePair(PARAM_PHONE_MODEL,    Build.MODEL));
                nameValuePairs.add(new BasicNameValuePair(PARAM_BUILD_BRAND,    Build.BRAND));
                nameValuePairs.add(new BasicNameValuePair(PARAM_VERSION_ANDROID,Build.VERSION.RELEASE));
                nameValuePairs.add(new BasicNameValuePair(PARAM_API_LEVEL,      Integer.toString(Build.VERSION.SDK_INT)));
                nameValuePairs.add(new BasicNameValuePair(PARAM_VERSION_APPLI,  Integer.toString(versionCode)));
            } else {
                Log.d(LOG_TAG, "ALREADY SENT device: " + Build.MODEL + " (" + Build.MANUFACTURER + "/" + Build.BRAND + ") " + Build.VERSION.RELEASE + " (api_level=" + Build.VERSION.SDK_INT + ") versionCode=" + versionCode);
                // sinon, renvoi tout de même le modèle, pour faire du logging
                nameValuePairs.add(new BasicNameValuePair(PARAM_PHONE_MODEL,    Build.MODEL));
            }

            // TODO SRombauts : ajouter l'état de l'application (ouverte/fermée) + le nombre de messages récupérés localement

            // Prend en compte les combinaisons de paramètres optionnels lorsqu'ils sont non nuls
            if (   (null != aSubjectId)
                && (null != aText) ) {
                // Post d'un nouveau message :
                nameValuePairs.add(new BasicNameValuePair(PARAM_MSG_NEW_ID_SUBJ,aSubjectId));   // id du sujet où poster le nouveau message
                nameValuePairs.add(new BasicNameValuePair(PARAM_MSG_NEW_TEXT,   aText));        // texte du message (doit être non vide)
                Log.i(LOG_TAG, "Post d'un nouveau Msg " + PARAM_MSG_NEW_ID_SUBJ + "=" + aSubjectId + ", " + PARAM_MSG_NEW_TEXT + "=" + aText);
            }
            else if (   (null != aMessageId)
                     && (null != aText)
                     && (null != aEditText) ) {
                // Edition d'un message existant :
                nameValuePairs.add(new BasicNameValuePair(PARAM_MSG_EDIT_ID,    aMessageId));   // id du message à éditer
                nameValuePairs.add(new BasicNameValuePair(PARAM_MSG_EDIT_TEXT,  aText));        // nouveau texte du message (doit être non vide)
                nameValuePairs.add(new BasicNameValuePair(PARAM_MSG_EDIT_RAISON,aEditText));    // texte optionnel expliquant la raison de l'édition
                Log.i(LOG_TAG, "Edition d'un Msg existant " + PARAM_MSG_EDIT_ID + "=" + aMessageId + ", " + PARAM_MSG_EDIT_TEXT + "=" + aText + ", " + PARAM_MSG_EDIT_RAISON + "=" + aEditText);
            }
            else if (null != aMessageId) {
                // Suppression d'un message :
                nameValuePairs.add(new BasicNameValuePair(PARAM_MSG_DEL_ID,     aMessageId));   // id du message à supprimer
                Log.i(LOG_TAG, "Suppression d'un Msg " + PARAM_MSG_DEL_ID + "=" + aMessageId);
            }
            else if (   (null != aDestId)
                     && (null != aText) ) {
                // Post d'un nouveau PM :
                nameValuePairs.add(new BasicNameValuePair(PARAM_PM_NEW_DEST_ID, aDestId));      // id du destinataire du nouveau pm  
                nameValuePairs.add(new BasicNameValuePair(PARAM_PM_NEW_TEXT,    aText));        // texte du pm à créer
                Log.i(LOG_TAG, "Post d'un nouveau PM " + PARAM_PM_NEW_DEST_ID + "=" + aDestId + ", " + PARAM_PM_NEW_TEXT + "=" + aText);
            }
            else if (null != aPmId) {
                // Suppression d'un PM :
                nameValuePairs.add(new BasicNameValuePair(PARAM_PM_DEL_ID,      aPmId));        // id du pm à supprimer  
                Log.i(LOG_TAG, "Suppression d'un PM " + PARAM_PM_DEL_ID + "=" + aPmId);
            }
            
            // puis place tous ces paramètres dans la requête HTTP POST
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
            
            // Execute HTTP Post Request  
            HttpResponse response = httpClient.execute(httpPost);
            if (HttpStatus.SC_OK == response.getStatusLine ().getStatusCode()) {
                Log.d(LOG_TAG, "HttpStatus OK");

                // Récupère le contenu de la réponse
                InputStream             in          = response.getEntity().getContent();
            
                // Parse de la page XML
                DocumentBuilderFactory  dbf         = DocumentBuilderFactory.newInstance();
                dbf.setCoalescing(true);
                DocumentBuilder         db          = dbf.newDocumentBuilder();
                Document                dom         = db.parse(in);
                Element                 eltDocument  = dom.getDocumentElement();
                
                if (null != eltDocument) {
                    // Commence par vérifier une éventuelle erreur explicite de login/mot de passe
                    Element eltLoginError = (Element)eltDocument.getElementsByTagName(NODE_NAME_BAD_LOGIN).item(0);
                    if (null == eltLoginError) {
    
                        /////////////////////////////////////////////////////////////////////////////
                        // Récupère la liste des utilisateurs
                        // (en premier car la liste des utilisateurs est nécessaire pour la suite !)
                        {
                            NodeList    listUser = eltDocument.getElementsByTagName(NODE_NAME_USER);
                            int nbUsers = listUser.getLength();
                            if (0 < nbUsers) {
                                for (int i = 0; i < nbUsers; i++) {
                                    Element eltUser        = (Element)listUser.item(i);
            
                                    String  strIdUser   = eltUser.getAttribute(ATTR_NAME_USER_ID);
                                    int     idUser      = Integer.parseInt(strIdUser);
                                    String  strPseudo   = eltUser.getAttribute(ATTR_NAME_USER_PSEUDO);
                                    String  strName     = eltUser.getAttribute(ATTR_NAME_USER_NAME);
                                    String  strDateMaj  = eltUser.getAttribute(ATTR_NAME_USER_DATE_MAJ);
                                    long    longDateMaj = (long)Integer.parseInt(strDateMaj);
                                    Date    dateMaj     = new Date(longDateMaj*1000);
                                    String  strIsActive = eltUser.getAttribute(ATTR_NAME_USER_IS_ACTIVE);
                                    boolean isActive    = (0 != Integer.parseInt(strIsActive));
    
                                    String  strAddress  = null;
                                    String  strNotes    = null;
    
                                    // Récupère l'adresse de l'utilisateur
                                    NodeList    listAddr = eltUser.getElementsByTagName(NODE_NAME_FORUM_ADDRESS);
                                    Element eltAddr = (Element)listAddr.item(0);
                                    Node    txtAddr = eltAddr.getFirstChild();
                                    if (null != txtAddr) {
                                        strAddress  = eltAddr.getFirstChild().getNodeValue();
                                    }
                                    
                                    // Récupère les notes complémentaires à l'adresse de l'utilisateur
                                    NodeList    listNotes = eltUser.getElementsByTagName(NODE_NAME_FORUM_NOTES);
                                    Element eltNotes = (Element)listNotes.item(0);
                                    Node    txtNotes = eltNotes.getFirstChild();
                                    if (null != txtNotes) {
                                        strNotes  = txtNotes.getNodeValue();
                                    }
                                    
                                    Log.d(LOG_TAG, "User " + idUser + " " + strPseudo + " " + strName);
                                    
                                    User newUser = new User(idUser, strPseudo, strName, strAddress, strNotes, dateMaj, isActive);
                                    
                                    // Update l'utilisateur s'il existe déjà, sinon l'insert
                                    if (mUserDBAdapter.isExist(idUser)) {
                                        mUserDBAdapter.updateUser(newUser);
                                    } else {
                                        mUserDBAdapter.insertUser(newUser);
                                    }
                                }
    
                                Log.i(LOG_TAG, "nbUsers=" + nbUsers);
                                
                                // Ré-initialise la liste des utilisateurs
                                ApplicationSJLB appSJLB = (ApplicationSJLB)mContext.getApplication();
                                appSJLB.initUserContactList();
                                
                            } else {
                                Log.d(LOG_TAG, "no <user> XML content");
                            }
                        }
                        
                        ///////////////////////////////////////////////////////////////////////////
                        // Récupère la liste des Messages "non lus" sur le site SJLB, pour gérer les notifications
                        // (Note : on vient de transmettre au site l'éventuelle liste des messages lus localement sur l'appli, donc il est au courant)
                        {
                            NodeList    listUnreadMsg = eltDocument.getElementsByTagName(NODE_NAME_FORUM_UNREAD);
                            nbUnreadMsg = listUnreadMsg.getLength();
                            if (0 < nbUnreadMsg) {
                                for (int i = 0; i < nbUnreadMsg; i++) {
                                    Element eltMsg      = (Element)listUnreadMsg.item(i);
                                    String  strIdMsg    = eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_ID);
                                    int     idMsg       = Integer.parseInt(strIdMsg);
                                    
                                    if (0 < idMsg) {
                                        // Teste si le message est nouveau 
                                        // (sinon c'est qu'il s'agit d'un message déjà récupéré, tel quel ou entre temps modifié)
                                        if (false == mMsgDBAdapter.isExist(idMsg)) {
                                            Log.i(LOG_TAG, "Msg " + idMsg + " nouveau");
                                            nbNewMsg++;
                                        }
                                        else {
                                            Log.d(LOG_TAG, "Msg " + idMsg + " recuperes precedemment");
                                        }
                                    } else {
                                        if (-1 == idMsg) {
                                            Log.i(LOG_TAG, "Signal de l'agenda (ID -1)");
                                        } else {
                                            Log.w(LOG_TAG, "Msg d'ID " + idMsg + " < 0");
                                        }
                                    }
                                }
                                Log.i(LOG_TAG, "nbUnreadMsg=" + nbUnreadMsg);
                            } else {
                                Log.d(LOG_TAG, "no <unread> XML content");
                            }
                        }

                        // Efface les flags UNREAD_LOCALY des messages lus localement puisqu'on a transmis l'info au serveur
                        // (Note SRO : fait ici avant de récupérer les nouveaux messages, ainsi un message modifié peut être re-flagué non lus)
                        if (0 < strMsgLus.length()) {
                            int nbCleared = mMsgDBAdapter.clearMsgUnread ();
                            Log.i(LOG_TAG, "clearMsgUnread = " + nbCleared);
                        }
                        
                        ///////////////////////////////////////////////////////////////////////////
                        // Récupère la liste des Messages "supprimés" sur le site SJLB, pour gérer les effacements en bdd
                        {
                            NodeList    listSupprMsg = eltDocument.getElementsByTagName(NODE_NAME_FORUM_SUPPR);
                            int  nbSupprMsg = listSupprMsg.getLength();
                            if (0 < nbSupprMsg) {
                                long longDateSuppr = 0;
                                for (int i = 0; i < nbSupprMsg; i++) {
                                    Element eltMsg          = (Element)listSupprMsg.item(i);
                                    String  strIdMsg        = eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_ID);
                                    int     idMsg           = Integer.parseInt(strIdMsg);
                                    String  strDateSuppr    = eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_DATE_SUPPR);
                                            longDateSuppr   = (long)Integer.parseInt(strDateSuppr);
                                    
                                    // Supprime le message la la base
                                    Log.d(LOG_TAG, "Msg.delete(" + idMsg + ") date_suppr=" + strDateSuppr);
                                    mMsgDBAdapter.delete(idMsg);
                                }
                                Log.i(LOG_TAG, "nbSupprMsg=" + nbSupprMsg + ", last_date_suppr=" + longDateSuppr);
                                // Enregistre dans les préférences la date de suppression du dernier message supprimé
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putLong(PARAM_DATE_LAST_MSG_SUPPR, longDateSuppr);
                                editor.commit();
                            } else {
                                Log.d(LOG_TAG, "no <msg_suppr> XML content");
                            }
                        }
                                                
                        ///////////////////////////////////////////////////////////////////////////
                        // Récupère la liste des PM "supprimés" sur le site SJLB, pour gérer les effacements en bdd
                        {
                            NodeList    listSupprPM = eltDocument.getElementsByTagName(NODE_NAME_PRIVATE_SUPPR);
                            int nbSupprPM = listSupprPM.getLength();
                            if (0 < nbSupprPM) {
                                long longDateSuppr = 0;
                                for (int i = 0; i < nbSupprPM; i++) {
                                    Element eltPM           = (Element)listSupprPM.item(i);
                                    String  strIdPM         = eltPM.getAttribute(ATTR_NAME_PRIVATE_MSG_ID);
                                    int     idPM            = Integer.parseInt(strIdPM);
                                    String  strDateSuppr    = eltPM.getAttribute(ATTR_NAME_PRIVATE_MSG_DATE_SUPPR);
                                            longDateSuppr   = (long)Integer.parseInt(strDateSuppr);
       
                                    // Supprime le message la la base
                                    Log.d(LOG_TAG, "PM.delete(" + idPM + ") date_suppr=" + strDateSuppr);
                                    mPMDBAdapter.delete(idPM);
                                }
                                Log.i(LOG_TAG, "nbSupprPM=" + nbSupprPM + ", last_date_suppr=" + longDateSuppr);
                                // Enregistre dans les préférences la date de suppression du dernier message supprimé
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putLong(PARAM_DATE_LAST_PM_SUPPR, longDateSuppr);
                                editor.commit();
                            } else {
                                Log.d(LOG_TAG, "no <pm_suppr> XML content");
                            }
                        }
                                                
                        ///////////////////////////////////////////////////////////////////////////
                        // Récupère la liste des Sujets
                        {
                            NodeList    listSubj = eltDocument.getElementsByTagName(NODE_NAME_FORUM_SUBJ);
                            int nbSubj = listSubj.getLength();
                            if (0 < nbSubj) {
                                for (int i = 0; i < nbSubj; i++) {
                                    Element eltSubj     = (Element)listSubj.item(i);
                                    
                                    String  strText     = eltSubj.getFirstChild().getNodeValue();
        
                                    String  strIdSubj   = eltSubj.getAttribute(ATTR_NAME_FORUM_SUBJ_ID);
                                    int     idSubj      = Integer.parseInt(strIdSubj);
                                    String  strIdCat    = eltSubj.getAttribute(ATTR_NAME_FORUM_SUBJ_CAT_ID);
                                    int     idCat       = Integer.parseInt(strIdCat);
                                    String  strIdGroup  = eltSubj.getAttribute(ATTR_NAME_FORUM_SUBJ_GROUP_ID);
                                    int     idGroup     = Integer.parseInt(strIdGroup);
                                    String  strLastDate = eltSubj.getAttribute(ATTR_NAME_FORUM_SUBJ_DERNIERE_DATE);
                                    long    longLastDate= (long)Integer.parseInt(strLastDate);
                                    Date    lastDate    = new Date(longLastDate*1000);
                                    
                                    ForumSubject newSubj = new ForumSubject(idSubj, idCat, idGroup, lastDate, strText);
                                    Log.d(LOG_TAG, "Subj " + newSubj);
                                    
                                    // Update le sujet s'il existe déjà, sinon l'insert
                                    if (mSubjDBAdapter.isExist(idSubj)) {
                                        if (mSubjDBAdapter.updateSubj(newSubj)) {
                                            Log.d(LOG_TAG, "Subj " + idSubj + " updated");                                
                                        } else {
                                            Log.e(LOG_TAG, "Subj " + idSubj + " NOT updated !");
                                        }
                                    } else {
                                        if (mSubjDBAdapter.insertSubj(newSubj)) {
                                            Log.d(LOG_TAG, "Subj " + idSubj + " inserted");                                
                                        } else {
                                            Log.e(LOG_TAG, "Subj " + idSubj + " NOT inserted !");
                                        }
                                    }
                                    
                                }
                                Log.i(LOG_TAG, "nbSubj=" + nbSubj);
                            } else {
                                Log.d(LOG_TAG, "no <sujet> XML content");
                            }
                        }
                        
                        ///////////////////////////////////////////////////////////////////////////
                        // Récupère la liste des Msg
                        {
                            NodeList    listMsg = eltDocument.getElementsByTagName(NODE_NAME_FORUM_MSG);
                            int nbMsg = listMsg.getLength();
                            if (0 < nbMsg) {
                                Log.d(LOG_TAG, "listMsg.getLength() = " + nbMsg);
                                for (int i = 0; i < nbMsg; i++) {
                                    Element eltMsg      = (Element)listMsg.item(i);
                                    
                                    String  strText     = eltMsg.getFirstChild().getNodeValue();
        
                                    String  strIdMsg    = eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_ID);
                                    int     idMsg       = Integer.parseInt(strIdMsg);
                                    String  strDate     = eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_DATE);
                                    long    longDate    = (long)Integer.parseInt(strDate);
                                    Date    date        = new Date(longDate*1000);
                                    String  strDateEdit = eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_DATE_EDIT);
                                    long    longDateEdit= (long)Integer.parseInt(strDateEdit);
                                    Date    dateEdit    = new Date(longDateEdit*1000);
                                    String  strIdAuthor = eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_AUTHOR_ID);
                                    int     idAuthor    = Integer.parseInt(strIdAuthor);
                                    String  strIdSubject= eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_SUBJECT_ID);
                                    int     idSubject   = Integer.parseInt(strIdSubject);
                                    String  strUnread   = eltMsg.getAttribute(ATTR_NAME_FORUM_MSG_UNREAD);
                                    boolean bUnread     = (0 != Integer.parseInt(strUnread));
                                    
                                    ForumMessage newMsg = new ForumMessage(idMsg, date, dateEdit, idAuthor, idSubject, bUnread, strText);
                                    Log.v(LOG_TAG, "Msg " + newMsg);
                                    
                                    // Update le message s'il existe déjà, sinon l'insert
                                    if (mMsgDBAdapter.isExist(idMsg)) {
                                        if (mMsgDBAdapter.updateMsg(newMsg)) {
                                            Log.v(LOG_TAG, "Msg " + idMsg + " updated");
                                        } else {
                                            Log.e(LOG_TAG, "Msg " + idMsg + " NOT updated !");
                                        }
                                    } else {
                                        if (mMsgDBAdapter.insertMsg(newMsg)) {
                                            Log.v(LOG_TAG, "Msg " + idMsg + " inserted");                                
                                        } else {
                                            Log.e(LOG_TAG, "Msg " + idMsg + " NOT inserted !");
                                        }
                                    }
                                    
                                    // Dans le cas d'un message non lu, met aussi à jour (recalcule) le compteur de messages non lus du sujet en question
                                    if (bUnread) {
                                        final int NbUnread = mMsgDBAdapter.getNbUnread(idSubject);
                                        if (mSubjDBAdapter.updateNbUnread(idSubject, NbUnread))
                                        {
                                            Log.d(LOG_TAG, "NbUnread(" + idSubject + ")=" + NbUnread);                                
                                        } else {
                                            Log.e(LOG_TAG, "NbUnread(" + idSubject + ")=" + NbUnread);
                                        }
                                    }
                                    
        
                                    // Récupère la liste des fichiers attachés au Msg
                                    NodeList    listFile = eltMsg.getElementsByTagName(NODE_NAME_FORUM_FILE);
                                    int nbFile = listFile.getLength();
                                    for (int j = 0; j < nbFile; j++) {
                                        Element eltFile     = (Element)listFile.item(j);
                                        String  fileName    = eltFile.getFirstChild().getNodeValue();
    
                                        AttachedFile newAttachedFile = new AttachedFile(idMsg, fileName);
                                        Log.d(LOG_TAG, "Fichier " + newAttachedFile);
                                        
                                        // TODO SRombauts : BUG ! supprimer d'abord tout fichier attachés pour cet ID de message !
                                        if (mFileDBAdapter.insertFile(newAttachedFile)) {
                                            Log.d(LOG_TAG, "AttachedFile " + fileName + " inserted");                                
                                        } else {
                                            Log.e(LOG_TAG, "AttachedFile " + fileName + " NOT inserted !");
                                        }
                                    }
                                }
                                Log.i(LOG_TAG, "nbMsg=" + nbMsg);
                            } else {
                                Log.d(LOG_TAG, "no <msg> XML content");
                            }
                        }

                        ///////////////////////////////////////////////////////////////////////////
                        // Récupère la liste des PM
                        {
                            NodeList    listPM = eltDocument.getElementsByTagName(NODE_NAME_PRIVATE_MSG);
                            int nbPM = listPM.getLength();
                            if (0 < nbPM) {
                                for (int i = 0; i < nbPM; i++) {
                                    Element eltPm       = (Element)listPM.item(i);
                                    
                                    String  strText     = eltPm.getFirstChild().getNodeValue();
        
                                    String  strIdPM     = eltPm.getAttribute(ATTR_NAME_PRIVATE_MSG_ID);
                                    int     idPM        = Integer.parseInt(strIdPM);
                                    String  strDate     = eltPm.getAttribute(ATTR_NAME_PRIVATE_MSG_DATE);
                                    long    longDate    = (long)Integer.parseInt(strDate);
                                    Date    date        = new Date(longDate*1000);
                                    String  strIdAuthor = eltPm.getAttribute(ATTR_NAME_PRIVATE_MSG_AUTHOR_ID);
                                    int     idAuthor    = Integer.parseInt(strIdAuthor);
                                    String  strIdDest   = eltPm.getAttribute(ATTR_NAME_PRIVATE_MSG_DEST_ID);
                                    int     idDest      = Integer.parseInt(strIdDest);
                                    
                                    Log.d(LOG_TAG, "PM " + idPM + " ("+ idAuthor +") " + strDate + " : '"  + strText + "' (" + strText.length()+ ")");
                                    
                                    // Compte les nouveaux pm envoyés à l'utilisateur par les autres (ie, pas les pm dont l'utilisateur est l'auteur)
                                    if (((ApplicationSJLB)mContext.getApplication ()).getUserId() != idAuthor) {
                                        nbNewPM++;
                                    }
                                    
                                    PrivateMessage newPM = new PrivateMessage(idPM, date, idAuthor, idDest, strText);
                                    
                                    // Renseigne la bdd
                                    Boolean bInserted = mPMDBAdapter.insertPM(newPM);
                                    if (bInserted) {
                                        Log.d(LOG_TAG, "PM " + idPM + " inserted");                                
                                    }
                                }
                                Log.i(LOG_TAG, "nbPM=" + nbPM);
                            } else {
                                Log.d(LOG_TAG, "no <pm> XML content");
                            }
                        }
                        
                        //////////////////////////////////////////////////////////////
                        // Arrivé ici, c'est qu'il n'y a manifestement pas eu d'erreur (pas d'exception)
                        bSuccess = true;

                        // Mémorise les informations de versions qui ont été transmis au site SJLB
                        // ... seulement s'il y a eu du nouveau !
                        if (   (false == phoneModel.equals(Build.MODEL))
                            || (false == buildBrand.equals(Build.BRAND))
                            || (false == versionAndroid.equals(Build.VERSION.RELEASE))
                            || (apiLevel != Build.VERSION.SDK_INT)
                            || (versionAppli != versionCode)
                           )
                        {
                            Log.i(LOG_TAG, "SAVE device: " + Build.MODEL + " (" + Build.MANUFACTURER + "/" + Build.BRAND + ") " + Build.VERSION.RELEASE + " (api_level=" + Build.VERSION.SDK_INT + ") versionCode=" + versionCode);                            
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(PARAM_PHONE_MODEL, Build.MODEL);
                            editor.putString(PARAM_BUILD_BRAND, Build.BRAND);
                            editor.putString(PARAM_VERSION_ANDROID, Build.VERSION.RELEASE);
                            editor.putInt(PARAM_API_LEVEL, Build.VERSION.SDK_INT);
                            editor.putInt(PARAM_VERSION_APPLI, versionCode);
                            editor.commit();
                        } else {
                            Log.d(LOG_TAG, "ALREADY SAVED device: " + Build.MODEL + " (" + Build.MANUFACTURER + "/" + Build.BRAND + ") " + Build.VERSION.RELEASE + " (api_level=" + Build.VERSION.SDK_INT + ") versionCode=" + versionCode);
                        }
                        
                        //////////////////////////////////////////////////////////////
                        // En fin de refresh : affiche les éventuelles notifications !
                        // Notification dans la barre de status s'il y a de nouveaux PM
                        // pas de suppression de notification des PM tant que pas lus localement
                        Log.i(LOG_TAG, "nbNewPM = " + nbNewPM + ", nbNewMsg = " + nbNewMsg + ", nbUnreadMsg = " + nbUnreadMsg);

                        if (0 < nbNewPM) {
                            notifyUserPM (nbNewPM);
                        }
                        // s'il y a de nouveaux messages non lus :
                        if (   (0 < nbNewMsg)
                            && (0 < nbUnreadMsg) ) {
                            // Notification dans la barre de status
                            notifyUserMsg (nbUnreadMsg);
                        } else if (0 == nbUnreadMsg) {
                            // Suppression de la notification de la barre de status
                            revertNotifyUserMsg ();
                        }

                        Log.d(LOG_TAG, "fetchNewContent... ok");                        
                    } else {
                        //e.printStackTrace();
                        Log.w(LOG_TAG, "doInBackground: Bad Login/Password set in preferences");
                        
                        // Efface alors le mot de passe des préférences !
                        PrefsLoginPassword.InvalidatePassword (mContext);
                    }
                } else {
                    Log.e(LOG_TAG, "no XML document: server error");
                }
            } else {
                Log.e(LOG_TAG, "http error");
            }
            
        } catch (LoginPasswordEmptyException e) {
            // e.printStackTrace();
            Log.w(LOG_TAG, "No Login/Password set in Preferences");                                        
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            Log.e(LOG_TAG, "SAXException");                                        
            e.printStackTrace();
        } catch (ClassCastException e) {        
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        
        // Fermeture de tous les adapters ayant pu être ouverts (nécessaire, sinon stack d'erreur)
        mMsgDBAdapter.close();
        mPMDBAdapter.close();
        mUserDBAdapter.close();
        mSubjDBAdapter.close();
        mFileDBAdapter.close();
        
        return bSuccess;
    }
    

    /**
     * Notifie à l'utilisateur le nombre de messages privés lorsqu'un ou plusieurs PM a été reçu
     */
    private void notifyUserPM (int aNbNewPM) {
        Log.d(LOG_TAG, "notifyUserPM(" + aNbNewPM + ")");

        // Récupère les préférences de notification :
        PrefsNotification    notificationPrefs        = new PrefsNotification(mContext);

        // Récupère une reference sur le NotificationManager :
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) mContext.getSystemService(ns);

        // Définition du message détaillé à afficher dans le volet de notification
        Context         context             = mContext.getApplicationContext();
        CharSequence    contentTitle        = mContext.getString(R.string.notification_title_pm);
        CharSequence    contentText         = aNbNewPM + " " + mContext.getString(R.string.notification_text_pm);
        
        // Intent à envoyer lorsque l'utilisateur sélectionne la  notification
        Intent          notificationIntent  = new Intent(mContext, ActivityPrivateMessages.class);
        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        // Préparation du résumé de notification
        int             icon            = R.drawable.status_icon;
        CharSequence    tickerText      = mContext.getString(R.string.app_name) + ": " + aNbNewPM + " " + mContext.getString(R.string.notification_text_pm);
        long            when            = System.currentTimeMillis();
        // ... et instanciation de la Notification :
        Notification    notification    = new Notification(icon, tickerText, when);

        notification.number     = aNbNewPM;
        
        notification.defaults   = 0;
        if (notificationPrefs.mbSound) {
            notification.defaults   |= Notification.DEFAULT_SOUND;
        }
        if (notificationPrefs.mbLight) {
            notification.defaults   |= Notification.DEFAULT_LIGHTS;
        }
        if (notificationPrefs.mbVibrate) {
            notification.vibrate    = new long[]{0, 200, 300, 200, 300, 200};
        }

        // Assemblage final de la notification
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        // Passage de la Notification au NotificationManager:
        notificationManager.notify(NOTIFICATION_NEW_PM_ID, notification);
    }    

    /**
     *  Notifie à l'utilisateur les évolutions du nombre de messages non lus
     *  
     *  Génère une notification indiquant le nombre de messages non lus, dont le nombre 
     */
    private void notifyUserMsg (int aNbUnreadMsg) {
        Log.d(LOG_TAG, "notifyUserMsg(" + aNbUnreadMsg + ")");
        
        // Récupère une reference sur le NotificationManager :
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) mContext.getSystemService(ns);
    
        // Récupère les préférences de notification :
        PrefsNotification    notificationPrefs        = new PrefsNotification(mContext);
    
        // Définition du message détaillé à afficher dans le volet de notification
        Context         context             = mContext.getApplicationContext();
        CharSequence    contentTitle        = mContext.getString(R.string.notification_title_msg);
        CharSequence    contentText         = aNbUnreadMsg + " " + mContext.getString(R.string.notification_text_msg) + ")";
        
        // Intent à envoyer lorsque l'utilisateur sélectionne la  notification
        // => lien activité principale :
        Intent          notificationIntent  = new Intent(mContext, ActivityMain.class);
        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
    
        // Préparation du résumé de notification
        int          icon        = R.drawable.status_icon;
        CharSequence tickerText  = mContext.getString(R.string.app_name) + ": " + aNbUnreadMsg + " " + mContext.getString(R.string.notification_text_msg) + ")";
        long         when        = System.currentTimeMillis();
    
        // et instanciation de la Notification :
        Notification notification = new Notification(icon, tickerText, when);
    
        notification.number     = aNbUnreadMsg;
    
        notification.defaults   = 0;
        if (notificationPrefs.mbSound) {
            notification.defaults   |= Notification.DEFAULT_SOUND;
        }
        if (notificationPrefs.mbLight) {
            notification.defaults   |= Notification.DEFAULT_LIGHTS;
        }
        if (notificationPrefs.mbVibrate) {
            notification.vibrate    = new long[]{0, 200, 300, 200, 300, 200};
        }
        
        // Assemblage final de la notification
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
    
        // Passage de la Notification au NotificationManager:
        notificationManager.notify(NOTIFICATION_NEW_MSG_ID, notification);
    }
    
    /**
     *  Notifie à l'utilisateur les évolutions du nombre de messages non lus
     *  
     *  Génère une notification indiquant le nombre de messages non lus, dont le nombre 
     */
    private void revertNotifyUserMsg () {
        Log.d(LOG_TAG, "revertNotifyUserMsg");
        
        // Récupère une reference sur le NotificationManager :
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) mContext.getSystemService(ns);

        // Suppression de la Notification
        notificationManager.cancel(NOTIFICATION_NEW_MSG_ID);
    }
}

