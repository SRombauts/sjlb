package fr.srombauts.sjlb.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.net.ParseException;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.ContentProviderMsg;
import fr.srombauts.sjlb.model.LoginPasswordEmptyException;
import fr.srombauts.sjlb.model.PrefsLoginPassword;


/**
 * Travail en tâche de fond, chargée d'envoyer un nouveau message du forum
 */
public class AsynchTaskNewMsg extends AsyncTask<String, Void, Boolean> {
    private static final String  LOG_TAG            = "NewMsgTask";

    private Context             mContext            = null;
    private CallbackTransfer    mCallbackTransfer   = null;
    
    private ContentProviderMsg  mMsgDBAdapter       = null;
      
    /**
     * Constructeur utilisé pour mémorisée la référence sur le service appelant
     * @param context Activité lançant le transfert, devant implémenter l'interface CallbackTransfer
     */
    public AsynchTaskNewMsg(Context context) {
        mContext        = context;
        mCallbackTransfer = (CallbackTransfer)context;
                                
        mMsgDBAdapter   = new ContentProviderMsg(context);
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
        bResult = sendMsg (args[0], args[1], args[2], args[3]);
        
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
    Boolean sendMsg (String aIdCategory, String aIdSubject, String aIdGroup, String aText) {
        Boolean bResult = false;
        
        Log.d(LOG_TAG, "sendMsg (" + aIdCategory + ", " + aIdSubject + ", " + aIdGroup + ") : " + aText + " ...");
        
        try {
            
            // Utilise les préférences pour récupérer le login/mot de passe :
            PrefsLoginPassword loginPassword = new PrefsLoginPassword(mContext);
            
            // Récupère la date du message le plus récent déjà lu
            long lastMsgDate = mMsgDBAdapter.getLastMsgDate();
            
            // Instancie un client http et un header de requète "POST"  
            HttpClient  httpClient  = new DefaultHttpClient();  
            HttpPost    httpPost    = new HttpPost(mContext.getString(R.string.sjlb_msg_uri));  
               
            // Ajout des paramètres
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
            nameValuePairs.add(new BasicNameValuePair("login",          loginPassword.mLogin));  
            nameValuePairs.add(new BasicNameValuePair("password",       loginPassword.mPasswordMD5));
            if (0 < lastMsgDate) {
                nameValuePairs.add(new BasicNameValuePair("date_dernier", Long.toString(lastMsgDate)));
                Log.d(LOG_TAG, "fetchMsg (" + lastMsgDate + ")");
            }
            nameValuePairs.add(new BasicNameValuePair("id_categorie",   aIdCategory));
            nameValuePairs.add(new BasicNameValuePair("id_sujet",       aIdSubject));
            nameValuePairs.add(new BasicNameValuePair("id_groupe",      aIdGroup));
            nameValuePairs.add(new BasicNameValuePair("message",        aText));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
            
            // Execute HTTP Post Request  
            HttpResponse response = httpClient.execute(httpPost);
            if (HttpStatus.SC_OK == response.getStatusLine ().getStatusCode())
            {
                // Récupère le contenu de la réponse
                // TODO SRO : parser la réponse comme dans RefreshTask (ou au moins partiellement)
                //InputStream             in          = response.getEntity().getContent();
                
                Log.d(LOG_TAG, "sendMsg ok ");
                bResult = true;
            }
            
        } catch (LoginPasswordEmptyException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
        
        // Notifie l'activité appelante à l'aide d'une callback
        mCallbackTransfer.onTransferDone(abResult);
    }
}
