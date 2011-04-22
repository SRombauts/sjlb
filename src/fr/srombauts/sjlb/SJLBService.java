package fr.srombauts.sjlb;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;



class SJLBServiceTask implements Runnable {
    private static final int     NOTIFICATION_NEW_MSG_ID    = 1;
    private static final String  LOG_TAG                    = "SJLBServiceTask";

    private static final long    REFRESH_INTERVAL_NORMAL    =  5*60*1000L;
    private static final long    REFRESH_INTERVAL_ON_ERROR  =  1*60*1000L;
    
    static final private String NODE_NAME_NB_PRIVATE_MSG    = "nb_pm";
    static final private String NODE_NAME_NB_MSG            = "nb_msg";
                        
    private Context mContext        = null;
    private Handler mServiceHandler = null;
    private int     mNbPM           = 0;
    private int     mNbPMSaved      = 0;
    private int     mNbMsg          = 0;
    private int     mNbMsgSaved     = 0;
  
    public SJLBServiceTask(Handler serviceHandler, Context context) {
        this.mServiceHandler  = serviceHandler;
        this.mContext         = context;
    }

    public void run() {
        boolean bRet;
        
        Log.d(LOG_TAG, "run");

        bRet = refreshInfos ();
        
        if (bRet) {
            // TODO SRO : tests en cours, bidouille à remplacer par une base de données stockant les ID des derniers messages non lus
            if (   (mNbPMSaved  < mNbPM)
                || (mNbMsgSaved < mNbMsg) )
            {
                notifyUser ();
    
            }
            mNbPMSaved  = mNbPM;
            mNbMsgSaved = mNbMsg;
            
            mServiceHandler.postDelayed(this, REFRESH_INTERVAL_NORMAL);
        }
        else
        {
            mServiceHandler.postDelayed(this, REFRESH_INTERVAL_ON_ERROR);
        }
    }

    private boolean refreshInfos () {
        boolean bRet = false;
        
        Log.d(LOG_TAG, "refreshInfos");
        
        try
        {
            // Etablissement de la connexion http et récupération de la page Web
            // TODO SRO : utiliser les préférences pour sauvegarder le login/mot de passe :
            URL                 urlAPI          = new URL(mContext.getString(R.string.sjlb_api_uri) + "?login=Seb&password=4df51b1810f131b7f6a794900d93d58e");
            URLConnection       connection      = urlAPI.openConnection();
            HttpURLConnection   httpConnection  = (HttpURLConnection)connection;
            
            if (HttpURLConnection.HTTP_OK == httpConnection.getResponseCode()) {
                // Parse de la page XML
                InputStream             in  = httpConnection.getInputStream();
                
                DocumentBuilderFactory  dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder         db  = dbf.newDocumentBuilder();
                Document                dom = db.parse(in);
                Element                 docElement = dom.getDocumentElement();
                
                // Récupère le nombre de PM
                Element eltNbPM = (Element)docElement.getElementsByTagName(NODE_NAME_NB_PRIVATE_MSG).item(0);
                String  strNbPM = eltNbPM.getFirstChild().getNodeValue();
                mNbPM = Integer.parseInt(strNbPM);
                
                // Récupère le nombre de Messages
                Element eltNbMsg = (Element)docElement.getElementsByTagName(NODE_NAME_NB_MSG).item(0);
                String  strNbMsg = eltNbMsg.getFirstChild().getNodeValue();
                mNbMsg = Integer.parseInt(strNbMsg);
                
                bRet = true;
            }
           
           
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } /* catch (ParseException e) {
            e.printStackTrace();
        } */
        
        return bRet;
    }
    
    /**
     *  Notifie à l'utilisateur les évolutions du nombre de messages non lus
     */
    private void notifyUser () {
        Log.d(LOG_TAG, "notifyUser");
        
        // Get a reference to the NotificationManager:
        String              ns                     = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager   = (NotificationManager) mContext.getSystemService(ns);

        // Instantiate the Notification:
        int          icon        = android.R.drawable.stat_notify_sync;
        CharSequence tickerText  = "SJLB";
        long         when        = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);

        // Define the Notification's expanded message and Intent:
        Context         context             = mContext.getApplicationContext();
        CharSequence    contentTitle        = "Unread Messages";
        CharSequence    contentText         = mNbPM + " private messages and " + mNbMsg + " messages";

        Intent          notificationIntent  = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(mContext.getString(R.string.sjlb_forum_uri)));
        PendingIntent   contentIntent       = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        notification.number     = mNbPM + mNbMsg;
        notification.defaults   = Notification.DEFAULT_ALL;
        notification.vibrate    = new long[]{200, 200};
        
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        // Pass the Notification to the NotificationManager:
        mNotificationManager.notify(NOTIFICATION_NEW_MSG_ID, notification);
    }
}


public class SJLBService extends Service {
    private static final String  LOG_TAG = "SJLBService";

    private Handler mServiceHandler = new Handler();

  @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");

        // Création et exécution de la tâche
        SJLBServiceTask task = new SJLBServiceTask(mServiceHandler, this);
        mServiceHandler.post(task);
    
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
         return null;
    }
}
