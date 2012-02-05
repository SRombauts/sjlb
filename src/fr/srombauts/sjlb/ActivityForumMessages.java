package fr.srombauts.sjlb;

import java.util.Date;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 srombauts
 */
public class ActivityForumMessages extends ActivityTouchListener {
    private static final String LOG_TAG = "ActivityMsg";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_SUBJ_ID      = "SubjectId";
    public  static final String START_INTENT_EXTRA_SUBJ_LABEL   = "SubjectLabel";
    public  static final String START_INTENT_EXTRA_GROUP_ID     = "GroupId";
    
    private Cursor                  mCursor         = null;
    private MessageListItemAdapter  mAdapter        = null;
    private ListView                mMsgListView    = null;
    
    private long                    mSelectedCategoryId     = 0;
    private long                    mSelectedSubjectId      = 0;
    private String                  mSelectedSubjectLabel   = "";
    private long                    mSelectedGroupId        = 0;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout de l'activité
        setContentView(R.layout.msg_list);
        
        // Récupère l'éventuel paramètre de lancement (id de du sujet du forum sélectionnée)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            mSelectedCategoryId     = startIntent.getExtras().getLong  (START_INTENT_EXTRA_CAT_ID);
            mSelectedSubjectId      = startIntent.getExtras().getLong  (START_INTENT_EXTRA_SUBJ_ID);
            mSelectedSubjectLabel   = startIntent.getExtras().getString(START_INTENT_EXTRA_SUBJ_LABEL);
            mSelectedGroupId        = startIntent.getExtras().getLong  (START_INTENT_EXTRA_GROUP_ID);
            Log.i(LOG_TAG, "SelectedSubject (" + mSelectedSubjectId +", " + mSelectedGroupId + " [" + mSelectedCategoryId + "]) : " + mSelectedSubjectLabel);
        }
        
        // Map la description du sujet pour la renseigner
        TextView SubjectsDescription = (TextView)findViewById(R.id.subject_label);
        SubjectsDescription.setText(mSelectedSubjectLabel);
        
        // Récupére un curseur sur les données (les messages) en filtrant sur l'id du sujet sélectionné
        mCursor = managedQuery( SJLB.Msg.CONTENT_URI, null,
                                SJLB.Msg.SUBJECT_ID + "=" + mSelectedSubjectId,
                                null, null);

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new MessageListItemAdapter(  this,
                                                R.layout.msg,
                                                mCursor);
        
        mMsgListView = (ListView)findViewById(R.id.msg_listview);
        mMsgListView.setAdapter (mAdapter);
        // Scroll tout en bas de la liste des messages
        mMsgListView.setSelection(mMsgListView.getCount()-1);    
        
        // Enregister les listener d'IHM que la classe implémente
        mMsgListView.setOnTouchListener(this);
        mMsgListView.getRootView().setOnTouchListener(this);
    }
    
    @Override
    protected void onResume () {
        super.onResume();
        
        clearNotificationMsg ();
        
        mCursor.requery();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy () {
        super.onDestroy ();
        
        // Efface les flags "unread" des messages lus lorsqu'on quitte !
        ContentValues values = new ContentValues();
        values.put(SJLB.Msg.UNREAD, -1); // on les passe à "-1" ce qui indique qu'il faut encore signaler le site Web SJLB du fait qu'on les a lu !
        String where = "(" + SJLB.Msg.SUBJECT_ID + "=" + mSelectedSubjectId + " AND " + SJLB.Msg.UNREAD + "=1)";
        getContentResolver ().update(SJLB.Msg.CONTENT_URI, values, where, null);        
    }
    
    
    /**
     * Annule l'éventuelle notification de Msg non lus
     */
    private void clearNotificationMsg () {
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(AsynchTaskRefresh.NOTIFICATION_NEW_MSG_ID);
    }
    
    /**
     * Création du menu général
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.msg, menu);
        return true;
    }

    /**
     * Sur sélection dans le menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case (R.id.menu_show_online): {
				// lien vers le Forum sur le Site Web :
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + mSelectedSubjectId));
                Log.d (LOG_TAG, "onOptionsItemSelected: menu_show_online: " + getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + mSelectedSubjectId);                
				startActivity(intent);
                break;
            }
            case (R.id.menu_new_msg): {
                Intent intent = new Intent(this, ActivityNewForumMessage.class);
                intent.putExtra(ActivityNewForumMessage.START_INTENT_EXTRA_CAT_ID,        mSelectedCategoryId);
                intent.putExtra(ActivityNewForumMessage.START_INTENT_EXTRA_SUBJ_ID,       mSelectedSubjectId);
                intent.putExtra(ActivityNewForumMessage.START_INTENT_EXTRA_SUBJ_LABEL,    mSelectedSubjectLabel);
                intent.putExtra(ActivityNewForumMessage.START_INTENT_EXTRA_GROUP_ID,      mSelectedGroupId);
                Log.d (LOG_TAG, "onItemClick: intent.putExtra(" + mSelectedSubjectId + ", " + mSelectedSubjectLabel + ")");                
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
   
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activite pour retour à la liste des sujets");
        finish ();
        return true;
    }

    // TODO SRO : onRightGesture
    
    
    // Adaptateur mappant les données du curseur dans des objets du cache du pool d'objets View utilisés par la ListView
    private final class MessageListItemAdapter extends ResourceCursorAdapter {
        public MessageListItemAdapter(Context context, int layout, Cursor cursor) {
            super(context, layout, cursor);
        }

        // Met à jour avec un nouveau contenu un objet de cache du pool de view utilisées par la ListView 
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final MessageListItemCache  cache = (MessageListItemCache) view.getTag();
            
            // Fixe la barre de titre du message 
            String  title = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.User.PSEUDO)); // on utilise le champ "PSEUDO" issu du croisement avec la table 
            String  strDate = ForumMessage.getDateString (new Date(cursor.getLong(cursor.getColumnIndexOrThrow(SJLB.Msg.DATE)))) ;
            cache.titleView.setText(title + "\n" + strDate);
            // Fixe le contenu du message 
            String  text = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.Msg.TEXT));
            cache.textView.setText(text);
            // Fixe l'icone de 
            // TODO SRO : positionner le niveau de l'image "unread" !
            cache.imageView.setImageLevel(cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Msg.UNREAD))); 

            // Récupère le contact éventuellement associé à l'utilisateur (Uri et photo)
            ApplicationSJLB appSJLB = (ApplicationSJLB)getApplication ();
            UserContactDescr user = appSJLB.mUserContactList.get(cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.User._ID)));
            // Fixe la barre de QuickContact
            cache.quickContactView.assignContactUri(user.lookupUri);
            
            // Affiche la photo du contact si elle existe (sinon petite icone de robot par défaut)
            if (null != user.photo) {
                cache.quickContactView.setImageBitmap(user.photo);
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
            cache.quickContactView  = (QuickContactBadge)   view.findViewById(R.id.msgBadge);
            cache.titleView         = (TextView)            view.findViewById(R.id.msgTitre);
            cache.textView          = (TextView)            view.findViewById(R.id.msgText);
            cache.imageView         = (ImageView)           view.findViewById(R.id.msgNew);
            // enregistre cet objet de cache
            view.setTag(cache);

            return view;
        }
    }

    // Objet utilisé comme cache des données d'une View, dans un pool d'objets utilisés par la ListView
    final static class MessageListItemCache {
        public QuickContactBadge    quickContactView;
        public TextView             titleView;
        public TextView             textView;
        public ImageView            imageView;
    }
}