package fr.srombauts.sjlb;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


/**
 * Activité du menu principal, qui lance le service si besoin et permet de naviguer entre PM et Msg
 * @author 27/06/2010 srombauts
 */
public class SJLBMain extends Activity {
    private static final String LOG_TAG = "SJLBMain";
    
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Lance le service, si pas déjà lancé, et provoque un rafraichissement
        startService ();
        
        finish ();
    }

    /**
     * Lance le service de rafraichissemment, si pas déjà lancé
     */
    private void startService () {
        Intent  intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.RefreshService");
        ComponentName cname = startService(intentService);
        if (cname == null)
            Log.e(LOG_TAG, "SJLBService was not started");
        else
            Log.d(LOG_TAG, "SJLBService started");
    }
}
