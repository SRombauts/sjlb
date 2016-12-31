package fr.srombauts.sjlb.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;


// http://www.sjlb.fr/FichiersAttaches/Canape.jpg

/**
 * Travail en tâche de fond, chargée d'envoyer un nouveau message du forum
 * 
 * Paramètres :
 * - l'URL absolue du fichier à télécharger
 * - la position du fichier dans la liste
 */
public class AsynchTaskDownloadImage extends AsyncTask<String, Void, Bitmap> {
    private static final String     LOG_TAG        = "DownloadImageTask";
    
    private String                  mUrl;
    private int                     mPosition;

    private CallbackImageDownload   mCallbackDownload = null;
    
    /**
     * Constructeur utilisé pour mémorisée la référence sur l'objet appelant
     * @param context Objet lançant le transfert, devant implémenter l'interface CallbackImageDownload
     */
    public AsynchTaskDownloadImage(CallbackImageDownload aCallbackDownload) {
        mCallbackDownload   = aCallbackDownload;
    }
    
    /**
     * @brief Annule le transfert en court et reset la callback
     * 
     * Doit être appelé dans le thread de l'UI
     */
    public void cancel() {
        if (AsyncTask.Status.FINISHED != getStatus()) {
            Log.i(LOG_TAG, "annulation " + mUrl + "' (" + mPosition + ")");
            super.cancel(true); // force la fin de toute opération en cours
        }
        mCallbackDownload   = null;
        mUrl                = null;
    }

    protected void onPreExecute() {      
        //Log.v(LOG_TAG, "onPreExecute");
    }
    
    /**
     * Lance la récupération et le parse de la liste XML des messages non lus
     * 
     * Ce travail s'exécute en tâche de fond, et n'a donc pas le droit d'effectuer d'actions sur la GUI
     */
    protected Bitmap doInBackground(String... args) {
        Bitmap bitmap = null;
        
        mUrl        = args[0];
        mPosition   = Integer.parseInt(args[1]);
        
        Log.i(LOG_TAG, "lancement du téléchargement de '" + mUrl + "' (" + mPosition + ")");

        {
            try
            {
                HttpURLConnection conn = (HttpURLConnection)(new URL(mUrl)).openConnection();
                conn.connect();
                Log.d(LOG_TAG, "conn=" + conn);
                InputStream is = conn.getInputStream();
                Log.d(LOG_TAG, "is=" + is);
                bitmap = BitmapFactory.decodeStream(new FlushedInputStream(is));
                Log.d(LOG_TAG, "bitmap=" + bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }        
        
        return bitmap;
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
     * Fin d'envoi
     *
     * Cette méthode est synchronisée donc on peut y faire des notifications
     */
    protected void onPostExecute(Bitmap aBitmap) {
        super.onPostExecute(aBitmap);

        if (null != aBitmap) {
            Log.i(LOG_TAG, "onPostExecute(" + mUrl + ", " + mPosition + ") OK");
        }
        else {
            Log.e(LOG_TAG, "onPostExecute(" + mUrl + ", " + mPosition + ") KO");
        }
        
        // Notifie l'activité appelante à l'aide d'une callback
        if (null != mCallbackDownload) {
            mCallbackDownload.onImageDownloaded(aBitmap, mPosition);
        }
    }
}


class FlushedInputStream extends FilterInputStream {
    public FlushedInputStream(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    public long skip(long n) throws IOException {
        long totalBytesSkipped = 0L;
        while (totalBytesSkipped < n) {
            long bytesSkipped = in.skip(n - totalBytesSkipped);
            if (bytesSkipped == 0L) {
                  int b = read();
                  if (b < 0) {
                      break;  // we reached EOF
                  } else {
                      bytesSkipped = 1; // we read one byte
                  }
           }
            totalBytesSkipped += bytesSkipped;
        }
        return totalBytesSkipped;
    }
}

