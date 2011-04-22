package fr.srombauts.sjlb;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * Travail en tâche de fond, chargée de récupérer le fichier XML listant les messages non lus
 */
class AsynchTaskRefresh extends AsyncTask<Void, Void, Void> {

    public static final  int     NOTIFICATION_NEW_PM_ID     = 1;
    public static final  int     NOTIFICATION_NEW_MSG_ID    = 2;
    private static final String  LOG_TAG                    = "RefreshTask";

    static final private String NODE_NAME_LOGIN_ID          = "id_login";
    static final private String NODE_NAME_PRIVATE_MSG       = "pm";
    static final private String NODE_NAME_PRIVATE_MSG_ID    = "id";
    static final private String NODE_NAME_FORUM_MSG         = "msg";
    static final private String NODE_NAME_FORUM_MSG_ID      = "id";

    static final private String ATTR_NAME_PRIVATE_MSG_ID        = "id";
    static final private String ATTR_NAME_PRIVATE_MSG_DATE      = "date";
    static final private String ATTR_NAME_PRIVATE_MSG_ID_AUTHOR = "id_auteur";
    static final private String ATTR_NAME_PRIVATE_MSG_PSEUDO    = "pseudo";
    

    private ServiceRefresh mContext        = null;
    private int         mIdLogin        = 0;
    private int         mNbPM           = 0;
    private int         mNbNewPM        = 0;
    private int         mNbMsg          = 0;
    private int         mNbNewMsg       = 0;

    private ContentProviderPM   mPMDBAdapter = null;
    private ContentProviderMsg  mMsgDBAdapter = null;
      
    /**
     * Constructeur utilisé pour mémorisée la référence sur le service appelant
     * @param context
     */
    public AsynchTaskRefresh(ServiceRefresh context) {
        mContext        = context;
                                
        mPMDBAdapter  = new ContentProviderPM(context);
        mMsgDBAdapter = new ContentProviderMsg(context);
    }

    protected void onPreExecute() {
        // Toast notification de début de rafraichissement (pour le debug uniquement !)
        // Toast.makeText(mContext, mContext.getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
    }
    
    
    /**
     * Lance la récupération et le parse de la liste XML des messages non lus
     * 
     * Ce travail s'exécute en tâche de fond, et n'a donc pas le droit d'effectuer d'actions sur la GUI
     */
    protected Void doInBackground(Void... params) {
        
        // Recherche d'éventuelles nouveautés
        refreshInfos ();
        
        // Notifications des éventuelles nouveautés
        publishProgress ();
        
        // et récupération de ces éventuels nouveaux contenus
        fetchPM ();
        // TODO : fetchMsg ();
        
        return null;
    }
    
