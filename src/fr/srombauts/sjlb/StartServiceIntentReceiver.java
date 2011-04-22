package fr.srombauts.sjlb;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver chargé de lancer automatiquement le service de notification des nouveaux messages SJLB
 * @author seb
 */
public class StartServiceIntentReceiver extends BroadcastReceiver {
    public  static final String ACTION_REFRESH_EARTHQUAKE_ALARM = "fr.srombauts.sjlb.ACTION_REFRESH_ALARM";

    private static final String LOG_TAG = "StartServiceIntentReceiver";

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(LOG_TAG, action);
        if (action == null ) {
            Log.e(LOG_TAG,"Action==null!");
        }
        else if (   ("android.intent.action.BOOT_COMPLETED".equals(action))
                  || ("fr.srombauts.sjlb.ACTION_REFRESH_ALARM".equals(action)) )
        {
            Intent intentService = new Intent();
            intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.SJLBService");
            ComponentName cname = context.startService(intentService);
            if (cname == null) {
                Log.e(LOG_TAG,"SJLBService was not started");
            }
            else {
                Log.d(LOG_TAG,"SJLBService started");
            }
        }
    }
}