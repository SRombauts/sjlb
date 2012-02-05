package fr.srombauts.sjlb;

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
import android.net.ParseException;
import android.os.AsyncTask;
import android.util.Log;


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
class AsynchTaskRefresh extends AsyncTask<Void, Void, Void> {
    private static final String  LOG_TAG                    = "RefreshTask";

    public static final  int     NOTIFICATION_NEW_PM_ID     = 1;
    public static final  int     NOTIFICATION_NEW_MSG_ID    = 2;

    static final private String NODE_NAME_PRIVATE_MSG       = "pm";
    static final private String NODE_NAME_PRIVATE_MSG_ID    = "id";
    static final private String NODE_NAME_FORUM_MSG         = "msg";
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
    private int             mNbMsg          = 0;    // Nombre de messages non lus par l'utilisateur (issu directement de la liste XML)
    private int             mNbNewMsg       = 0;    // Nombre de messages non lus pour l'utilisateur
    
    private ContentProviderPM       mPMDBAdapter    = null;
    private ContentProviderSubj     mSubjDBAdapter  = null;
    private ContentProviderMsg      mMsgDBAdapter   = null;
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
        mUserDBAdapter  = new ContentProviderUser(context);
    }


    /**
     * Avant début de refresh
     *
     * Cette méthode est synchronisée avec le thread main, donc on peut y demander des modifs de GUI (notifications...)
     */
    protected void onPreExecute() {
        // Toast notification de début de rafraichissement
        // SRO COMMENTE : comme on tourne dans un service en tache de fond, on ne veut pas gêner l'utilisateur avec une notification
        // Toast.makeText(mContext, mContext.getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
    }
    
    
    /**
     * Lance la récupération et le parse de la liste XML des messages non lus
     * 
     * Ce travail s'exécute en tâche de fond, et n'a donc pas le droit d'effectuer d'actions sur la GUI
     */
    protected Void doInBackground(Void... args) {
        
        // Récupération de la liste des utilisateurs (seulement si la BDD est vide)
        refreshUsers ();
        
        // Recherche d'éventuelles nouveautés ()
        refreshInfos ();
        
        // et récupération de ces éventuels nouveaux contenus (seulement si nécessaire)
        fetchPM ();
        fetchMsg ();
        
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
      // s'il y a de nouveaux messages non lus :
      if (0 < mNbNewMsg) {
          // Notification dans la barre de status
          notifyUserMsg ();
      }
      
      // Le service peut dès lors être interrompu une fois qu'il a effectué le rafraichissement
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
    void refreshUsers () {
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
                    
                    // Récupère la liste des utilisateurs
                    NodeList    listPM = (NodeList)docElement.getElementsByTagName(NODE_NAME_USER);
                    if (null != listPM) {
                        int nbUsers = listPM.getLength();
                        for (int i = 0; i < nbUsers; i++) {
                            Element user        = (Element)listPM.item(i);
    
                            String  strIdUser   = user.getAttribute(ATTR_NAME_USER_ID);
                            int     idUser      = Integer.parseInt(strIdUser);
                            String  strPseudo   = user.getAttribute(ATTR_NAME_USER_PSEUDO);
                            String  strName     = user.getAttribute(ATTR_NAME_USER_NAME);
                            
                            Log.d(LOG_TAG, "User " + idUser + " " + strPseudo + " " + strName);
                            
                            User newUser = new User(idUser, strPseudo, strName);
                            
                            // Renseigne la bdd si User inconnu
                            // TODO SRO : un peu moche, provoque une exception SQL si le PM est déjà en base (pas propre en débug)
                            mUserDBAdapter.insertUser(newUser);
                        }
                    }
    
                    Log.d(LOG_TAG, "refreshUsers... ok");
                }
                   
            } catch (LoginPasswordException e) {
                e.printStackTrace();
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
     * Récupération des listes d'identifiants de messages non lus
     */
    void refreshInfos () {

        mNbNewPM    = 0;
        mNbNewMsg   = 0;
        
        try {
            Log.d(LOG_TAG, "refreshInfos...");
            
            // Utilise les préférences pour récupérer le login/mot de passe :
            PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);

            // Instancie un client http et un header de requète "POST"
            HttpClient  httpClient  = new DefaultHttpClient();  
            HttpPost    httpPost    = new HttpPost(mContext.getString(R.string.sjlb_polling_uri));  
               
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
                
                // Récupère la liste des PM
                Element     eltPM  = (Element)docElement.getElementsByTagName(NODE_NAME_PRIVATE_MSG).item(0);
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
                // Détection du cas où l'on dispose de PLUS de PM en BDD locale qu'il n'en reste sur le site !
                if (0 == mNbNewPM ) {
                    int nbPMInBdd = (int)mPMDBAdapter.countPM();
                    if (mNbPM < nbPMInBdd) {
                        // nombre négatif, un(des) PM a(ont) été supprimé(s)  !
                        mNbNewPM = mNbPM - nbPMInBdd;
                    }
                }
                
                // Récupère la liste des Messages
                Element     eltMsg  = (Element)docElement.getElementsByTagName(NODE_NAME_FORUM_MSG).item(0);
                NodeList    listMsg = eltMsg.getElementsByTagName(NODE_NAME_FORUM_MSG_ID);
                if (null != listMsg) {
                    mNbMsg = listMsg.getLength();
                    for (int i = 0; i < mNbMsg; i++) {
                        Element msg      = (Element)listMsg.item(i);
                        String  strIdMsg = msg.getFirstChild().getNodeValue();
                        int     idMsg    = Integer.parseInt(strIdMsg);
                        
                        // Renseigne la bdd si Message inconnu
                        // TODO SRO : PEUT ÊTRE PAS, il faudrait peut être juste tester si le message est nouveau, et le mettre en base plus bas, dans "fetchMsg()"
                        if (false == mMsgDBAdapter.isExist(idMsg)) {
                            Log.d(LOG_TAG, "Msg " + idMsg + " nouveau");
                            mNbNewMsg++;
                        }
                        else {
                            Log.d(LOG_TAG, "Msg " + idMsg + " recuperes precedemment");
                        }
                    }
                }
                Log.d(LOG_TAG, "refreshInfos... ok : mNbNewPM="+mNbNewPM+" ("+mNbPM+"), mNbNewMsg="+mNbNewMsg+" ("+mNbMsg+")");
            }
               
        } catch (LoginPasswordException e) {
            e.printStackTrace();
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
     * Récupération du contenu des messages privés
     */
    void fetchPM () {
    
        // s'il y a de nouveaux PM (ou au contraire s'il y en a moins) :
        if (0 != mNbNewPM) {
            // Le nombre de nouveau PM va être recalculé par rapport à ce qui sera réellement mis en BDD (pour servir ensuite à la notification)
            // TODO SRO : seulement lorsqu'on ne fera plus un "clearAllPM" avant de re-peupler la base
            //mNbNewPM = 0;
    
            try {
                Log.d(LOG_TAG, "fetchPM...");
                
                // Utilise les préférences pour récupérer le login/mot de passe :
                PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);
                
                // Instancie un client http et un header de requète "POST"  
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
                    
                    // Récupère la liste des PM
                    NodeList    listPM = docElement.getElementsByTagName(NODE_NAME_PRIVATE_MSG);
                    if (null != listPM) {
                    
                        // Vide la table des messages privés
                        // TODO SRO : c'est moche, au lieu de tout vider il faudrait plutôt supprimer les messages ayant disparus, et ne réinserer que les nouveaux !
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
                                // TODO SRO : seulement lorsqu'on ne fera plus un "clearAllPM" avant de re-peupler la base
                                //mNbNewPM++;
                            }
                        }
                    }
                    Log.d(LOG_TAG, "fetchPM... ok");                        
                }
                
            } catch (LoginPasswordException e) {
                e.printStackTrace();
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
     * Récupération du contenu des messages du forum
     */
    void fetchMsg () {

        // s'il y a de nouveaux Msg
//      if (0 < mNbNewMsg) {
        // TODO SRO : en fait même pas, il faudrait simplement toujours demander la liste des derniers messages, elle serait vide
        // TODO SRO : ET EN PLUS il faut aussi redemander les messages marqués "non lus" qui ont en fait simplement été édités par un user, alors qu'on les avait déjà !  
        if (true) {
            // Le nombre de nouveau Msg va être recalculé par rapport à ce qui sera réellement mis en BDD (pour servir ensuite à la notification)
            // TODO SRO mNbNewMsg   = 0;
            try {
                Log.d(LOG_TAG, "fetchMsg...");
                
                // Utilise les préférences pour récupérer le login/mot de passe :
                PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);

                // Récupère la date du message le plus récent déjà lu
                long lastMsgDate = mMsgDBAdapter.getLastMsgDate();
                
                // Instancie un client http et un header de requète "POST"  
                HttpClient  httpClient  = new DefaultHttpClient();  
                HttpPost    httpPost    = new HttpPost(mContext.getString(R.string.sjlb_msg_uri));  
                   
                // Ajout des paramètres
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
                nameValuePairs.add(new BasicNameValuePair("login",    loginPassword.mLogin));  
                nameValuePairs.add(new BasicNameValuePair("password", loginPassword.mPasswordMD5));
                if (0 < lastMsgDate) {
                    nameValuePairs.add(new BasicNameValuePair("date_dernier", Long.toString(lastMsgDate)));
                    Log.d(LOG_TAG, "fetchMsg (" + lastMsgDate + ")");
                }
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

                            String  strIdMsg     = Msg.getAttribute(ATTR_NAME_FORUM_MSG_ID);
                            int     idMsg        = Integer.parseInt(strIdMsg);
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
                            //Log.d(LOG_TAG, "Msg " + newMsg);
                            
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
                        }
                    }
                    Log.d(LOG_TAG, "fetchMsg... ok");                        
                }
                
            } catch (LoginPasswordException e) {
                e.printStackTrace();
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
     *  Notifie à l'utilisateur les évolutions du nombre de messages privés non lus
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
        CharSequence    contentText         = mNbPM + " " + mContext.getString(R.string.notification_text_pm) + " (" + mNbNewPM + mContext.getString(R.string.notification_new) + ")";
        
        // Intent à envoyer lorsque l'utilisateur sélectionne la  notification
        Intent          notificationIntent  = new Intent(mContext, ActivityPrivateMessages.class);
        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        // Préparation du résumé de notification
        // TODO SRO : utiliser une icone pour les PM, et une autre pour les Posts
        int             icon            = R.drawable.status_icon;
        CharSequence    tickerText      = mContext.getString(R.string.app_name) + ": " + mNbPM + " " + mContext.getString(R.string.notification_text_pm) + " (" + mNbNewPM + mContext.getString(R.string.notification_new) + ")";
        long            when            = System.currentTimeMillis();
        // ... et instanciation de la Notification :
        Notification    notification    = new Notification(icon, tickerText, when);

        notification.number     = mNbPM;
        
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
     */
    private void notifyUserMsg () {
        Log.d(LOG_TAG, "notifyUserMsg");
        
        // Récupère les préférences de notification :
        PrefsNotification    notificationPrefs        = new PrefsNotification(mContext);

        // Récupère une reference sur le NotificationManager :
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) mContext.getSystemService(ns);

        // Définition du message détaillé à afficher dans le volet de notification
        Context         context             = mContext.getApplicationContext();
        CharSequence    contentTitle        = mContext.getString(R.string.notification_title_msg);
        CharSequence    contentText         = mNbMsg + " " + mContext.getString(R.string.notification_text_msg) + " (" + mNbNewMsg + mContext.getString(R.string.notification_new) + ")";
        
        // Intent à envoyer lorsque l'utilisateur sélectionne la  notification
        // => lien Site Web :
// TODO SRO : ancienne méthode
//        Intent          notificationIntent  = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(mContext.getString(R.string.sjlb_forum_uri)));
//        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
        // Intent à envoyer lorsque l'utilisateur sélectionne la  notification
        // => lien activité principale :
// TODO SRO : pas encore au point
        Intent          notificationIntent  = new Intent(mContext, ActivityMain.class);
        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        // Préparation du résumé de notification
        // TODO SRO : utiliser une icone pour les PM, et une autre pour les Posts
        int          icon        = R.drawable.status_icon;
        CharSequence tickerText  = mContext.getString(R.string.app_name) + ": " + mNbMsg + " " + mContext.getString(R.string.notification_text_msg) + " (" + mNbNewMsg + mContext.getString(R.string.notification_new) + ")";
        long         when        = System.currentTimeMillis();

        // et instanciation de la Notification :
        Notification notification = new Notification(icon, tickerText, when);

        notification.number     = mNbMsg;

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
}

