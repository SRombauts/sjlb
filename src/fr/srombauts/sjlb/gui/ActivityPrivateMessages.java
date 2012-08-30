package fr.srombauts.sjlb.gui;

import java.util.Date;

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
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import fr.srombauts.sjlb.ApplicationSJLB;
import fr.srombauts.sjlb.BuildConfig;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.model.ForumMessage;
import fr.srombauts.sjlb.model.UserContactDescr;
import fr.srombauts.sjlb.service.API;
import fr.srombauts.sjlb.service.OnServiceResponseListener;
import fr.srombauts.sjlb.service.ResponseReceiver;
import fr.srombauts.sjlb.service.ServiceSJLB;
import fr.srombauts.sjlb.service.StartService;


/**
 * Activité présentant la liste des messages privés reçus
 * @author 14/06/2010 SRombauts
 */
public class ActivityPrivateMessages extends ActivityTouchListener implements OnServiceResponseListener {
    private static final String LOG_TAG = "ActivityPM";
    
    static final private int    DIALOG_ID_PM_DELETE_ONE     = 1;
    static final private int    DIALOG_ID_PM_DELETE_ALL     = 2;
    
    private Cursor              mCursor                     = null;
    private PmListItemAdapter   mAdapter                    = null;
    private ListView            mPrivateMessagesListView    = null;
    
    private long                mSelectedPmId               = 0;
    
    private ResponseReceiver    mResponseReceiver           = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout de l'activité, et titre
        setContentView(R.layout.pm_list);
        setTitle(getString(R.string.pm_description));
        
        // Récupère un curseur sur les données (les messages Privés reçus) en filtrant sur l'Id de l'utilisateur de l'appli SJLB 
        mCursor = managedQuery( SJLB.PM.CONTENT_URI, null,
                                SJLB.PM.DEST_ID + "=" + ((ApplicationSJLB)getApplication ()).getUserId(),
                                null, null);

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new PmListItemAdapter( this,
                                          R.layout.pm_item,
                                          mCursor);
        
        mPrivateMessagesListView = (ListView)findViewById(R.id.pm_listview);
        mPrivateMessagesListView.setAdapter (mAdapter);
        // Scroll tout en bas de la liste des messages
        mPrivateMessagesListView.setSelection(mCursor.getCount()-1);
        
        // Enregistre le menu contextuel de la liste
        registerForContextMenu (mPrivateMessagesListView);        
        
