package fr.srombauts.sjlb;

import java.util.Date;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 srombauts
 */
public class ActivityForumMessages extends ActivityTouchListener implements OnItemClickListener, OnItemLongClickListener {
    private static final String LOG_TAG = "ActivityMsg";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_SUBJ_ID      = "SubjectId";
    public  static final String START_INTENT_EXTRA_SUBJ_LABEL   = "SubjectLabel";
    public  static final String START_INTENT_EXTRA_GROUP_ID     = "GroupId";
    
    private Cursor                  mCursor         = null;
    private MessageListItemAdapter  mAdapter        = null;
    private ListView                mMsgListView    = null;
    private EditText                mEditText       = null;
    private Button                  mEditButton     = null;
    
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
        
        // Scroll en bas de la liste des messages, sur le message précédant le premier message non lu
        Cursor subCursor = managedQuery(
                SJLB.Msg.CONTENT_URI,
                null,
                "(" +       SJLB.Msg.SUBJECT_ID + "=" + mSelectedSubjectId
                + " AND " + SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_TRUE + ")",
                null,
                null);
        // Calcul de l'offset d'affichage correspondant au nombre de messages non lus moins un, mais plafonné à zéro !
        int offsetNewMessages = Math.max(subCursor.getCount() - 1, 0);
        // Calcul de l'index du premier message à afficher dans la liste, qui doit être du coup le premier non lu le cas échéant, là aussi plaffoné par 0
        int idxOfFirstUnreadMsg = Math.max(mMsgListView.getCount()-1 - offsetNewMessages, 0);
        Log.d(LOG_TAG, "idxOfFirstUnreadMsg = " + idxOfFirstUnreadMsg);
        mMsgListView.setSelection(idxOfFirstUnreadMsg);    

        // Binding de la zone de saisie du message (masquée par la liste tant qu'on ne clic pas)
        mEditText   = (EditText)findViewById(R.id.textEditText);
        mEditButton = (Button)  findViewById(R.id.textSendButton);
        