    /**
     * Récupération des listes d'identifiants de messages non lus
     */
    void refreshInfos () {
        Log.d(LOG_TAG, "refreshInfos...");

        mNbNewPM    = 0;
        mNbNewMsg   = 0;
        
        try
        {
            // Utilise les préférences pour sauvegarder le login/mot de passe :
            SharedPreferences   Prefs       = PreferenceManager.getDefaultSharedPreferences(mContext);
            String              login       = Prefs.getString(SJLB.PREFS.LOGIN,    ""); // "Seb";
            String              password    = Prefs.getString(SJLB.PREFS.PASSWORD, ""); // "mmdpsjlb";

            if (   (false == login.contentEquals(""))
                && (false == password.contentEquals("")) )
            {
                // Génère le hash MD5 du mot de passe
                String          passwordMD5 = ""; // "4df51b1810f131b7f6a794900d93d58e";
                try
                {
                    MessageDigest digest = java.security.MessageDigest.getInstance("MD5");  
                    digest.update(password.getBytes());  
                    byte[] messageDigest = digest.digest();
                    BigInteger number = new BigInteger(1,messageDigest);
                    passwordMD5 = number.toString(16);
               
                    while (passwordMD5.length() < 32) {
                        passwordMD5 = "0" + passwordMD5;
                    }
                    
                } catch (NoSuchAlgorithmException e) {  
                    e.printStackTrace();  
                }
                
//                Log.d(LOG_TAG, "login=Seb password=4df51b1810f131b7f6a794900d93d58e");
//                Log.d(LOG_TAG, "login=" + login + " password=" + passwordMD5 + "(" + password + ")");
//                Log.d(LOG_TAG, "urlAPI: " + mContext.getString(R.string.sjlb_polling_uri) + "?login=" + login + "&password=" + passwordMD5);
                
                URL                 urlAPI          = new URL(mContext.getString(R.string.sjlb_polling_uri) + "?login=" + login + "&password=" + passwordMD5);
                URLConnection       connection      = urlAPI.openConnection();
                HttpURLConnection   httpConnection  = (HttpURLConnection)connection;
                
                // Etablissement de la connexion http et récupération de la page Web
                if (HttpURLConnection.HTTP_OK == httpConnection.getResponseCode()) {
                    // Parse de la page XML
                    InputStream             in  = httpConnection.getInputStream();
                    
                    DocumentBuilderFactory  dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder         db  = dbf.newDocumentBuilder();
                    Document                dom = db.parse(in);
                    Element                 docElement = dom.getDocumentElement();
                    
                    // Récupère l'id de l'utilsateur loggé
                    // TODO SRO : traiter ici les cas de noeuds "null" (cas du login/mdp erroné par exemple) ! 
                    Element eltIdLogin  = (Element)docElement.getElementsByTagName(NODE_NAME_LOGIN_ID).item(0);
                    String  strIdLogin  = eltIdLogin.getFirstChild().getNodeValue();
                    mIdLogin            = Integer.parseInt(strIdLogin);
                    
                    // Récupère la liste des PM
                    Element     eltPM  = (Element)docElement.getElementsByTagName(NODE_NAME_PRIVATE_MSG).item(0);
                    NodeList    listPM = eltPM.getElementsByTagName(NODE_NAME_PRIVATE_MSG_ID);
                    if (null != listPM)
                    {
                        mNbPM = listPM.getLength();
                        for (int i = 0; i < mNbPM; i++) {
                            Element pm      = (Element)listPM.item(i);
                            String  strIdPM = pm.getFirstChild().getNodeValue();
                            int     idPM    = Integer.parseInt(strIdPM);
                            
                            // Renseigne la bdd si PM inconnu
                            long nbInserted = mPMDBAdapter.insertPM(idPM);
                            if (-1 != nbInserted) {
                                mNbNewPM++;
                            }
                        }
                    }
                    // TODO SRO : détecter aussi les cas où l'on dispose de PLUS de PM en local qu'il n'en reste sur le site !
                    
                    // Récupère la liste des Messages
                    Element     eltMsg  = (Element)docElement.getElementsByTagName(NODE_NAME_FORUM_MSG).item(0);
                    NodeList    listMsg = eltMsg.getElementsByTagName(NODE_NAME_FORUM_MSG_ID);
                    if (null != listMsg)
                    {
                        mNbMsg = listMsg.getLength();
                        for (int i = 0; i < mNbMsg; i++) {
                            Element msg      = (Element)listMsg.item(i);
                            String  strIdMsg = msg.getFirstChild().getNodeValue();
                            int     idMsg    = Integer.parseInt(strIdMsg);
                            
                            // Renseigne la bdd si Message inconnu
                            long nbInserted = mMsgDBAdapter.insertMsg(idMsg);
                            if (-1 != nbInserted) {
                                mNbNewMsg++;
                            }
                        }
                    }
                    Log.d(LOG_TAG, "refreshInfos... ok");
                }
            }
               
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
    
    
    /**
     * Publication en cours de travail
     *
     * Cette méthode est synchronisée donc une action sur la GUI est autorisée 
     */
    protected void onProgressUpdate(Void... values) {
      super.onProgressUpdate(values);
      
      // s'il y a de nouveaux messages non lus :
      if (0 < mNbNewPM)
      {
          // Notification dans la barre de status
          notifyUserPM ();
      }
      if (0 < mNbNewMsg)
      {
          // Notification dans la barre de status
          notifyUserMsg ();
      }
    }
    
    
    /**
     * Récupération du contenu des messages privés
     */
    void fetchPM () {
        Log.d(LOG_TAG, "fetchPM...");
        
        // s'il y a de nouveaux messages non lus :
        // TODO SRO : pas bon, il faut aussi le faire lorsqu'il y a MOINS de messages que la fois précédentes !
        if (0 < mNbNewPM)
        {
            try
            {
                // Utilise les préférences pour sauvegarder le login/mot de passe :
                SharedPreferences   Prefs       = PreferenceManager.getDefaultSharedPreferences(mContext);
                String              login       = Prefs.getString(SJLB.PREFS.LOGIN,    ""); // "Seb";
                String              password    = Prefs.getString(SJLB.PREFS.PASSWORD, ""); // "mmdpsjlb";

                if (   (false == login.contentEquals(""))
                    && (false == password.contentEquals("")) )
                {
                    // Génère le hash MD5 du mot de passe
                    String          passwordMD5 = ""; // "4df51b1810f131b7f6a794900d93d58e";
                    try
                    {
                        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");  
                        digest.update(password.getBytes());  
                        byte[] messageDigest = digest.digest();
                        BigInteger number = new BigInteger(1,messageDigest);
                        passwordMD5 = number.toString(16);
                   
                        while (passwordMD5.length() < 32) {
                            passwordMD5 = "0" + passwordMD5;
                        }
                        
                    } catch (NoSuchAlgorithmException e) {  
                        e.printStackTrace();  
                    }
                    
//                    Log.d(LOG_TAG, "login=Seb password=4df51b1810f131b7f6a794900d93d58e");
//                    Log.d(LOG_TAG, "login=" + login + " password=" + passwordMD5 + " (" + password + ")");
//                    Log.d(LOG_TAG, "urlAPI: " + mContext.getString(R.string.sjlb_pm_uri) + "?login=" + login + "&password=" + passwordMD5);
                    
                    URL                 urlAPI          = new URL(mContext.getString(R.string.sjlb_pm_uri) + "?login=" + login + "&password=" + passwordMD5);
                    URLConnection       connection      = urlAPI.openConnection();
                    HttpURLConnection   httpConnection  = (HttpURLConnection)connection;
                    
                    // Etablissement de la connexion http et récupération de la page Web
                    if (HttpURLConnection.HTTP_OK == httpConnection.getResponseCode()) {
                        // Parse de la page XML
                        InputStream             in  = httpConnection.getInputStream();
                        
                        DocumentBuilderFactory  dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder         db  = dbf.newDocumentBuilder();
                        Document                dom = db.parse(in);
                        Element                 docElement = dom.getDocumentElement();
                        
                        // Récupère la liste des PM
                        NodeList    listPM = docElement.getElementsByTagName(NODE_NAME_PRIVATE_MSG);
                        if (null != listPM)
                        {
                            // Vide la table des messages privés
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
                                String  strIdAuthor = pm.getAttribute(ATTR_NAME_PRIVATE_MSG_ID_AUTHOR);
                                int     idAuthor    = Integer.parseInt(strIdAuthor);
                                String  strAuthor   = pm.getAttribute(ATTR_NAME_PRIVATE_MSG_PSEUDO);
                                
                                Log.d(LOG_TAG, "PM " + idPM + " " + strAuthor + " ("+ idAuthor +") " + date + " (" + strDate + ") : "  + strText);
                                
                                PrivateMessage newPM = new PrivateMessage(idPM, date, strAuthor, strText);
                                
                                // Renseigne la bdd
                                Boolean bInserted = mPMDBAdapter.insertPM(newPM);
                                if (bInserted) {
                                    Log.d(LOG_TAG, "PM " + idPM + " inserted");                                
                                }
                            }
                        }
                        Log.d(LOG_TAG, "fetchPM... ok");                        
                    }
                }
                
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
    }
    
    
    /**
     * Fin de refresh
     *
     * Cette méthode est synchronisée donc on met à jour l'affichage de la liste 
     */
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);

      // Le service peut dès lors être interrompu une fois qu'il a effectué le rafraichissement
      mContext.stopSelf ();
    }

    /**
     *  Notifie à l'utilisateur les évolutions du nombre de messages privés non lus
     */
    private void notifyUserPM () {
        Log.d(LOG_TAG, "notifyUserPM");

        // Get a reference to the NotificationManager:
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) mContext.getSystemService(ns);

        // Instantiate the Notification:
        int          icon        = R.drawable.status_icon;
        CharSequence tickerText  = mContext.getString(R.string.app_name) + ": " + mContext.getString(R.string.notification_title_pm);
        long         when        = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);