        // Enregistre les listener d'IHM que la classe implémente        
        mPrivateMessagesListView.setOnTouchListener(this);
        mPrivateMessagesListView.getRootView().setOnTouchListener(this);
    }
    
    // Appelée lorsque l'activité était déjà lancée (par exemple clic sur une notification de nouveau PM)
    protected void onNewIntent (Intent intent) {
        // Rafraîchit la liste des messages privés 
        mCursor.requery();
    }
    
    protected void onResume () {
        super.onResume();
        
        clearNotificationPM ();
        
        // Demande à être notifié des résultats des actions réalisées par le service
        mResponseReceiver = new ResponseReceiver(this);
    }
    
    // Appelée lorsque l'activité passe de "au premier plan" à "en pause/cachée" 
    protected void onPause() {
        super.onPause();
        
        // Plus de notification de résultat du service, vu qu'on se met en pause !
        mResponseReceiver.unregister(this);
        mResponseReceiver = null;
    }

    private void clearNotificationPM () {
        // Annule l'éventuelle notification de PM non lus
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(API.NOTIFICATION_NEW_PM_ID);
    }

    /**
     * Sur réception d'une réponse du service SJLB
     * 
     * @param aIntent Informations sur le type d'action traitée et le résultat obtenu
     */
    @Override
    public void onServiceResponse(Intent intent) {
      //String  responseType    = intent.getStringExtra(ServiceSJLB.RESPONSE_INTENT_EXTRA_TYPE);
        boolean reponseResult   = intent.getBooleanExtra(ServiceSJLB.RESPONSE_INTENT_EXTRA_RESULT, false);
        if (reponseResult) {
            if (false != BuildConfig.DEBUG) {
                // En mise au point uniquement : Toast notification signalant la réponse
                Toast.makeText(this, "refresh", Toast.LENGTH_SHORT).show();
            }
            // Rafraîchit la liste des messages privés (il y a peut être de nouveaux messages privés)
            mCursor.requery();
        }
    }
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activite pour retour à la liste des sujets");
        finish ();
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
     * Sur sélection dans le menu général
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
                // Demande de rafraîchissement asynchrone des informations
                StartService.refresh(this);
                Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
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
     * Création du menu contextuel
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pm_context, menu);
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
    // TODO SRombauts : ne pas créer de boîte de dialogue directement dans le code comme ça !
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
                // TODO SRombauts : compléter le contenu de la boîte de dialogue pour montrer le texte du pm
                break;
            case(DIALOG_ID_PM_DELETE_ALL):
                // Rien à faire
                break;
        }        
    }

    /**
     * Sur click du bouton correspondant, lance l'activité d'affichage des PM
     */
    public void onShowPMSent (View v) {
        // Lance l'activité lisant les PM envoyés
        Intent intent = new Intent(this, ActivityPrivateMessagesSent.class);
        startActivity(intent);
    }
    
    /**
     * Effacement d'un message privé
    */
    void deletePM (long aIdPm) {
        Log.d (LOG_TAG, "deletePM (" + aIdPm + ")" );
        // Met dans la fifo du service l'id du pm à effacer   
        StartService.delPM(this, aIdPm);
    }

    /**
     * Effacement de tous les messages privés
    */
    void deleteAllPM () {
        Log.d (LOG_TAG, "deletePM (all)" );
        // Met dans la fifo du service la demande d'effacement de tous les pm   
        StartService.delAllPM(this);
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
    

    // Adaptateur mappant les données du curseur dans des objets du cache du pool d'objets View utilisés par la ListView
    private final class PmListItemAdapter extends ResourceCursorAdapter {
        public PmListItemAdapter(Context context, int layout, Cursor cursor) {
            super(context, layout, cursor);
        }

        // Met à jour avec un nouveau contenu un objet de cache du pool de view utilisées par la ListView 
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final MessageListItemCache  cache = (MessageListItemCache) view.getTag();
            
            // Récupère le pseudo et le contact (Uri et photo) éventuellement associé à l'utilisateur
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.PM.AUTHOR_ID));
            UserContactDescr user = ((ApplicationSJLB)getApplication ()).getUserContactById(userId);
            
            // Fixe les infos du message 
            String  pseudo = user.getPseudo(); // on utilise le pseudo fourni par la liste d'utilisateur, plus simple qu'un croisement en bdd
            cache.pseudoView.setText(pseudo);
            String  strDate = ForumMessage.getDateString (new Date(cursor.getLong(cursor.getColumnIndexOrThrow(SJLB.PM.DATE)))) ;
            cache.dateView.setText(strDate);
            // Fixe le contenu du message 
            String  text = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.PM.TEXT));
            cache.textView.setText(text);

            // Fixe la barre de QuickContact
            cache.quickContactView.assignContactUri(user.getLookupUri());
            
            // Affiche la photo du contact si elle existe (sinon petite icône de robot par défaut)
            if (null != user.getPhoto()) {
                cache.quickContactView.setImageBitmap(user.getPhoto());
            } else {
                cache.quickContactView.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        // Création d'une nouvelle View et de son objet de cache (vide) pour le pool
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            
            // Construction d'un nouvel objet de cache
            MessageListItemCache cache = new MessageListItemCache();
            // et binding sur les View décrites par le Layout
            cache.quickContactView  = (QuickContactBadge)   view.findViewById(R.id.pmBadge);
            cache.pseudoView        = (TextView)            view.findViewById(R.id.pmPseudo);
            cache.dateView          = (TextView)            view.findViewById(R.id.pmDate);
            cache.textView          = (TextView)            view.findViewById(R.id.pmText);
            // enregistre cet objet de cache
            view.setTag(cache);

            return view;
        }
    }

    // Objet utilisé comme cache des données d'une View, dans un pool d'objets utilisés par la ListView
    final static class MessageListItemCache {
        public QuickContactBadge    quickContactView;
        public TextView             pseudoView;
        public TextView             dateView;
        public TextView             textView;
    }    
}