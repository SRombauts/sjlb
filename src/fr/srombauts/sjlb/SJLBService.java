package fr.srombauts.sjlb;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;



class SJLBServiceTask implements Runnable {
	private static final int     HELLO_ID = 1;
	private static final String  LOG_TAG  = "SJLBService";
	
	private Context  mContext          = null;
	private Handler  mServiceHandler   = null;
	private int      mCountDown  = 0;
	private long     mInterval   = 0L;
  
	public SJLBServiceTask(int countDown, long interval, Handler serviceHandler, Context context) {
		this.mCountDown       = countDown;
		this.mInterval        = interval;
		this.mServiceHandler  = serviceHandler;
		this.mContext         = context;
	}

	public void run() {
		Log.d(LOG_TAG, "Counter: " + mCountDown);
		if (--mCountDown > 0) {
		    mServiceHandler.postDelayed(this, mInterval);
		}
		notifyUser (mCountDown);
	}

	private void notifyUser (int countDown) {
		// Get a reference to the NotificationManager:
		String 				ns                     = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager   = (NotificationManager) mContext.getSystemService(ns);

		// Instantiate the Notification:
        int          icon        = android.R.drawable.stat_notify_sync;
        CharSequence tickerText  = "Hello";
        long         when        = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);

        // Define the Notification's expanded message and Intent:
        Context         context              = mContext.getApplicationContext();
        CharSequence    contentTitle         = "My notification";
        CharSequence    contentText          = "Hello World!";
        Intent          notificationIntent   = new Intent(mContext, SJLB.class);
        PendingIntent   contentIntent        = PendingIntent.getService(mContext, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        // Pass the Notification to the NotificationManager:
        mNotificationManager.notify(HELLO_ID, notification);      
  }
}


public class SJLBService extends Service {
    private static final String  LOG_TAG              = "SJLBService";
    private static final int     COUNTDOWN_LIMIT      = 100;
    private static final long    COUNTDOWN_INTERVAL   = 3*1000L;

    private Handler mServiceHandler = new Handler();

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    Log.d(LOG_TAG, "onStart");
    SJLBServiceTask task = new SJLBServiceTask(COUNTDOWN_LIMIT, COUNTDOWN_INTERVAL, mServiceHandler, this);
    mServiceHandler.postDelayed(task, COUNTDOWN_INTERVAL);
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
