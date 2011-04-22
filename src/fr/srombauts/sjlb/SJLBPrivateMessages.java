package fr.srombauts.sjlb;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


/**
 * Activité présentant la liste des messages privés
 * @author 14/06/2010 srombauts
 */
public class SJLBPrivateMessages extends Activity {
    private Cursor              mCursor                     = null;
    private SimpleCursorAdapter mAdapter                    = null;
    private ListView            mPrivateMessagesListView    = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Récupére un curseur sur les données (les messages Privés) 
        mCursor = managedQuery( SJLB.PM.CONTENT_URI,
    							 null,
    							 null, null, null);

        // Les colonnes à mapper :
        String[]    from = new String[] { SJLB.PM.AUTHOR, SJLB.PM.DATE, SJLB.PM.TEXT };
        
        // Les ID des views sur lesqules les mapper :
        int[]       to   = new int[]    { R.id.pmAuthor, R.id.pmDate, R.id.pmText };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SimpleCursorAdapter(	this,
        									R.layout.pm,
        									mCursor,
        									from,
        									to);
        
        mPrivateMessagesListView = (ListView)findViewById(R.id.privateMessagesListView);
        mPrivateMessagesListView.setAdapter (mAdapter);
        
    }
    
    protected void onResume () {
        super.onResume();
        
        clearNotificationPM ();
        
        mCursor.requery();
        mAdapter.notifyDataSetChanged();
    }

    private void clearNotificationPM () {
        // Annule l'éventuelle notification de PM non lus
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(RefreshTask.NOTIFICATION_NEW_PM_ID);
    }


}