package fr.srombauts.sjlb;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
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
        
        mIntentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.SJLBService");

        // Lance le service ssi pas déjà lancé
        startService ();

        /**
        // Récupére un curseur sur les données (les messages Privés) 
        Cursor cursor = getContentResolver().query(SJLB.PM.CONTENT_URI, new String[] {SJLB.PM.ID, SJLB.PM.AUTHOR, SJLB.PM.DATE, SJLB.PM.TEXT}, null, null, null);
        startManagingCursor(cursor);

        // THE DESIRED COLUMNS TO BE BOUND
        String[] columns = new String[] { SJLB.PM.ID, SJLB.PM.AUTHOR, SJLB.PM.DATE, SJLB.PM.TEXT };
        
        // THE XML DEFINED VIEWS WHICH THE DATA WILL BE BOUND TO
        //int[] to = new int[] { R.id.name_entry, R.id.number_entry };

        // TODO CREATE THE ADAPTER USING THE CURSOR POINTING TO THE DESIRED DATA AS WELL AS THE LAYOUT INFORMATION
        mAdapter = new SimpleCursorAdapter(this, R.id.privateMessagesListView, cursor, columns, null);
        // TODO SRO setListAdapter(mAdapter);
        
        mPrivateMessagesListView = (ListView)findViewById(R.id.privateMessagesListView);
        // TODO SRO : à tester
        mPrivateMessagesListView.setAdapter (mAdapter);
        mAdapter.notifyDataSetChanged ();
        */        
        
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

}