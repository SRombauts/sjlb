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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ParseException;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import fr.srombauts.sjlb.ApplicationSJLB;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.ContentProviderFile;
import fr.srombauts.sjlb.db.ContentProviderMsg;
import fr.srombauts.sjlb.db.ContentProviderPM;
import fr.srombauts.sjlb.db.ContentProviderSubj;
import fr.srombauts.sjlb.db.ContentProviderUser;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.gui.ActivityMain;
import fr.srombauts.sjlb.gui.ActivityPrivateMessages;
import fr.srombauts.sjlb.model.AttachedFile;
import fr.srombauts.sjlb.model.ForumMessage;
import fr.srombauts.sjlb.model.ForumSubject;
import fr.srombauts.sjlb.model.LoginPasswordBadException;
import fr.srombauts.sjlb.model.LoginPasswordEmptyException;
import fr.srombauts.sjlb.model.PrefsLoginPassword;
import fr.srombauts.sjlb.model.PrefsNotification;
import fr.srombauts.sjlb.model.PrivateMessage;
import fr.srombauts.sjlb.model.User;


/**
 * Tâche de travail en tâche de fond, chargée de récupérer les fichier XML listant les messages non lus
 *
 * Charge un fichier XML light (Polling.php) ne listant que les ID des messages privés/des messages du forum non lus,
 * de manière à ne pas charger le réseau inutilement.
 *
 * SSI de nouveaux ID y sont mentionnés, charge les fichiers XML complémentaires (PrivateMessages.php ou ForumMessages.php).
 *
 * @author 14/06/2010 srombauts
 */
public class AsynchTaskRefresh extends AsyncTask<Void, Void, Void> {
    private static final String  LOG_TAG                    = "RefreshTask";

    public static final int     NOTIFICATION_NEW_PM_ID      = 1;
    public static final int     NOTIFICATION_NEW_MSG_ID     = 2;

    static final private String NODE_NAME_BAD_LOGIN         = "login_error";
    static final private String NODE_NAME_PRIVATE_MSG       = "pm";
    static final private String NODE_NAME_PRIVATE_MSG_ID    = "id";
    static final private String NODE_NAME_FORUM_MSG         = "msg";
    static final private String NODE_NAME_FORUM_FILE        = "fichier";
    static final private String NODE_NAME_FORUM_MSG_ID      = "id";
    static final private String NODE_NAME_FORUM_SUBJ        = "sujet";
    static final private String NODE_NAME_USER              = "user";

    static final private String ATTR_NAME_PRIVATE_MSG_ID        = "id";
    static final private String ATTR_NAME_PRIVATE_MSG_DATE      = "date";
    static final private String ATTR_NAME_PRIVATE_MSG_AUTHOR_ID = "id_auteur";
    static final private String ATTR_NAME_PRIVATE_MSG_PSEUDO    = "pseudo";
    
    static final private String ATTR_NAME_FORUM_SUBJ_ID             = "id";
    static final private String ATTR_NAME_FORUM_SUBJ_CAT_ID         = "id_categorie";
    static final private String ATTR_NAME_FORUM_SUBJ_GROUP_ID       = "id_groupe";
    static final private String ATTR_NAME_FORUM_SUBJ_DERNIERE_DATE  = "derniere_date";

    static final private String ATTR_NAME_FORUM_MSG_ID          = "id";
    static final private String ATTR_NAME_FORUM_MSG_AUTHOR_ID   = "id_auteur";
    static final private String ATTR_NAME_FORUM_MSG_AUTHOR      = "auteur";
    static final private String ATTR_NAME_FORUM_MSG_DATE        = "date";
    static final private String ATTR_NAME_FORUM_MSG_SUBJECT_ID  = "id_sujet";
    static final private String ATTR_NAME_FORUM_MSG_UNREAD      = "unread";
    
    static final private String ATTR_NAME_USER_ID               = "id";
    static final private String ATTR_NAME_USER_PSEUDO           = "pseudo";
    static final private String ATTR_NAME_USER_NAME             = "nom";

    private ServiceRefresh  mContext        = null;
    private int             mNbPM           = 0;    // Nombre de PM de l'utilisateur (issu directement dans la liste XML)
    private int             mNbNewPM        = 0;    // Nombre d'ID de PM inconnus (issu de la comparaison de la liste XML avec la BDD)
    private int             mNbUnreadMsg    = 0;    // Nombre de messages non lus par l'utilisateur (issu directement de la liste XML)
    private int             mNbNewMsg       = 0;    // Nombre de messages non lus pour l'utilisateur
    
