package fr.srombauts.sjlb;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SJLBPrivateMessages extends Activity {
    private static final String LOG_TAG = "SJLBPrivateMessages";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // TODO SRO : Lance le service ssi pas déjà lancé
        //startService ();
        
        // quitte immédiatement
        //finish ();
    }
    
    private void startService () {
        Intent        intentService = new Intent();
	    intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.SJLBService");
	    ComponentName cname = startService(intentService);
	    if (cname == null)
            Log.e(LOG_TAG, "SJLBService was not started");
	    else
	        Log.d(LOG_TAG, "SJLBService started");
    }
}