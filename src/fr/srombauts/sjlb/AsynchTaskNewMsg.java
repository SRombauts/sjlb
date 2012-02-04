package fr.srombauts.sjlb;

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


/**
 * Travail en tâche de fond, chargée d'envoyer un nouveau message du forum
 */
class AsynchTaskNewMsg extends AsyncTask<String, Void, Void> {
    private static final String  LOG_TAG        = "NewMsgTask";

    private Context             mContext        = null;
    
    private ContentProviderMsg  mMsgDBAdapter   = null;
      
    /**
     * Constructeur utilisé pour mémorisée la référence sur le service appelant
     * @param context
     */
    public AsynchTaskNewMsg(Context context) {
        mContext      = context;
                                
        mMsgDBAdapter  = new ContentProviderMsg(context);
    }

    protected void onPreExecute() {
        // Toast notification de début de rafraichissement
        Toast.makeText(mContext, mContext.getString(R.string.sending), Toast.LENGTH_SHORT).show();
    }
    
    
    /**
     * Lance la récupération et le parse de la liste XML des messages non lus
     * 
     * Ce travail s'exécute en tâche de fond, et n'a donc pas le droit d'effectuer d'actions sur la GUI
     */
    protected Void doInBackground(String... args) {
        
        // Envoi du pm, et récupération des éventuels nouveaux contenus
        sendMsg (args[0], args[1], args[2], args[3]);
        
        return null;
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
    void sendMsg (String aIdCategory, String aIdSubject, String aIdGroup, String aText) {
        Log.d(LOG_TAG, "sendMsg (" + aIdCategory + ", " + aIdSubject + ", " + aIdGroup + ") : " + aText + " ...");
        
        try {
            
            // Utilise les préférences pour récupérer le login/mot de passe :
            LoginPassword loginPassword = new LoginPassword(mContext);
            
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
                // TODO SRO : parser la réponse comme dans RefreshTask
                //InputStream             in          = response.getEntity().getContent();
            
                Log.d(LOG_TAG, "sendMsg ok ");                        
            }
            
        } catch (LoginPasswordException e) {
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
    }
    
    
    /**
     * Fin de refresh
     *
     * Cette méthode est synchronisée donc on met à jour l'affichage de la liste 
     */
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
    }
}

