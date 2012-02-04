package fr.srombauts.sjlb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;


/**
 * Activité présentant la liste des messages privés
 * @author 14/06/2010 srombauts
 */
public class ActivityPrivateMessages extends Activity {
    private static final String LOG_TAG = "ActivityPM";

    static final private int MENU_ID_UPDATE     = Menu.FIRST;
    static final private int MENU_ID_RESET      = Menu.FIRST + 1;
    static final private int MENU_ID_PREFS      = Menu.FIRST + 2;
    static final private int MENU_ID_QUIT       = Menu.FIRST + 3;
    
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
     * Création du menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ID_UPDATE,   Menu.NONE, R.string.menu_update);
        menu.add(0, MENU_ID_RESET,    Menu.NONE, R.string.menu_reset);
        menu.add(0, MENU_ID_PREFS,    Menu.NONE, R.string.menu_prefs);
        menu.add(0, MENU_ID_QUIT,     Menu.NONE, R.string.menu_quit);
        
        return true;
    }
    
    /**
     * Sur sélection dans le menu
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case (MENU_ID_UPDATE): {
                // TODO SRO : utiliser une méthode unique au lieu de dupliquer le code ainsi
                // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
                SharedPreferences   prefs       = PreferenceManager.getDefaultSharedPreferences(this);
                String              login       = prefs.getString(SJLB.PREFS.LOGIN,    "");
                String              password    = prefs.getString(SJLB.PREFS.PASSWORD, "");

                if (   (false == login.contentEquals(""))
                    && (false == password.contentEquals("")) )
                {
                    // Toast notification de début de rafraichissement
                    Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
                    // TODO voir si c'est la meilleurs manière de faire...
                    IntentReceiverStartService.startService (this, LOG_TAG);
                }
                else
                {
                    // Toast notification signalant l'absence de login/password
                    Toast.makeText(this, getString(R.string.refresh_impossible), Toast.LENGTH_SHORT).show();
                }
                break;            }
            case (MENU_ID_RESET): {
                ContentProviderPM   pms = new ContentProviderPM (this);
                pms.clearPM();
                ContentProviderMsg  msgs = new ContentProviderMsg (this);
                msgs.clearMsg();
                break;
            }
            case (MENU_ID_PREFS): {
                Intent intent = new Intent(this, ActivityPreferences.class);
                startActivity(intent);
                break;
            }
            case (MENU_ID_QUIT): {
                finish ();
                break;
            }
            default:
                return false;
        }
        return true;
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