    private ContentProviderPM       mPMDBAdapter    = null;
    private ContentProviderSubj     mSubjDBAdapter  = null;
    private ContentProviderMsg      mMsgDBAdapter   = null;
    private ContentProviderFile     mFileDBAdapter  = null;
    private ContentProviderUser     mUserDBAdapter  = null;
      
    /**
     * Constructeur utilisé pour mémorisée la référence sur le service appelant
     * @param context
     */
    public AsynchTaskRefresh(ServiceRefresh context) {
        mContext      = context;
                                
        mPMDBAdapter    = new ContentProviderPM(context);
        mSubjDBAdapter  = new ContentProviderSubj(context);
        mMsgDBAdapter   = new ContentProviderMsg(context);
        mFileDBAdapter  = new ContentProviderFile(context);
        mUserDBAdapter  = new ContentProviderUser(context);
    }


    /**
     * Avant début de refresh
     *
     * Cette méthode est synchronisée avec le thread main, donc on peut y demander des modifs de GUI (notifications...)
     */
    protected void onPreExecute() {
        // Toast notification de début de rafraîchissement
        // SRO COMMENTE : comme on tourne dans un service en tache de fond, on ne veut pas gêner l'utilisateur avec une notification
        // Toast.makeText(mContext, mContext.getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
    }
    
    
    /**
     * Lance la récupération et le parse de la liste XML des messages non lus
     * 
     * Ce travail s'exécute en tâche de fond, et n'a donc pas le droit d'effectuer d'actions sur la GUI
     */
    protected Void doInBackground(Void... args) {
        
        try {
            // Récupération de la liste des utilisateurs (seulement si nécessaire, c'est à dire si la BDD est vide)
            refreshUsers ();
            
            // Recherche d'éventuelles nouveautés ()
            refreshInfos ();
            
            // et récupération de ces éventuels nouveaux contenus
            fetchPM ();  // récupère la liste des PM seulement si nécessaire
            fetchMsg (); // récupère systématiquement la liste des nouveaux messages, éventuellement vide
            
        } catch (LoginPasswordBadException e) {
            //e.printStackTrace();
            Log.w(LOG_TAG, "doInBackground: Bad Login/Password set in preferences");
            
            // Efface alors le mot de passe des préférences !
            PrefsLoginPassword.InvalidatePassword (mContext);
            
            mUserDBAdapter.close();
            mMsgDBAdapter.close();
            mPMDBAdapter.close();
        }        
        return null;
    }
    
    
    /**
     * Fin de refresh
     *
     * Cette méthode est synchronisée avec le thread main, donc on peut y demander des modifs de GUI (notifications...)
     */
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);

      // s'il y a de nouveaux PM :
      if (0 < mNbNewPM) {
          // Notification dans la barre de status
          notifyUserPM ();
      }
      // s'il y a de nouveaux messages non lus (ou au contraire, s'il ne reste plus de messages non lus) :
      if ( (0 < mNbNewMsg) || (0 == mNbUnreadMsg) ) {
          // Notification dans la barre de status (ou suppression de la notification)
          notifyUserMsg ();
      }
      
      // Le service peut dès lors être interrompu une fois qu'il a effectué le rafraîchissement
      mContext.stopSelf ();
    }

    
    /**
     * Publication en cours de travail, sur appel à publishProgress ()
     *
     * Cette méthode est synchronisée donc une action sur la GUI est autorisée 
     */
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);      
    }
    

    /**
     * Récupération des listes des utilisateurs du site
     */
    void refreshUsers () throws LoginPasswordBadException
    {
        // TODO SRO : pour l'instant, fait une unique fois dans la vie de la BDD de l'application
        if (0 == mUserDBAdapter.countUsers()) {
            try {
                Log.d(LOG_TAG, "refreshUsers...");
                
                // Utilise les préférences pour récupérer le login/mot de passe :
                PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);
                
                // Instancie un client http et un header de requète "POST"
                HttpClient  httpClient  = new DefaultHttpClient();
                HttpPost    httpPost    = new HttpPost(mContext.getString(R.string.sjlb_users_uri));
                   
                // Ajout des paramètres
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("login",    loginPassword.mLogin));
                nameValuePairs.add(new BasicNameValuePair("password", loginPassword.mPasswordMD5));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                
                // Execute HTTP Post Request  
                HttpResponse response = httpClient.execute(httpPost);
                if (HttpStatus.SC_OK == response.getStatusLine ().getStatusCode()) {
                    // Récupère le contenu de la réponse
                    InputStream             in          = response.getEntity().getContent();
                    
                    // Parse de la page XML
                    DocumentBuilderFactory  dbf         = DocumentBuilderFactory.newInstance();
                    dbf.setCoalescing(true);
                    DocumentBuilder         db          = dbf.newDocumentBuilder();
                    Document                dom         = db.parse(in);
                    Element                 docElement  = dom.getDocumentElement();
                    
                    if (null != docElement) {
                        // Commence par vérifier une éventuelle erreur explicite de login/mot de passe
                        Element eltLoginError = (Element)docElement.getElementsByTagName(NODE_NAME_BAD_LOGIN).item(0);
                        if (null != eltLoginError) {
                            throw new LoginPasswordBadException ();
                        } else {
                            // Récupère la liste des utilisateurs
                            NodeList    listUser = docElement.getElementsByTagName(NODE_NAME_USER);
                            if (null != listUser) {
                                int nbUsers = listUser.getLength();
                                for (int i = 0; i < nbUsers; i++) {
                                    Element user        = (Element)listUser.item(i);
            
                                    String  strIdUser   = user.getAttribute(ATTR_NAME_USER_ID);
                                    int     idUser      = Integer.parseInt(strIdUser);
                                    String  strPseudo   = user.getAttribute(ATTR_NAME_USER_PSEUDO);
                                    String  strName     = user.getAttribute(ATTR_NAME_USER_NAME);
                                    
                                    Log.d(LOG_TAG, "User " + idUser + " " + strPseudo + " " + strName);
                                    
                                    User newUser = new User(idUser, strPseudo, strName);
                                    
                                    // Renseigne la bdd si User inconnu
                                    // TODO SRO : un peu moche, provoque une exception SQL si l'utilisateur est déjà en base (mais ne survient pas, cf TODO plus haut)
                                    mUserDBAdapter.insertUser(newUser);
                                }
                                
                                // Initialise la liste des utilisateurs
                                ApplicationSJLB appSJLB = (ApplicationSJLB)mContext.getApplication();
                                appSJLB.initUserContactList();
                            } else {
                                Log.e(LOG_TAG, "refreshUsers: bad XML content");
                            }
                        }
                    } else {
                        Log.e(LOG_TAG, "refreshUsers: no XML document: server error");
                    }
                    
                    Log.d(LOG_TAG, "refreshUsers... ok");
                    
                } else {
                    Log.e(LOG_TAG, "refreshUsers: http error");
                }
                   
            } catch (LoginPasswordEmptyException e) {
                //e.printStackTrace();
                Log.w(LOG_TAG, "refreshUsers: No Login/Password set in preferences");                
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {        
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            mUserDBAdapter.close();
        }
    }
        
        
    /**
     * Récupération des listes d'identifiants de messages non lus
     */
    void refreshInfos () throws LoginPasswordBadException {

        mNbNewPM    = 0;
        mNbNewMsg   = 0;
        
        try {
            Log.d(LOG_TAG, "refreshInfos...");
            
            // Utilise les préférences pour récupérer le login/mot de passe :
            PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);

            // Établi la liste des messages lus localement, à transmettre au site SJLB pour qu'il se mette à jour
            // TODO SRO : déplacer ce qui suit dans une méthode dédiée
            Cursor cursor = mMsgDBAdapter.getMsgUnread ();
            String strMsgLus = "";
            int    nbMsgLus = cursor.getCount ();
            if (0 < nbMsgLus) {
                if (cursor.moveToFirst ()) {
                    do {
                        //Log.d(LOG_TAG, "refreshUsers id_msg_non_lu=" + cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Msg._ID)));
                        if (strMsgLus != "") {
                            strMsgLus += ",";
                        }
                        strMsgLus += cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Msg._ID));
                    } while (cursor.moveToNext ());
                }
            }
            cursor.close ();
            
            // Instancie un client HTTP et un header de requête "POST"
            HttpClient  httpClient  = new DefaultHttpClient();  
            HttpPost    httpPost    = new HttpPost(mContext.getString(R.string.sjlb_polling_uri));  
               
            // Ajout des 8 paramètres
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(8);  
            // commence par les 2 infos de login et le password
            nameValuePairs.add(new BasicNameValuePair("login",    loginPassword.mLogin));  
            nameValuePairs.add(new BasicNameValuePair("password", loginPassword.mPasswordMD5));  
            // puis la liste des messages lus localement
            if (0 < nbMsgLus) {
                Log.d(LOG_TAG, "strMsgLus=" + strMsgLus);
                nameValuePairs.add(new BasicNameValuePair("msg_lus", strMsgLus));
            }
            // y ajoute les 5 informations de version de l'équipement et de l'application
            // TODO SRO : ne transmettre que lorsque différent !
            Log.i(LOG_TAG, "device: " + Build.MODEL + " (" + Build.MANUFACTURER + "/" + Build.BRAND + ") " + Build.VERSION.RELEASE + " (api_level=" + Build.VERSION.SDK_INT + ")");
            nameValuePairs.add(new BasicNameValuePair("model",      Build.MODEL));
            nameValuePairs.add(new BasicNameValuePair("brand",      Build.BRAND));
            nameValuePairs.add(new BasicNameValuePair("android",    Build.VERSION.RELEASE));
            nameValuePairs.add(new BasicNameValuePair("api",        Integer.toString(Build.VERSION.SDK_INT)));
            nameValuePairs.add(new BasicNameValuePair("appli",      Integer.toString(mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES).versionCode)));
            // TODO SRO : ajouter l'état de l'application (ouverte/fermée) + le nombre de messages récupérés localement
            // puis place tous ces paramètres dans la requête HTTP POST
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
            
            // Execute HTTP Post Request  
            HttpResponse response = httpClient.execute(httpPost);
            if (HttpStatus.SC_OK == response.getStatusLine ().getStatusCode()) {
                // Récupère le contenu de la réponse
                InputStream             in          = response.getEntity().getContent();
                
                // Parse de la page XML
                DocumentBuilderFactory  dbf         = DocumentBuilderFactory.newInstance();
                dbf.setCoalescing(true);
                DocumentBuilder         db          = dbf.newDocumentBuilder();
                Document                dom         = db.parse(in);
                Element                 docElement  = dom.getDocumentElement();
                
                if (null != docElement) {
                    // Commence par vérifier une éventuelle erreur explicite de login/mot de passe
                    Element eltLoginError = (Element)docElement.getElementsByTagName(NODE_NAME_BAD_LOGIN).item(0);
                    if (null != eltLoginError) {
                        throw new LoginPasswordBadException ();
                    } else {
                        // Récupère la liste des PM
                        Element     eltPM  = (Element)docElement.getElementsByTagName(NODE_NAME_PRIVATE_MSG).item(0);
                        if (null != eltPM) {
                            NodeList    listPM = eltPM.getElementsByTagName(NODE_NAME_PRIVATE_MSG_ID);
                            if (null != listPM) {
                                mNbPM = listPM.getLength();
                                for (int i = 0; i < mNbPM; i++) {
                                    Element pm      = (Element)listPM.item(i);
                                    String  strIdPM = pm.getFirstChild().getNodeValue();
                                    int     idPM    = Integer.parseInt(strIdPM);
                                    
                                    // Signale la présence d'au moins un nouveau message si PM inconnu
                                    if (false == mPMDBAdapter.isExist(idPM)) {
                                        mNbNewPM++;
                                    }
                                }
                            }
                            // Si aucun nouveau PM n'est détecté,
                            if (0 == mNbNewPM ) {
                                int nbPMInBdd = (int)mPMDBAdapter.countPM();
                                // détection du cas où l'on dispose de PLUS de PM en BDD locale qu'il n'en reste sur le site !
                                if (mNbPM < nbPMInBdd) {
                                    // nombre négatif, un(des) PM a(ont) été supprimé(s)  !
                                    mNbNewPM = mNbPM - nbPMInBdd;
                                }
                            }
                        } else {
                            Log.e(LOG_TAG, "refreshInfos: bad XML content");
                        }
                    }
                    
                    // Récupère la liste des Messages
                    Element     eltMsg  = (Element)docElement.getElementsByTagName(NODE_NAME_FORUM_MSG).item(0);
                    NodeList    listMsg = eltMsg.getElementsByTagName(NODE_NAME_FORUM_MSG_ID);
                    if (null != listMsg) {
                        mNbUnreadMsg = listMsg.getLength();
                        for (int i = 0; i < mNbUnreadMsg; i++) {
                            Element msg      = (Element)listMsg.item(i);
                            String  strIdMsg = msg.getFirstChild().getNodeValue();
                            int     idMsg    = Integer.parseInt(strIdMsg);
                            
                            if (0 < idMsg) {
                                // Teste si le message est nouveau 
                                // (sinon c'est qu'il s'agit d'un message déjà récupéré, tel quel ou entre temps modifié)
                                if (false == mMsgDBAdapter.isExist(idMsg)) {
                                    Log.d(LOG_TAG, "Msg " + idMsg + " nouveau");
                                    mNbNewMsg++;
                                }
                                else {
                                    Log.d(LOG_TAG, "Msg " + idMsg + " recuperes precedemment");
                                }
                            } else {
                                Log.e(LOG_TAG, "Msg d'ID " + idMsg + " interdit");
                            }
                        }
                    }
                    
                    // Efface les flags UNREAD_LOCALY des messages lus localement
                    if (0 < nbMsgLus) {
                        int nbCleared = mMsgDBAdapter.clearMsgUnread ();
                        Log.d(LOG_TAG, "clearMsgUnread = " + nbCleared);
                    }
                } else {
                    Log.e(LOG_TAG, "refreshInfos: no XML document: server error");
                }
                
                Log.d(LOG_TAG, "refreshInfos... ok : mNbNewPM="+mNbNewPM+" ("+mNbPM+"), mNbNewMsg="+mNbNewMsg+" ("+mNbUnreadMsg+")");

            } else {
                Log.e(LOG_TAG, "refreshInfos: http error");
            }
               
        } catch (LoginPasswordEmptyException e) {
            //e.printStackTrace();
            Log.w(LOG_TAG, "refreshInfos: No Login/Password set in preferences");                
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {        
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        mMsgDBAdapter.close();
        mPMDBAdapter.close();
    }
    
        
    /**
     * Récupération du contenu des messages privés
     */
    void fetchPM () {
    
        // s'il y a de nouveaux PM (ou au contraire s'il y en a moins) :
        if (0 != mNbNewPM) {
    
            try {
                Log.d(LOG_TAG, "fetchPM...");
                
                // Utilise les préférences pour récupérer le login/mot de passe :
                PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);
                
                // Instancie un client HTTP et un header de requête "POST"  
                HttpClient  httpClient  = new DefaultHttpClient();  
                HttpPost    httpPost    = new HttpPost(mContext.getString(R.string.sjlb_pm_uri));  
                   
                // Ajout des paramètres
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
                nameValuePairs.add(new BasicNameValuePair("login",    loginPassword.mLogin));  
                nameValuePairs.add(new BasicNameValuePair("password", loginPassword.mPasswordMD5));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
                
                // Execute HTTP Post Request  
                HttpResponse response = httpClient.execute(httpPost);
                if (HttpStatus.SC_OK == response.getStatusLine ().getStatusCode()) {
                    // Récupère le contenu de la réponse
                    InputStream             in          = response.getEntity().getContent();
                    
                    // TODO SRO mutualiser ce qui suit pour l'utiliser aussi lors d'un effacement de pm
                    // Parse de la page XML
                    DocumentBuilderFactory  dbf         = DocumentBuilderFactory.newInstance();
                    dbf.setCoalescing(true);
                    DocumentBuilder         db          = dbf.newDocumentBuilder();
                    Document                dom         = db.parse(in);
                    Element                 docElement  = dom.getDocumentElement();
                    
                    if (null != docElement) {
                        // Récupère la liste des PM
                        NodeList    listPM = docElement.getElementsByTagName(NODE_NAME_PRIVATE_MSG);
                        if (null != listPM) {
                        
                            // Vide la table des messages privés
                            // (plus simple que de supprimer les messages ayant disparus et ne réinsérer que les nouveaux)
                            mPMDBAdapter.clearPM ();
                            
                            mNbPM = listPM.getLength();
                            for (int i = 0; i < mNbPM; i++) {
                                Element pm      = (Element)listPM.item(i);
                                
                                String  strText     = pm.getFirstChild().getNodeValue();
    
                                String  strIdPM     = pm.getAttribute(ATTR_NAME_PRIVATE_MSG_ID);
                                int     idPM        = Integer.parseInt(strIdPM);
                                String  strDate     = pm.getAttribute(ATTR_NAME_PRIVATE_MSG_DATE);
                                long    longDate    = (long)Integer.parseInt(strDate);
                                Date    date        = new Date(longDate*1000);
                                String  strIdAuthor = pm.getAttribute(ATTR_NAME_PRIVATE_MSG_AUTHOR_ID);
                                int     idAuthor    = Integer.parseInt(strIdAuthor);
                                String  strAuthor   = pm.getAttribute(ATTR_NAME_PRIVATE_MSG_PSEUDO);
                                
                                Log.d(LOG_TAG, "PM " + idPM + " " + strAuthor + " ("+ idAuthor +") " + strDate + " : '"  + strText + "' (" + strText.length()+ ")");
                                
                                PrivateMessage newPM = new PrivateMessage(idPM, date, idAuthor, strAuthor, strText);
                                
                                // Renseigne la bdd
                                Boolean bInserted = mPMDBAdapter.insertPM(newPM);
                                if (bInserted) {
                                    Log.d(LOG_TAG, "PM " + idPM + " inserted");                                
                                }
                            }
                        } else {
                            Log.e(LOG_TAG, "fetchPM: no XML document: server error");
                        }
                    }

                    Log.d(LOG_TAG, "fetchPM... ok");                        

                } else {
                    Log.e(LOG_TAG, "fetchPM: http error");
                }
                
            } catch (LoginPasswordEmptyException e) {
                //e.printStackTrace();
                Log.w(LOG_TAG, "fetchPM: No Login/Password set in preferences");                
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {        
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        mPMDBAdapter.close();
    }
    
    
    /**
     * Récupération du contenu des messages du forum
     * 
     * Récupère la liste des messages flagués comme "non lus" sur le site SJLB, c'est à dire :
     * - les nouveaux messages (bien qu'il puisent avoir déjà été récupérés par l'application mobile)
     * - les messages modifiés (qui ont déjà été récupéré, mais dont le contenu a changé entre temps)
     */
    void fetchMsg () {

        try {
            Log.d(LOG_TAG, "fetchMsg...");
            
            // Utilise les préférences pour récupérer le login/mot de passe :
            PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);

            // Récupère la date du message le plus vieux (le premier) et du plus récent (le dernier) connus
            long firstMsgDate   = mMsgDBAdapter.getFirstMsgDate();
            long lastMsgDate    = mMsgDBAdapter.getLastMsgDate();
            
            // Instancie un client HTTP et un header de requête "POST"  
            HttpClient  httpClient  = new DefaultHttpClient();  
            HttpPost    httpPost    = new HttpPost(mContext.getString(R.string.sjlb_msg_uri));  
               
            // Prépare les 4 paramètres POST
            // commence par les 2 infos de login et le password
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
            nameValuePairs.add(new BasicNameValuePair("login",      loginPassword.mLogin));  
            nameValuePairs.add(new BasicNameValuePair("password",   loginPassword.mPasswordMD5));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
            if (   (0 < firstMsgDate)
                && (0 < lastMsgDate) )
            {
                // et si disponibles (ie après la première fois) les 2 dates du plus vieux et du plus récent message
                // TODO SRO : la récupération des anciens messages pose actuellement trop de problèmes de performances dans la vue des sujets
                //nameValuePairs.add(new BasicNameValuePair("date_premier", Long.toString(firstMsgDate)));
                nameValuePairs.add(new BasicNameValuePair("date_dernier", Long.toString(lastMsgDate)));
                Log.d(LOG_TAG, "fetchMsg (" + firstMsgDate +"," + lastMsgDate + ")");
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
                Element                 docElement  = dom.getDocumentElement();
                
                if (null != docElement) {
                    // Récupère la liste des Sujets
                    NodeList    listSubj = docElement.getElementsByTagName(NODE_NAME_FORUM_SUBJ);
                    if (null != listSubj) {
                        int nbSubj = listSubj.getLength();
                        Log.d(LOG_TAG, "listSubj.getLength() = " + nbSubj);
                        for (int i = 0; i < nbSubj; i++) {
                            Element Subj      = (Element)listSubj.item(i);
                            
                            String  strText     = Subj.getFirstChild().getNodeValue();

                            String  strIdSubj   = Subj.getAttribute(ATTR_NAME_FORUM_SUBJ_ID);
                            int     idSubj      = Integer.parseInt(strIdSubj);
                            String  strIdCat    = Subj.getAttribute(ATTR_NAME_FORUM_SUBJ_CAT_ID);
                            int     idCat       = Integer.parseInt(strIdCat);
                            String  strIdGroup  = Subj.getAttribute(ATTR_NAME_FORUM_SUBJ_GROUP_ID);
                            int     idGroup     = Integer.parseInt(strIdGroup);
                            String  strLastDate = Subj.getAttribute(ATTR_NAME_FORUM_SUBJ_DERNIERE_DATE);
                            long    longLastDate= (long)Integer.parseInt(strLastDate);
                            Date    lastDate    = new Date(longLastDate*1000);
                            
                            ForumSubject newSubj = new ForumSubject(idSubj, idCat, idGroup, lastDate, strText);
                            Log.d(LOG_TAG, "Subj " + newSubj);
                            
                            // Renseigne la bdd SSI le sujet n'existe pas, sinon le met simplement à jour
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
                    }
                    
                    // Récupère la liste des Msg
                    NodeList    listMsg = docElement.getElementsByTagName(NODE_NAME_FORUM_MSG);
                    if (null != listMsg) {
                        int nbMsg = listMsg.getLength();
                        Log.d(LOG_TAG, "listMsg.getLength() = " + nbMsg);
                        for (int i = 0; i < nbMsg; i++) {
                            Element Msg      = (Element)listMsg.item(i);
                            
                            String  strText     = Msg.getFirstChild().getNodeValue();

                            String  strIdMsg    = Msg.getAttribute(ATTR_NAME_FORUM_MSG_ID);
                            int     idMsg       = Integer.parseInt(strIdMsg);
                            String  strDate     = Msg.getAttribute(ATTR_NAME_FORUM_MSG_DATE);
                            long    longDate    = (long)Integer.parseInt(strDate);
                            Date    date        = new Date(longDate*1000);
                            String  strIdAuthor = Msg.getAttribute(ATTR_NAME_FORUM_MSG_AUTHOR_ID);
                            int     idAuthor    = Integer.parseInt(strIdAuthor);
                            String  strAuthor   = Msg.getAttribute(ATTR_NAME_FORUM_MSG_AUTHOR);
                            String  strIdSubject= Msg.getAttribute(ATTR_NAME_FORUM_MSG_SUBJECT_ID);
                            int     idSubject   = Integer.parseInt(strIdSubject);
                            String  strUnread   = Msg.getAttribute(ATTR_NAME_FORUM_MSG_UNREAD);
                            boolean bUnread     = (0 != Integer.parseInt(strUnread));
                            
                            ForumMessage newMsg = new ForumMessage(idMsg, date, idAuthor, strAuthor, idSubject, bUnread, strText);
                            Log.d(LOG_TAG, "Msg " + newMsg);
                            
                            // Renseigne la bdd SSI le message n'est pas déjà inséré, sinon fait un update
                            if (mMsgDBAdapter.isExist(idMsg)) {
                                if (mMsgDBAdapter.updateMsg(newMsg)) {
                                    Log.d(LOG_TAG, "Msg " + idMsg + " updated");
                                } else {
                                    Log.e(LOG_TAG, "Msg " + idMsg + " NOT updated !");
                                }
                            } else {
                                if (mMsgDBAdapter.insertMsg(newMsg)) {
                                    Log.d(LOG_TAG, "Msg " + idMsg + " inserted");                                
                                } else {
                                    Log.e(LOG_TAG, "Msg " + idMsg + " NOT inserted !");
                                }                                
                            }

                            // Récupère la liste des fichiers attachés au Msg
                            NodeList    listFile = Msg.getElementsByTagName(NODE_NAME_FORUM_FILE);
                            if (null != listFile) {
                                int nbFile = listFile.getLength();
                                //Log.d(LOG_TAG, "listFile.getLength() = " + nbFile);
                                for (int j = 0; j < nbFile; j++) {
                                    Element FileElement = (Element)listFile.item(j);
//                                    String  FileNameX   = FileElement.getNodeValue();
//                                    Node    NodeFile    = FileElement.getFirstChild();
                                    String  FileName    = FileElement.getFirstChild().getNodeValue();

                                    AttachedFile newAttachedFile = new AttachedFile(idMsg, FileName);
                                    Log.d(LOG_TAG, "Fichier " + newAttachedFile);
                                    
                                    if (mFileDBAdapter.insertFile(newAttachedFile)) {
                                        Log.d(LOG_TAG, "AttachedFile " + FileName + " inserted");                                
                                    } else {
                                        Log.e(LOG_TAG, "AttachedFile " + FileName + " NOT inserted !");
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e(LOG_TAG, "fetchMsg: no XML document: server error");
                    }
                }
            
                Log.d(LOG_TAG, "fetchMsg... ok");                        

            } else {
                Log.e(LOG_TAG, "fetchMsg: http error");
            }
            
        } catch (LoginPasswordEmptyException e) {
            // e.printStackTrace();
            Log.w(LOG_TAG, "fetchMsg: No Login/Password set in Preferences");                                        
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            Log.w(LOG_TAG, "fetchMsg: SAXException");                                        
            e.printStackTrace();
        } catch (ClassCastException e) {        
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        mMsgDBAdapter.close();
        mSubjDBAdapter.close();
        mFileDBAdapter.close();
    }
    
    
    /**
     * Notifie à l'utilisateur le nombre de messages privés lorsqu'un ou plusieurs PM a été reçu
     */
    private void notifyUserPM () {
        Log.d(LOG_TAG, "notifyUserPM");

        // Récupère les préférences de notification :
        PrefsNotification    notificationPrefs        = new PrefsNotification(mContext);

        // Récupère une reference sur le NotificationManager :
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) mContext.getSystemService(ns);

        // Définition du message détaillé à afficher dans le volet de notification
        Context         context             = mContext.getApplicationContext();
        CharSequence    contentTitle        = mContext.getString(R.string.notification_title_pm);
        CharSequence    contentText         = mNbNewPM + " " + mContext.getString(R.string.notification_text_pm);
        
        // Intent à envoyer lorsque l'utilisateur sélectionne la  notification
        Intent          notificationIntent  = new Intent(mContext, ActivityPrivateMessages.class);
        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        // Préparation du résumé de notification
        int             icon            = R.drawable.status_icon;
        CharSequence    tickerText      = mContext.getString(R.string.app_name) + ": " + mNbNewPM + " " + mContext.getString(R.string.notification_text_pm);
        long            when            = System.currentTimeMillis();
        // ... et instanciation de la Notification :
        Notification    notification    = new Notification(icon, tickerText, when);

        notification.number     = mNbNewPM;
        
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
    private void notifyUserMsg () {
        Log.d(LOG_TAG, "notifyUserMsg");
        
        // Récupère une reference sur le NotificationManager :
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) mContext.getSystemService(ns);
    
        if (0 < mNbUnreadMsg) {
            // Récupère les préférences de notification :
            PrefsNotification    notificationPrefs        = new PrefsNotification(mContext);
        
            // Définition du message détaillé à afficher dans le volet de notification
            Context         context             = mContext.getApplicationContext();
            CharSequence    contentTitle        = mContext.getString(R.string.notification_title_msg);
            CharSequence    contentText         = mNbUnreadMsg + " " + mContext.getString(R.string.notification_text_msg) + " (" + mNbNewMsg + mContext.getString(R.string.notification_new) + ")";
            
            // Intent à envoyer lorsque l'utilisateur sélectionne la  notification
            // => lien activité principale :
            Intent          notificationIntent  = new Intent(mContext, ActivityMain.class);
            PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
        
            // Préparation du résumé de notification
            int          icon        = R.drawable.status_icon;
            CharSequence tickerText  = mContext.getString(R.string.app_name) + ": " + mNbUnreadMsg + " " + mContext.getString(R.string.notification_text_msg) + " (" + mNbNewMsg + mContext.getString(R.string.notification_new) + ")";
            long         when        = System.currentTimeMillis();
        
            // et instanciation de la Notification :
            Notification notification = new Notification(icon, tickerText, when);
        
            notification.number     = mNbUnreadMsg;
        
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
        } else {
            // Suppression de la Notification
            notificationManager.cancel(NOTIFICATION_NEW_MSG_ID);
        }
    }    
}
