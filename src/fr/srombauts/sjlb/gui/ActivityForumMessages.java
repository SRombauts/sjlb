package fr.srombauts.sjlb.gui;

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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
 * Activité présentant la liste des messages du sujet sélectionné
 * @author 22/08/2010 SRombauts
 */
public class ActivityForumMessages extends ActivityTouchListener implements OnItemClickListener, OnItemLongClickListener, OnServiceResponseListener {
    private static final String LOG_TAG = "ActivityMsg";
    
    public static final String  START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public static final String  START_INTENT_EXTRA_SUBJ_ID      = "SubjectId";
    public static final String  START_INTENT_EXTRA_SUBJ_LABEL   = "SubjectLabel";
    public static final String  START_INTENT_EXTRA_GROUP_ID     = "GroupId";
    
    private Cursor                  mCursor         = null;
    private MessageListItemAdapter  mAdapter        = null;
    static private ListView         mMsgListView    = null; // TODO SRombauts "static" pour être accéder depuis la CallbackImageDownload

    private boolean                 mbIsSending     = false;
    private EditText                mEditText       = null;
    private Button                  mEditButton     = null;
    
    private long                    mSelectedCategoryId     = 0;
    private long                    mSelectedSubjectId      = 0;
    private String                  mSelectedSubjectLabel   = "";
    private long                    mSelectedGroupId        = 0;
    
    private int                     mOriginalMsgListHeight  = -1; 
    
    private ResponseReceiver        mResponseReceiver       = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d (LOG_TAG, "onCreate...");
        
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
        
        // Map la description du sujet pour la renseigner dans le titre
        setTitle(mSelectedSubjectLabel);
        
        // Récupère un curseur sur les données (les messages) en filtrant sur l'id du sujet sélectionné
        mCursor = managedQuery( SJLB.Msg.CONTENT_URI,
                                null, // Pas d'argument "projection" pour filtrer les colonnes de résultats car elles sont toutes utiles
                                SJLB.Msg.SUBJECT_ID + "=" + mSelectedSubjectId,
                                null, null);

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new MessageListItemAdapter(  this,
                                                R.layout.msg_item,
                                                mCursor);
        
        mMsgListView = (ListView)findViewById(R.id.msg_listview);
        mMsgListView.setAdapter (mAdapter);
        
        // Scroll en bas de la liste des messages, sur le message précédant le premier message non lu
        final String[] columns = {SJLB.Msg.SUBJECT_ID}; // ne récupère que le minimum pour compter le nombre de Msg non lus
        final String selection = "(" + SJLB.Msg.SUBJECT_ID + "=" + mSelectedSubjectId + " AND "
                                     + SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_TRUE + ")";
        Cursor countCursor = managedQuery(  SJLB.Msg.CONTENT_URI,
                                            columns, selection, null,
                                            null);
        // Calcul de l'offset d'affichage correspondant au nombre de messages non lus moins un, mais plafonné à zéro !
        int offsetNewMessages = Math.max(countCursor.getCount() - 1, 0);
        // Calcul de l'index du premier message à afficher dans la liste, qui doit être du coup le premier non lu le cas échéant, là aussi plafonné par 0
        int idxOfFirstUnreadMsg = Math.max(mMsgListView.getCount()-1 - offsetNewMessages, 0);
        Log.d(LOG_TAG, "idxOfFirstUnreadMsg = " + idxOfFirstUnreadMsg + " (nbUnreadMsg=" + countCursor.getCount() + ")");
        mMsgListView.setSelection(idxOfFirstUnreadMsg);    

        // Binding de la zone de saisie du message (masquée par la liste tant qu'on ne clic pas)
        mEditText   = (EditText)findViewById(R.id.textEditText);
        mEditButton = (Button)  findViewById(R.id.textSendButton);
        
        // Enregistre les listener d'IHM que la classe implémente
        mMsgListView.setOnItemClickListener(this);
        mMsgListView.setOnItemLongClickListener(this);
        mMsgListView.setOnTouchListener(this);
        
        // Restaure un éventuel état sauvegardé (état de la boîte d'édition) suite à un changement d'orientation  :
        if (null != savedInstanceState) {
            final boolean bEditTextOpen = savedInstanceState.getBoolean("bEditTextOpen");
            mbIsSending = savedInstanceState.getBoolean("mbIsSending");
            Log.i(LOG_TAG, "bEditTextOpen=" + bEditTextOpen + ", mbIsSending=" + mbIsSending);
            if (bEditTextOpen) {
                // Sur édition en cours, restaure la boîte d'édition
                openEditText();
            }
            if (mbIsSending) {
                // Sur envoi en cours, verrouille le bouton et le champ texte pour éviter les envois multiples
                mEditText.setEnabled(false);
                mEditButton.setEnabled(false);
            }            
        }        
        
