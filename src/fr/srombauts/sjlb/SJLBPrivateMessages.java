package fr.srombauts.sjlb;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

public class SJLBPrivateMessages extends Activity {
    private static final String LOG_TAG = "SJLBPrivateMessages";
    
    private ServiceConnection   mServiceConnection;
    private Intent              mIntentService = new Intent();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mIntentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.SJLBService");

        // Lance le service ssi pas déjà lancé
        startService ();
        
        // Se connecte au service 
        bindService ();
        
///////////////////////////////////////////////////////////////////////////////////////////////////////////
// TODO SRO : à la place de BINDER le service, utiliser un ContentProvider fait pour retourner un Cursor !!
///////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        /** TODO Récupérer un curseur sur les données (les messages Privés) **/ 
        Cursor cursor;// TODO SRO : = getContentResolver().query(People.CONTENT_URI, new String[] {People._ID, People.NAME, People.NUMBER}, null, null, null);
        startManagingCursor(cursor);

        // TODO THE DESIRED COLUMNS TO BE BOUND
        String[] columns = new String[] { /*TODO PrivateMessage.AUTHOR, PrivateMessage.TEXT*/ };
        
        // THE XML DEFINED VIEWS WHICH THE DATA WILL BE BOUND TO
        //int[] to = new int[] { R.id.name_entry, R.id.number_entry };

        
        // TODO CREATE THE ADAPTER USING THE CURSOR POINTING TO THE DESIRED DATA AS WELL AS THE LAYOUT INFORMATION
        SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this, R.id.privateMessagesListView, cursor, columns, null/*to*/);
        //setListAdapter(mAdapter);
        
        
        // TODO SRO quitte immédiatement
        //finish ();
    }
    
    private void startService () {
	    ComponentName cname = startService(mIntentService);
	    if (cname == null)
            Log.e(LOG_TAG, "SJLBService was not started");
	    else
	        Log.d(LOG_TAG, "SJLBService started");
    }

    private void bindService () {
        if (bindService(mIntentService, mServiceConnection, 0))
            Log.d(LOG_TAG, "SJLBService binded");
        else
            Log.e(LOG_TAG, "SJLBService was not binded");
    }

}