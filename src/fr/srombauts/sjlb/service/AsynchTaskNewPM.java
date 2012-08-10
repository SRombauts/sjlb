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

import android.content.Context;
import android.net.ParseException;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.ContentProviderPM;
import fr.srombauts.sjlb.model.LoginPasswordEmptyException;
import fr.srombauts.sjlb.model.PrefsLoginPassword;
import fr.srombauts.sjlb.model.PrivateMessage;


/**
 * Travail en tâche de fond, chargée d'envoyer un nouveau message privé
 */
public class AsynchTaskNewPM extends AsyncTask<String, Void, Boolean> {
    private static final String  LOG_TAG                    = "NewPMTask";

    public static final  int     NOTIFICATION_NEW_PM_ID     = 1;
    public static final  int     NOTIFICATION_NEW_MSG_ID    = 2;

    static final private String NODE_NAME_PRIVATE_MSG       = "pm";

    static final private String ATTR_NAME_PRIVATE_MSG_ID        = "id";
    static final private String ATTR_NAME_PRIVATE_MSG_DATE      = "date";
    static final private String ATTR_NAME_PRIVATE_MSG_ID_AUTHOR = "id_auteur";
    static final private String ATTR_NAME_PRIVATE_MSG_PSEUDO    = "pseudo";
    

    private Context         mContext        = null;
    private int             mNbPM           = 0;
    
    private ContentProviderPM   mPMDBAdapter  = null;
      
    /**
     * Constructeur utilisé pour mémorisée la référence sur le service appelant
     * @param context
     */
    public AsynchTaskNewPM(Context context) {
        mContext      = context;
                                
        mPMDBAdapter  = new ContentProviderPM(context);
    }

    protected void onPreExecute() {
        // Toast notification de début de rafraîchissement
        Toast.makeText(mContext, mContext.getString(R.string.toast_sending), Toast.LENGTH_SHORT).show();
    }
    
    
    /**
     * Lance la récupération et le parse de la liste XML des messages non lus
     * 
     * Ce travail s'exécute en tâche de fond, et n'a donc pas le droit d'effectuer d'actions sur la GUI
     */
    protected Boolean doInBackground(String... args) {
        Boolean bResult;
        
        // Envoi du pm, et récupération des éventuels nouveaux contenus
        bResult = sendPM (args[0], args[1]);
        
        return bResult;
    }
    
    
    /**
     * Publication en cours de travail
     *
     * Cette méthode est synchronisée donc une action sur la GUI est autorisée 
     */
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
    
    
    /**
     * Envoi du nouveau message, et récupération du contenu des messages privés
     */
    Boolean sendPM (String aIdDestinataire, String aText) {
        Boolean bResult = false;
        
        Log.d(LOG_TAG, "sendPM (" + aIdDestinataire + ") : " + aText + " ...");
        
        try {
            
            // Utilise les préférences pour récupérer le login/mot de passe :
            PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);
            
            // Instancie un client http et un header de requète "POST"  
            HttpClient  httpClient  = new DefaultHttpClient();  
            HttpPost    httpPost    = new HttpPost(mContext.getString(R.string.sjlb_pm_uri));  
               
            // Ajout des paramètres
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
            nameValuePairs.add(new BasicNameValuePair("login",              loginPassword.mLogin));  
            nameValuePairs.add(new BasicNameValuePair("password",           loginPassword.mPasswordMD5));
            nameValuePairs.add(new BasicNameValuePair("id_destinataire",    aIdDestinataire));
            nameValuePairs.add(new BasicNameValuePair("Message",            aText));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
            
            // Execute HTTP Post Request  
            HttpResponse response = httpClient.execute(httpPost);
            if (HttpStatus.SC_OK == response.getStatusLine ().getStatusCode())
            {
                // Récupère le contenu de la réponse
                InputStream             in          = response.getEntity().getContent();
            
                Log.d(LOG_TAG, "sendPM ok");                        
                
                // TODO SRombauts mutualiser ce qui suit pour l'utiliser aussi lors d'un effacement de pm
                // Parse de la page XML
                DocumentBuilderFactory  dbf         = DocumentBuilderFactory.newInstance();
                dbf.setCoalescing(true);
                DocumentBuilder         db          = dbf.newDocumentBuilder();
                Document                dom         = db.parse(in);
                Element                 docElement  = dom.getDocumentElement();
                
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
                        
                        PrivateMessage newPM = new PrivateMessage(idPM, date, idAuthor, strAuthor, strText);
                        
                        // Renseigne la bdd
                        Boolean bInserted = mPMDBAdapter.insertPM(newPM);
                        if (bInserted) {
                            Log.d(LOG_TAG, "PM " + idPM + " inserted");
                        }
                    }
                }
                Log.d(LOG_TAG, "sendPM... ok");
                bResult = true;
            }
            
        } catch (LoginPasswordEmptyException e) {
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
        
        return bResult;
    }
    
    
    /**
     * Fin d'envoi
     *
     * Cette méthode est synchronisée donc on peut y faire des notifications
     */
    protected void onPostExecute(Boolean abResult) {
        super.onPostExecute(abResult);

        if (abResult) {
            // Toast notification de fin d'envoi
            Toast.makeText(mContext, mContext.getString(R.string.toast_sent), Toast.LENGTH_SHORT).show();
        }
        else {
            // Toast notification d'erreur d'envoi !
            Toast.makeText(mContext, mContext.getString(R.string.toast_not_sent), Toast.LENGTH_SHORT).show();
        }
    }
}