        // Demande à être notifié des résultats des actions réalisées par le service
        // (ceci pour toute la durée de vie de l'activité car on ne peut se permettre de louper un ack sous peine d'état incohérent)
        mResponseReceiver = new ResponseReceiver(this);
    }
    
    @Override
    protected void onResume () {
        super.onResume();
        Log.d (LOG_TAG, "onResume... clear UNREAD flags");

        // Supprime l'éventuelle notification de Msg non lus
        // TODO SRombauts : il faudrait plutôt la mettre à jour (la recréer) avec le nombre de messages non lus restant
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(API.NOTIFICATION_NEW_MSG_ID);

        // Efface les flags "UNREAD_TRUE" des messages lus dès qu'on entre !
        ContentValues valuesMsg = new ContentValues();
        valuesMsg.put(SJLB.Msg.UNREAD, SJLB.Msg.UNREAD_LOCALY); // on les passe à UNREAD_LOCALY ce qui indique qu'il faut encore signaler le site Web SJLB du fait qu'on les a lu !
        final String    whereMsg        = "(" + SJLB.Msg.SUBJECT_ID + "=? AND " + SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_TRUE + ")";
        final String[]  selectionArgs   = {Long.toString(mSelectedSubjectId)};  // L'utilisation de "?" optimise la compilation SQL
        final int nbUpdatedRows = getContentResolver ().update(SJLB.Msg.CONTENT_URI, valuesMsg, whereMsg, selectionArgs);
        Log.i (LOG_TAG, "nbUpdatedRows=" + nbUpdatedRows);
        
        // Met à zéro le nombre de messages non lus restant dans le sujet
        ContentValues valuesSubj = new ContentValues();
        valuesSubj.put(SJLB.Subj.NB_UNREAD, 0);
        final String whereSubj = "(" + SJLB.Subj._ID + "=?)";
        // NOTE : selectionArgs est identique
        final int nbUpdatedRowsSubj = getContentResolver ().update(SJLB.Subj.CONTENT_URI, valuesSubj, whereSubj, selectionArgs);
        Log.i (LOG_TAG, "nbUpdatedRowsSubj=" + nbUpdatedRowsSubj);
    }
    
    // Appelée lorsque l'activité se termine ("back")
    protected void onDestroy() {
        super.onDestroy();
        Log.d (LOG_TAG, "onDestroy...");
        
        // Plus de notification de résultat du service, vu qu'on se met en pause !
        mResponseReceiver.unregister(this);
    }

    // Sauvegarde l'état de la boîte d'édition (pour restauration suite à un changement d'orientation par exemple)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i (LOG_TAG, "onSaveInstanceState");
        final boolean bEditTextOpen = (-1 != mOriginalMsgListHeight);
        outState.putBoolean("bEditTextOpen", bEditTextOpen);
        outState.putBoolean("mbIsSending",   mbIsSending);
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
                // Demande de rafraîchissement asynchrone des informations
                StartService.refresh(this);
                Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
                break;
            }
            case (R.id.menu_prefs): {
                Intent intent = new Intent(this, ActivityPreferences.class);
                intent.setAction(ServiceSJLB.ACTION_REFRESH);
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
     *  Sur clic sur un message, fait apparaître la boîte de réponse
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        openEditText ();
    }
    
    /**
     *  Sur long clic sur un message, envoie sur le site Web à la page du sujet contenant ce message
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view, int index, long id) {
        // lien vers le Forum sur le Site Web :
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + mSelectedSubjectId + getString(R.string.sjlb_forum_subj_dernier)));
        Log.d (LOG_TAG, "onItemLongClick: show_online: " + getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + mSelectedSubjectId + getString(R.string.sjlb_forum_subj_dernier));                
        startActivity(intent);
        return true;
    }
    
    /**
     *  Fait apparaître la boîte de réponse directement en bas de la liste de messages
     */
    public void openEditText () {
        // Pour ça, on va réduire la taille occupé par la liste de message
        ViewGroup.LayoutParams params = mMsgListView.getLayoutParams();
        if (-1 == mOriginalMsgListHeight) {
            mOriginalMsgListHeight = params.height;
        }
        // TODO SRombauts : remplacer le nombre de pixel par des "sp" ou trouver une manière vraiment plus élégante de faire 
        params.height = getWindowManager().getDefaultDisplay().getHeight() - 310;
        mMsgListView.requestLayout();
        // On fait apparaître la zone d'édition
        //mMsgListView.setVisibility(View.VISIBLE);
        mEditText.setVisibility(View.VISIBLE);
        mEditText.setEnabled(true);
        mEditButton.setVisibility(View.VISIBLE);
        mEditButton.setEnabled(true);

        // donne le focus à la zone d'édition
        mEditText.requestFocus();
        // et lève le clavier virtuel
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);

        // TODO SRombauts : Scroll tout en bas de la liste des messages ne marche pas :(
        mMsgListView.setSelection(0);         
        mMsgListView.setSelection(mMsgListView.getCount()-1);        
        mMsgListView.getParent().requestLayout();
    }
    
    /**
     *  Fait disparaître la boîte de réponse du bas de la liste de messages
     */
    public void closeEditText () {
        // Pour ça, on va restaurer la taille occupé par la liste de message
        ViewGroup.LayoutParams params = mMsgListView.getLayoutParams();
        if (-1 != mOriginalMsgListHeight) {
            params.height = mOriginalMsgListHeight;
        }
        // On fait disparaître la zone d'édition
        mEditText.setVisibility(View.GONE);
        mEditButton.setVisibility(View.GONE);

        // et cache lève le clavier virtuel
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(mEditText, InputMethodManager.HIDE_IMPLICIT_ONLY);   

        mMsgListView.invalidate();
        mMsgListView.getParent().requestLayout();
    }
    
    /**
     * Envoie au server le nouveau Msg 
     */
    public void onSendMsg (View v) {
        Log.d (LOG_TAG, "onSendMsg ("+ mSelectedSubjectId + " : " + mEditText.getText().toString());
        // Met dans la fifo du service les données du message à envoyer
        StartService.newMsg(this, mSelectedSubjectId, mEditText.getText().toString());
        Toast.makeText(this, getString(R.string.toast_sending), Toast.LENGTH_SHORT).show();
        // Sur tentative d'envoi, verrouille le bouton et le champ texte pour éviter les envois multiples
        mbIsSending = true;
        mEditText.setEnabled(false);
        mEditButton.setEnabled(false);
    }    

    /**
     * Sur réception d'une réponse du service SJLB
     * 
     * @param aIntent Informations sur le type d'action traitée et le résultat obtenu
     */
    @Override
    public void onServiceResponse(Intent intent) {
        String  responseType    = intent.getStringExtra(ServiceSJLB.RESPONSE_INTENT_EXTRA_TYPE);
        boolean bReponseResult  = intent.getBooleanExtra(ServiceSJLB.RESPONSE_INTENT_EXTRA_RESULT, false);
        if (responseType.equals(ServiceSJLB.ACTION_NEW_MSG)) {
            mbIsSending = false;
            if (bReponseResult) {
                Toast.makeText(this, getString(R.string.toast_sent), Toast.LENGTH_SHORT).show();
                // Si le message a été envoyé avec succès, on peur refermer la zone de saisie texte
                closeEditText ();
                // Rafraîchit la liste des messages
                mCursor.requery();
            } else {
                Toast.makeText(this, getString(R.string.toast_not_sent), Toast.LENGTH_SHORT).show();
                // Erreur d'envoi, déverrouille le bouton et le champ texte pour permettre une nouvelle tentative
                mEditText.setEnabled(true);
                mEditButton.setEnabled(true);
            }
        } else if (responseType.equals(ServiceSJLB.ACTION_REFRESH)) {
            if (bReponseResult) {
                if (false != BuildConfig.DEBUG) {
                    // En mise au point uniquement : Toast notification signalant la réponse
                    Toast.makeText(this, "refresh", Toast.LENGTH_SHORT).show();
                }
                // Rafraîchit la liste des messages (il y a peut être de nouveaux messages non lus)
                mCursor.requery();
            }
        }
    }
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activité pour retour à la liste des sujets");
        finish ();
        return true;
    }

    // NOTE SRombauts : pas besoin de onRightGesture()
    
    // Adaptateur mappant les données du curseur dans des objets du cache du pool d'objets View utilisés par la ListView
    private final class MessageListItemAdapter extends ResourceCursorAdapter implements OnClickListener {
        Context mContext;
        
        public MessageListItemAdapter(Context context, int layout, Cursor cursor) {
            super(context, layout, cursor);
            mContext = context;
        }

        // Met à jour avec un nouveau contenu un objet de cache du pool de view utilisées par la ListView 
        // TODO SRombauts : tenter d'optimiser à mort cette méthode qui consomme beaucoup,
        //                   par exemple en remplaçant les getColumnIndexOrThrow par des constantes
        // => pour cela, commencer par ajouter qq traces pour voir ce qui se passe exactement !
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final MessageListItemCache  cache = (MessageListItemCache) view.getTag();
                        
            // Récupère le pseudo et le contact (Uri et photo) éventuellement associé à l'utilisateur
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Msg.AUTHOR_ID));
            UserContactDescr user = ((ApplicationSJLB)getApplication ()).getUserContactById(userId);
            
            // Fixe la barre d'informations du message 
            String  pseudo = user.getPseudo(); // on utilise le pseudo fourni par la liste d'utilisateur, plus simple qu'un croisement en bdd
            cache.pseudoView.setText(pseudo);
            String  strDate = ForumMessage.getDateString (new Date(cursor.getLong(cursor.getColumnIndexOrThrow(SJLB.Msg.DATE)))) ;
            cache.dateView.setText(strDate);
            // Fixe le contenu du message 
            String  text = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.Msg.TEXT));
            cache.textView.setText(text);
            // Fixe l'icone de nouveau message uniquement si le message est nouveau 
            boolean bIsNew = (SJLB.Msg.UNREAD_TRUE == cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Msg.UNREAD)));
            cache.imageViewNew.setVisibility(bIsNew?ImageView.VISIBLE:ImageView.INVISIBLE); 

            // Fixe la barre de QuickContact
            cache.quickContactView.assignContactUri(user.getLookupUri());
            
            // Affiche la photo du contact si elle existe (sinon petite icône de robot par défaut)
            if (null != user.getPhoto()) {
                cache.quickContactView.setImageBitmap(user.getPhoto());
            } else {
                cache.quickContactView.setImageResource(R.drawable.ic_contact_picture);
            }

            // mémorise l'ID du message (en cas de clic sur le bouton "fichiers attachés")
            cache.msgId = cursor.getLong(0); // cursor.getColumnIndexOrThrow(SJLB.Msg._ID)
            
            // Récupère un curseur sur les données (les fichiers) en filtrant sur l'id du sujet sélectionné
            final String[] columns = {SJLB.File.FILENAME};
            Cursor cursorFiles = managedQuery(  SJLB.File.CONTENT_URI,
                                                columns, // ne récupère que le filename
                                                SJLB.File.MSG_ID + "=" + cache.msgId,
                                                null, null);

            int count = cursorFiles.getCount();
            if (0 < count) {
                Log.d(LOG_TAG, "msgId=" + cache.msgId + " nbFiles=" + count);
                cache.fileButton.setVisibility(View.VISIBLE);
                final String attachedFileText;
                if (1 == count) {
                    attachedFileText = getString(R.string.button_show_file);
                } else {
                    attachedFileText = count + " " + getString(R.string.button_show_files);
                }
                cache.fileButton.setText(attachedFileText);
                cache.fileButton.setOnClickListener (this);
            } else {
                cache.fileButton.setVisibility(View.GONE);
                cache.fileButton.setOnClickListener (null);
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
            cache.quickContactView  = (QuickContactBadge)   view.findViewById(R.id.msgBadge);
            cache.pseudoView        = (TextView)            view.findViewById(R.id.msgPseudo);
            cache.dateView          = (TextView)            view.findViewById(R.id.msgDate);
            cache.textView          = (TextView)            view.findViewById(R.id.msgText);
            cache.imageViewNew      = (ImageView)           view.findViewById(R.id.msgNew);
            cache.fileButton        = (Button)              view.findViewById(R.id.msgFileButton);
            // enregistre cet objet de cache
            view.setTag(cache);

            return view;
        }
        
        /**
         *  Sur clic sur le bouton pour voir la liste des fichiers
         */
        @Override
        public void onClick(View view) {
            // lien vers le fichier sur le Site Web :
            final MessageListItemCache  cache = (MessageListItemCache) ((View) view.getParent()).getTag();
            Intent intent = new Intent(mContext, ActivityForumFiles.class);
            intent.putExtra(ActivityForumFiles.START_INTENT_EXTRA_MSG_ID,        cache.msgId);
            intent.putExtra(ActivityForumFiles.START_INTENT_EXTRA_MSG_TEXT,      cache.textView.getText());
            intent.putExtra(ActivityForumFiles.START_INTENT_EXTRA_SUBJ_LABEL,    mSelectedSubjectLabel);
            Log.d (LOG_TAG, "onClick: Message Id=" + cache.msgId);                
            startActivity(intent);
        }
    }
    

    // Objet utilisé comme cache des données d'une View, dans un pool d'objets utilisés par la ListView
    final static class MessageListItemCache {
        public long                 msgId;
        public QuickContactBadge    quickContactView;
        public TextView             pseudoView;
        public TextView             dateView;
        public TextView             textView;
        public ImageView            imageViewNew;
        public Button               fileButton;
    }


}