        // Enregistre les listener d'IHM que la classe implémente
        mMsgListView.setOnItemClickListener(this);
        mMsgListView.setOnItemLongClickListener(this);
        mMsgListView.setOnTouchListener(this);
        mMsgListView.getRootView().setOnTouchListener(this);
    }
    
    @Override
    protected void onResume () {
        super.onResume();
        
        clearNotificationMsg ();
        
        // TODO SRO : tentative de refresh des données affichées (nb de new msg)
        //mCursor.requery();    // => inutile car on utilise managedQuery !
        //mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy () {
        super.onDestroy ();
        
        // Efface les flags "UNREAD_TRUE" des messages lus lorsqu'on quitte !
        ContentValues values = new ContentValues();
        values.put(SJLB.Msg.UNREAD, SJLB.Msg.UNREAD_LOCALY); // on les passe à UNREAD_LOCALY ce qui indique qu'il faut encore signaler le site Web SJLB du fait qu'on les a lu !
        String where = "(" + SJLB.Msg.SUBJECT_ID + "=" + mSelectedSubjectId + " AND " + SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_TRUE + ")";
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
     * Sur sélection dans le menu général
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case (R.id.menu_show_online): {
				// lien vers le Forum sur le Site Web :
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + mSelectedSubjectId + getString(R.string.sjlb_forum_subj_dernier)));
                Log.d (LOG_TAG, "onOptionsItemSelected: menu_show_online: " + getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + mSelectedSubjectId + getString(R.string.sjlb_forum_subj_dernier));                
				startActivity(intent);
                break;
            }
            case (R.id.menu_new_msg): {
                openEditText ();
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
       
    /**
     *  Sur clic sur un message, fait apparaitre la boîte de réponse
     */
    @SuppressWarnings("unchecked")
    public void onItemClick(AdapterView adapter, View view, int index, long arg3) {
        openEditText ();
    }
    
    /**
     *  Sur long clic sur un message, envoie sur le site Web à la page du sujet contenant ce message
     */
    @SuppressWarnings("unchecked")
    public boolean onItemLongClick(AdapterView adapter, View view, int index, long id) {
        // lien vers le Forum sur le Site Web :
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + mSelectedSubjectId + getString(R.string.sjlb_forum_subj_dernier)));
        Log.d (LOG_TAG, "onItemLongClick: show_online: " + getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + mSelectedSubjectId + getString(R.string.sjlb_forum_subj_dernier));                
        startActivity(intent);
        return true;
    }
    
    /**
     *  Fait apparaitre la boîte de réponse directement en bas de la liste de messages
     */
    public void openEditText () {
        // Pour ça, on va réduire la taille occupé par la liste de message
        ViewGroup.LayoutParams params = mMsgListView.getLayoutParams();
        params.height = 420; // pixels
        mMsgListView.requestLayout();
        // On fait apparaître la zone d'édition
        mMsgListView.setVisibility(View.VISIBLE);
        mEditText.setVisibility(View.VISIBLE);
        mEditButton.setVisibility(View.VISIBLE);

        // donne le focus à la zone d'édition
        mEditText.requestFocus();
        // et lève le clavier virtuel
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);

        // TODO SRO : Scroll tout en bas de la liste des messages ne marche pas :(
        mMsgListView.setSelection(0);         
        mMsgListView.setSelection(mMsgListView.getCount()-1);        
    }
    
    /**
     * Envoie au server le nouveau Msg 
     */
    public void onSendMsg (View v) {
        Log.d (LOG_TAG, "onSendMsg ("+ mSelectedCategoryId +", " + mSelectedSubjectId + ", [" + mSelectedGroupId + "]) : " + mEditText.getText().toString());
        AsynchTaskNewMsg TaskSendMsg = new AsynchTaskNewMsg(this);
        // Envoie le message en le passant en paramètres
        TaskSendMsg.execute(Long.toString(mSelectedCategoryId),
                            Long.toString(mSelectedSubjectId),
                            Long.toString(mSelectedGroupId),
                            mEditText.getText().toString());
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
        // TODO SRO : tenter d'optimiser à mort cette méthode qui consomme beaucoup,
        //            par exemple en remplaçant les getColumnIndexOrThrow par des constantes
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
            // Fixe l'icone de nouveau message uniquement si le message est nouveau 
            boolean bIsNew = (SJLB.Msg.UNREAD_TRUE == cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Msg.UNREAD)));
            cache.imageViewNew.setVisibility(bIsNew?ImageView.VISIBLE:ImageView.INVISIBLE); 

            // Récupère le contact éventuellement associé à l'utilisateur (Uri et photo)
            ApplicationSJLB appSJLB = (ApplicationSJLB)getApplication ();
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Msg.AUTHOR_ID));
            UserContactDescr user = appSJLB.mUserContactList.get(userId);
            // Fixe la barre de QuickContact
            cache.quickContactView.assignContactUri(user.lookupUri);
            
            // Affiche la photo du contact si elle existe (sinon petite icone de robot par défaut)
            if (null != user.photo) {
                cache.quickContactView.setImageBitmap(user.photo);
            } else {
                cache.quickContactView.setImageResource(R.drawable.ic_contact_picture);
            }

            
            // Affiche la liste les éventuels fichiers attachés, pour l'ID du message concerné
            // cursor.getColumnIndexOrThrow(SJLB.Msg._ID) retourne 6 !
            int msgId = cursor.getInt(0); // SJLB.Msg._ID, à ne pas confondre avec SJLB.Msg
            
            // Récupére un curseur sur les données (les fichiers) en filtrant sur l'id du sujet sélectionné
            Cursor cursorFiles = managedQuery(  SJLB.File.CONTENT_URI, null,
                                                SJLB.File.MSG_ID + "=" + msgId,
                                                null, null);

            // Constitue le tableau de fichiers
            FileListItem [] arrayFileListItem = new FileListItem [cursorFiles.getCount()];
            int count = cursorFiles.getCount();
            //Log.d(LOG_TAG, "msgId " + msgId + " cursorFiles.getCount()=" + cursorFiles.getCount());
            for (int i=0; i<count ;i++)
            {
                // Récupère le nom du fichier
                boolean bMoved = cursorFiles.moveToPosition(i);
                if (bMoved) {
                    FileListItem file = new FileListItem();
                    file.filename = cursorFiles.getString(cursorFiles.getColumnIndexOrThrow(SJLB.File.FILENAME));
                    arrayFileListItem[i] = file;
                }
            }

            // Créer l'adapteur entre la liste de fichiers et le layout et les informations sur le mapping des colonnes
            FileListItemAdapter adapterFiles = new FileListItemAdapter( context,
                                                                        R.layout.file,
                                                                        arrayFileListItem);

            cache.fileListView.setAdapter (adapterFiles);
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
            cache.imageViewNew      = (ImageView)           view.findViewById(R.id.msgNew);
            cache.fileListView      = (ListView)            view.findViewById(R.id.msgFileListview);
            // enregistre cet objet de cache
            view.setTag(cache);

            return view;
        }
    }
    
    // TODO SRO : http://stackoverflow.com/questions/459729/how-to-display-list-of-images-in-listview-in-android

    // Objet utilisé comme cache des données d'une View, dans un pool d'objets utilisés par la ListView
    final static class MessageListItemCache {
        public QuickContactBadge    quickContactView;
        public TextView             titleView;
        public TextView             textView;
        public ImageView            imageViewNew;
        public ListView             fileListView;
    }

    
    
    // TODO SRO : documentation !
    private class FileListItemAdapter extends ArrayAdapter<FileListItem> implements OnClickListener {

        private FileListItem[] mListeItem;

        public FileListItemAdapter(Context context, int textViewResourceId, FileListItem[] items) {
                super(context, textViewResourceId, items);
                mListeItem = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = vi.inflate(R.layout.file, null);
                }

                FileListItem it = mListeItem[position];
                if (it != null) {
                        it.imageViewFile = (ImageView) view.findViewById(R.id.fileImage);
                        it.imageViewFile.setOnClickListener(this);
                        it.imageViewFile.setTag(it.filename);
                        if (   (it.imageViewFile != null)
                            && (it.imageBitmap != null) ) {
                            it.imageViewFile.setImageBitmap(it.imageBitmap);
                        }
                }

                return view;
        }

        @Override
        public void onClick(View view) {
            // lien vers le fichier sur le Site Web :
            final String  filename = (String) view.getTag();
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_fichiers_attaches) + filename));
            Log.d (LOG_TAG, "onClick: " + getString(R.string.sjlb_fichiers_attaches) + filename );                
            startActivity(intent);
        }
    }    
        
    // Objet représentant une image et le nom du fichier associé
    final static class FileListItem {
        public ImageView    imageViewFile;
        public Bitmap       imageBitmap;
        public String       filename;
    }
}