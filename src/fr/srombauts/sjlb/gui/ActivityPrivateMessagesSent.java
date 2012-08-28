package fr.srombauts.sjlb.gui;

import java.util.Date;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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


/**
 * Activité présentant la liste des messages privés envoyés
 * @author 14/06/2010 SRombauts
 */
public class ActivityPrivateMessagesSent extends ActivityTouchListener implements OnServiceResponseListener {
    private static final String LOG_TAG = "ActivityPM";
    
    private Cursor              mCursor                     = null;
    private PmListItemAdapter   mAdapter                    = null;
    private ListView            mPrivateMessagesListView    = null;
    
    private ResponseReceiver    mResponseReceiver           = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout de l'activité, et titre
        setContentView(R.layout.activity_list);
        setTitle(getString(R.string.pm_sent_description));
        
        // Récupère un curseur sur les données (les messages Privés envoyés) en filtrant sur l'Id de l'utilisateur de l'appli SJLB 
        mCursor = managedQuery( SJLB.PM.CONTENT_URI, null,
                                SJLB.PM.AUTHOR_ID + "=" + ((ApplicationSJLB)getApplication ()).getUserId(),
                                null, null);

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new PmListItemAdapter( this,
                                          R.layout.pm_item,
                                          mCursor);
        
        mPrivateMessagesListView = (ListView)findViewById(R.id.activity_listview);
        mPrivateMessagesListView.setAdapter (mAdapter);
        // Scroll tout en bas de la liste des messages
        mPrivateMessagesListView.setSelection(mCursor.getCount()-1);
        
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
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activite pour retour à la liste des PM");
        finish ();
        return true;
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
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.PM.DEST_ID));
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