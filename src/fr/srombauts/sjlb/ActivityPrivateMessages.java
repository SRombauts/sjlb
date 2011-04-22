package fr.srombauts.sjlb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemLongClickListener;


/**
 * Activité présentant la liste des messages privés
 * @author 14/06/2010 srombauts
 */
public class ActivityPrivateMessages extends Activity {
    static final private int    DIALOG_ID_PM_DELETE         = 1;
    
    private Cursor              mCursor                     = null;
    private SimpleCursorAdapter mAdapter                    = null;
    private ListView            mPrivateMessagesListView    = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pm_list);
        
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
        
        mPrivateMessagesListView.setOnItemLongClickListener(
            new OnItemLongClickListener(){
                public boolean onItemLongClick(AdapterView<?> adapter, View view, int index, long arg3) {
                    mCursor.moveToPosition(index);
                    showDialog(DIALOG_ID_PM_DELETE);
                    return true;
                }
            }
        );
        
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
        notificationManager.cancel(AsynchTaskRefresh.NOTIFICATION_NEW_PM_ID);
    }

    /**
     * Création de la boîte de dialogue
     */
    public Dialog onCreateDialog (int id) {
        switch(id) {
            case(DIALOG_ID_PM_DELETE): {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                // TODO internationaliser :
                builder.setMessage("Are you sure you want to delete this PM ?")
                       .setCancelable(false)
                       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                ;
                           }
                       })
                       .setNegativeButton("No", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                           }
                       });
                AlertDialog alert = builder.create();
                return alert;
            }
        }
        return null;
    }
    

    /**
     * Apparition de la boîte de dialogue
     */
    public void onPrepareDialog (int id, Dialog dialog) {
        switch(id) {
            case(DIALOG_ID_PM_DELETE): {
                // TODO SRO : modifier le contenu de la boîte de dialogue pour montrer le texte du pm
                break;
            }
        }        
    }
}