        // Define the Notification's expanded message and Intent:
        Context         context             = mContext.getApplicationContext();
        CharSequence    contentTitle        = mContext.getString(R.string.notification_title_pm);
        // TODO SRO : faire le cumul avec les chiffres de l'éventuelle notification déjà actuellement affichée 
        CharSequence    contentText         = mNbNewPM + " " + mContext.getString(R.string.notification_text_pm);

        // Intent à envoyer lorsque l'utilisateur sélectionne la  notification
        Intent          notificationIntent  = new Intent(mContext, ActivityPrivateMessages.class);
        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        notification.number     = mNbNewPM;
        notification.defaults   = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND;
        notification.vibrate    = new long[]{0, 200, 500, 200};

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        // Pass the Notification to the NotificationManager:
        notificationManager.notify(NOTIFICATION_NEW_PM_ID, notification);
    }    

    /**
     *  Notifie à l'utilisateur les évolutions du nombre de messages non lus
     */
    private void notifyUserMsg () {
        Log.d(LOG_TAG, "notifyUserMsg");
        
        // Get a reference to the NotificationManager:
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) mContext.getSystemService(ns);

        // Instantiate the Notification:
        int          icon        = R.drawable.status_icon;
        CharSequence tickerText  = mContext.getString(R.string.app_name) + ": " + mContext.getString(R.string.notification_title_msg);
        long         when        = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);

        // Define the Notification's expanded message and Intent:
        Context         context             = mContext.getApplicationContext();
        CharSequence    contentTitle        = mContext.getString(R.string.notification_title_msg);
        // TODO SRO : faire le cumul avec les chiffres de l'éventuelle notification déjà actuellement affichée 
        CharSequence    contentText         = mNbNewMsg + " " + mContext.getString(R.string.notification_text_msg);

        Intent          notificationIntent  = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(mContext.getString(R.string.sjlb_forum_uri)));
        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        notification.number     = mNbNewMsg;
        notification.defaults   = Notification.DEFAULT_LIGHTS + Notification.DEFAULT_SOUND;
        notification.vibrate    = new long[]{0, 200, 300, 200, 300, 200};
        
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        // Pass the Notification to the NotificationManager:
        notificationManager.notify(NOTIFICATION_NEW_MSG_ID, notification);
    }    
}


