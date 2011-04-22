package fr.srombauts.sjlb;

import java.util.ArrayList;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SJLBPrivateMessages extends Activity {
    private static final String LOG_TAG = "SJLBPrivateMessages";
    
    private Intent                      mIntentService              = new Intent();

    private ListView                    mPrivateMessagesListView    = null;
    private ArrayList<PrivateMessage>   mPMs                        = new ArrayList<PrivateMessage>();
    SimpleCursorAdapter                 mAdapter                    = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Annule l'éventuelle notification de PM non lus
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(RefreshTask.NOTIFICATION_NEW_PM_ID);

        // Lance le service ssi pas déjà lancé
        startService ();

        // Récupére un curseur sur les données (les messages Privés) 
        Cursor cursor = managedQuery(SJLB.PM.CONTENT_URI,
        							 null,
        							 null, null, null);

        // Les colonnes à mapper :
        String[]    from = new String[] { SJLB.PM.AUTHOR, SJLB.PM.DATE, SJLB.PM.TEXT };
        
        // Les ID des views sur lesqules les mapper :
        int[]       to   = new int[]    { R.id.pmAuthor, R.id.pmDate, R.id.pmText };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SimpleCursorAdapter(	this,
        									R.layout.pm,
        									cursor,
        									from,
        									to);
        
        mPrivateMessagesListView = (ListView)findViewById(R.id.privateMessagesListView);
        mPrivateMessagesListView.setAdapter (mAdapter);
    }
    
    private void startService () {
        mIntentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.SJLBService");
	    ComponentName cname = startService(mIntentService);
	    if (cname == null)
            Log.e(LOG_TAG, "SJLBService was not started");
	    else
	        Log.d(LOG_TAG, "SJLBService started");
    }

}