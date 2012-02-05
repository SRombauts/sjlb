package fr.srombauts.sjlb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;


/**
 * Activité présentant la liste des messages privés
 * @author 14/06/2010 srombauts
 */
public class ActivityPrivateMessages extends Activity implements OnTouchListener {
    private static final String LOG_TAG = "ActivityPM";
    
    static final private int    DIALOG_ID_PM_DELETE_ONE     = 1;
    static final private int    DIALOG_ID_PM_DELETE_ALL     = 2;
    
    private Cursor              mCursor                     = null;
    private SimpleCursorAdapter mAdapter                    = null;
    private ListView            mPrivateMessagesListView    = null;
    
    private long                mSelectedPmId               = 0;
   
    private float               mTouchStartPositionX    = 0;
    private float               mTouchStartPositionY    = 0;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout de l'activité
        setContentView(R.layout.pm_list);
        
        // Récupére un curseur sur les données (les messages Privés) 
        mCursor = managedQuery( SJLB.PM.CONTENT_URI,
                                null,
                                null, null, null);

        // Les colonnes à mapper :
        String[]    from = new String[] { SJLB.PM.AUTHOR, SJLB.PM.DATE, SJLB.PM.TEXT };
        
        // Les ID des views sur lesquels les mapper :
        int[]       to   = new int[]    { R.id.pmAuthor, R.id.pmDate, R.id.pmText };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SimpleCursorAdapter( this,
                                            R.layout.pm,
                                            mCursor,
                                            from,
                                            to);
        
        mPrivateMessagesListView = (ListView)findViewById(R.id.pm_listview);
        mPrivateMessagesListView.setAdapter (mAdapter);
        // Scroll tout en bas de la liste des messages
        mPrivateMessagesListView.setSelection(mPrivateMessagesListView.getCount()-1);
        
        registerForContextMenu (mPrivateMessagesListView);        
        
        // Enregister les listener d'IHM que la classe implémente        
        mPrivateMessagesListView.setOnTouchListener(this);
        mPrivateMessagesListView.getRootView().setOnTouchListener(this);
    }
    
    protected void onResume () {
        super.onResume();
        
        clearNotificationPM ();
        
        mCursor.requery();
        mAdapter.notifyDataSetChanged();
    }
    
    /// TODO SRO : trouver un moyen d'utiliser cette méthode depuis l'AsynchTaskRefresh (conserver une référence l'Activity ?)
    public void notifyDataSetChanged () {
        mCursor.requery();
        mAdapter.notifyDataSetChanged();
    }

    private void clearNotificationPM () {
        // Annule l'éventuelle notification de PM non lus
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(AsynchTaskRefresh.NOTIFICATION_NEW_PM_ID);
    }

    // TODO SRO : callback d'évènement tactiles, à mutualiser entre les activités
    public boolean onTouch(View aView, MotionEvent aMotionEvent) {
        boolean     bActionTraitee = false;
        final int   touchAction = aMotionEvent.getAction();
        final float touchX      = aMotionEvent.getX();
        final float touchY      = aMotionEvent.getY();
        
        switch (touchAction)
        {
            case MotionEvent.ACTION_DOWN: {
                //Log.d (LOG_TAG, "onTouch (ACTION_DOWN) : touch (" + touchX + ", " + touchY + ")");
                mTouchStartPositionX = touchX;
                mTouchStartPositionY = touchY;
                break;
            }
            case MotionEvent.ACTION_UP: {
                //Log.d (LOG_TAG, "onTouch (ACTION_UP) : touch (" + touchX + ", " + touchY + ")");
                final float proportionalDeltaX = (touchX - mTouchStartPositionX) / (float)aView.getWidth();
                final float proportionalDeltaY = (touchY - mTouchStartPositionY) / (float)aView.getHeight();
                //Log.d (LOG_TAG, "onTouch: deltas proportionnels : (" + proportionalDeltaX + ", " + proportionalDeltaY + ")");
                
                // Teste si le mouvement correspond à un mouvement franc
                if (   (Math.abs(proportionalDeltaX) > 0.2)                                 // mouvement d'ampleur importante
                    && (Math.abs(proportionalDeltaX)/Math.abs(proportionalDeltaY) > 0.8) )  // mouvement plus latéral que vertical
                {
                    //Log.d (LOG_TAG, "onTouch: mouvement lateral franc");
                    
                    // Teste sa direction :
                    if (proportionalDeltaX < 0) {
                        Log.i (LOG_TAG, "onTouch: mouvement vers la gauche, on quitte l'activité");
                        bActionTraitee = true;
                        finish ();
                    }
                }
                break;
            }
            default: {
                //Log.d (LOG_TAG, "onTouch autre (" + touchAction  + ") : touch (" + touchX + ", " + touchY + ")");
            }
        }

        // Si on n'a pas déjà traité l'action, on passe la main à la Vue sous-jacente
        if (false == bActionTraitee) {
            aView.onTouchEvent(aMotionEvent);
        }
        
        // Si on retourne false, on n'est plus notifié des évènements suivants
        return true;
    }
    
    /**
     * Création du menu général
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pm, menu);
        return true;
    }

    /**
     * Création du menu contextuel
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pm_context, menu);
    }
    
    /**
     * Sur sélection dans le menu
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case (R.id.menu_new_pm): {
                Intent intent = new Intent(this, ActivityNewPrivateMessage.class);
                startActivity(intent);
                break;
            }
            case (R.id.menu_update): {
                // Toast notification de début de rafraichissement
                Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
                // TODO voir si c'est la meilleurs manière de faire : donnerait plus de contrôle si l'on pouvait faire un accès direct à la AsynchTask...
                IntentReceiverStartService.startService (this, LOG_TAG);
                // TODO SRO : trouver un moyen de rafraichir la liste à l'échéance de la tache de rafraichissement
                mCursor.requery();
                mAdapter.notifyDataSetChanged();
                break;            }
            case (R.id.menu_delete_all_pm): {
                showDialog(DIALOG_ID_PM_DELETE_ALL);
                break;
            }
            case (R.id.menu_prefs): {
                Intent intent = new Intent(this, ActivityPreferences.class);
                startActivity(intent);
                break;
            }
            case (R.id.menu_quit): {
                finish ();
                break;
            }
            default:
                return false;
        }
        return true;
    }

    /**
     * Sur sélection d'un choix du menu contextuel
     */
    public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      switch (item.getItemId()) {
          case R.id.menu_pm_answer:
              answerPM(mCursor.getInt(mCursor.getColumnIndexOrThrow(SJLB.PM.AUTHOR_ID)));
              return true;
          case R.id.menu_pm_delete:
              mSelectedPmId = info.id;
              showDialog(DIALOG_ID_PM_DELETE_ONE);
              return true;
          default:
              return super.onContextItemSelected(item);
      }
    }    


    /**
     * Création de la boîte de dialogue
     */
    // TODO SRO : ne pas créer de lister directement dans le code comme ça !
    public Dialog onCreateDialog (int id) {
        switch(id) {
            case(DIALOG_ID_PM_DELETE_ONE): {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.delete_one_pm_confirmation))
                       .setCancelable(false)
                       .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               deletePM(mSelectedPmId);
                           }
                       })
                       .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
							   dialog.cancel();
                           }
                       });
                AlertDialog alert = builder.create();
                return alert;
            }
            case(DIALOG_ID_PM_DELETE_ALL): {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.delete_all_pm_confirmation))
                       .setCancelable(false)
                       .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               deleteAllPM();
                           }
                       })
                       .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
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
            case(DIALOG_ID_PM_DELETE_ONE):
                // TODO SRO : modifier le contenu de la boîte de dialogue pour montrer le texte du pm
                break;
            case(DIALOG_ID_PM_DELETE_ALL):
                // Rien à faire
                break;
        }        
    }
    
    /**
     * Effacement d'un message privé
    */
    void deletePM (long aIdPm) {
        Log.d (LOG_TAG, "deletePM (" + aIdPm + ")" );
        AsynchTaskDeletePM TaskDeletePM = new AsynchTaskDeletePM(this);
        TaskDeletePM.execute(Long.toString(aIdPm));
    }

    /**
     * Effacement d'un message privé
    */
    void deleteAllPM () {
        Log.d (LOG_TAG, "deletePM (all)" );
        AsynchTaskDeletePM TaskDeletePM = new AsynchTaskDeletePM(this);
        TaskDeletePM.execute("all");
    }
    
    void answerPM (int aSelectedPmAuthorId) {
        Log.d (LOG_TAG, "answerPM (" + aSelectedPmAuthorId + ")" );        
        // Lance l'activité correspondante avec en paramètre l'id du destinataire :
        Intent intent = new Intent(this, ActivityNewPrivateMessage.class);
        intent.putExtra(ActivityNewPrivateMessage.START_INTENT_EXTRA_AUTHOR_ID, aSelectedPmAuthorId);
        startActivity(intent);        
    }
    

    /**
     * Rédaction d'un nouveau PM sur clic sur le bouton approprié 
     */
    public void onNewPM (View v) {
        Intent intent = new Intent(this, ActivityNewPrivateMessage.class);
        startActivity(intent);
    }    
}