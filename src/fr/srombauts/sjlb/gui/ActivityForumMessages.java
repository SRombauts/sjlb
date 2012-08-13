package fr.srombauts.sjlb.gui;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import fr.srombauts.sjlb.ApplicationSJLB;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.model.ForumMessage;
import fr.srombauts.sjlb.model.UserContactDescr;
import fr.srombauts.sjlb.service.AsynchTaskDownloadImage;
import fr.srombauts.sjlb.service.AsynchTaskNewMsg;
import fr.srombauts.sjlb.service.AsynchTaskRefresh;
import fr.srombauts.sjlb.service.CallbackImageDownload;
import fr.srombauts.sjlb.service.CallbackTransfer;
import fr.srombauts.sjlb.service.IntentReceiverStartService;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 SRombauts
 */
public class ActivityForumMessages extends ActivityTouchListener implements OnItemClickListener, OnItemLongClickListener, CallbackTransfer {
    private static final String LOG_TAG = "ActivityMsg";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_SUBJ_ID      = "SubjectId";
    public  static final String START_INTENT_EXTRA_SUBJ_LABEL   = "SubjectLabel";
    public  static final String START_INTENT_EXTRA_GROUP_ID     = "GroupId";
    
    private Cursor                  mCursor         = null;
    private MessageListItemAdapter  mAdapter        = null;
    static private ListView         mMsgListView    = null; // TODO SRombauts "static" pour être accéder depuis la CallbackImageDownload
    private EditText                mEditText       = null;
    private Button                  mEditButton     = null;
    
    private long                    mSelectedCategoryId     = 0;
    private long                    mSelectedSubjectId      = 0;
    private String                  mSelectedSubjectLabel   = "";
    private long                    mSelectedGroupId        = 0;
    
    private int                     mOriginalMsgListHeight  = -1; 
    
    
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
        mMsgListView.getRootView().setOnTouchListener(this);
    }
    
    @Override
    protected void onResume () {
        super.onResume();
        Log.d (LOG_TAG, "onResume... clear UNREAD flags");

        // Supprime l'éventuelle notification de Msg non lus
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(AsynchTaskRefresh.NOTIFICATION_NEW_MSG_ID);

        // Efface les flags "UNREAD_TRUE" des messages lus dès qu'on entre !
        ContentValues valuesMsg = new ContentValues();
        valuesMsg.put(SJLB.Msg.UNREAD, SJLB.Msg.UNREAD_LOCALY); // on les passe à UNREAD_LOCALY ce qui indique qu'il faut encore signaler le site Web SJLB du fait qu'on les a lu !
        final String    whereMsg        = "(" + SJLB.Msg.SUBJECT_ID + "=? AND " + SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_TRUE + ")";
        final String[]  selectionArgs   = {Long.toString(mSelectedSubjectId)};  // L'utilisation de "?" optimise la compilation SQL
        final int nbUpdatedRows = getContentResolver ().update(SJLB.Msg.CONTENT_URI, valuesMsg, whereMsg, selectionArgs);
        Log.w (LOG_TAG, "nbUpdatedRows=" + nbUpdatedRows);
        
        // Met à zéro le nombre de messages non lus restant dans le sujet
        ContentValues valuesSubj = new ContentValues();
        valuesSubj.put(SJLB.Subj.NB_UNREAD, 0);
        final String whereSubj = "(" + SJLB.Subj._ID + "=?)";
        // NOTE : selectionArgs est identique
        final int nbUpdatedRowsSubj = getContentResolver ().update(SJLB.Subj.CONTENT_URI, valuesSubj, whereSubj, selectionArgs);
        Log.w (LOG_TAG, "nbUpdatedRowsSubj=" + nbUpdatedRowsSubj);
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
                // Toast notification de début de rafraîchissement
                Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
                // TODO voir si c'est la meilleurs manière de faire : donnerait plus de contrôle si l'on pouvait faire un accès direct à la AsynchTask...
                IntentReceiverStartService.startService (this, LOG_TAG);
                // TODO SRombauts : trouver un moyen de rafraîchir la liste à l'échéance de la tache de rafraîchissement
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
     *  Sur clic sur un message, fait apparaître la boîte de réponse
     */
    public void onItemClick(AdapterView<?> parent, View view, int index, long arg3) {
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
        params.height = getWindowManager().getDefaultDisplay().getHeight() - 380;
        mMsgListView.requestLayout();
        // On fait apparaître la zone d'édition
        //mMsgListView.setVisibility(View.VISIBLE);
        mEditText.setVisibility(View.VISIBLE);
        mEditButton.setVisibility(View.VISIBLE);

        // donne le focus à la zone d'édition
        mEditText.requestFocus();
        // et lève le clavier virtuel
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);

        // TODO SRombauts : Scroll tout en bas de la liste des messages ne marche pas :(
        mMsgListView.setSelection(0);         
        mMsgListView.setSelection(mMsgListView.getCount()-1);        

// TODO SRombauts : tests en cours
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
        Log.d (LOG_TAG, "onSendMsg ("+ mSelectedCategoryId +", " + mSelectedSubjectId + ", [" + mSelectedGroupId + "]) : " + mEditText.getText().toString());
        AsynchTaskNewMsg TaskSendMsg = new AsynchTaskNewMsg(this);
        // Envoie le message en le passant en paramètres
        TaskSendMsg.execute(Long.toString(mSelectedCategoryId),
                            Long.toString(mSelectedSubjectId),
                            Long.toString(mSelectedGroupId),
                            mEditText.getText().toString());
    }    

    // Appelée lorsqu'un transfert s'est terminé (post d'un nouveau messages, effacement d'un PM...)
    public void onTransferDone (boolean abResult) {
        // Si le message a été envoyé avec succès, on peur refermer la zone de saisie texte
        if (abResult) {
            closeEditText ();
        }
    }
    
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activité pour retour à la liste des sujets");
        finish ();
        return true;
    }

    // TODO SRombauts : onRightGesture ?
    
    
    
    
    // Adaptateur mappant les données du curseur dans des objets du cache du pool d'objets View utilisés par la ListView
    private final class MessageListItemAdapter extends ResourceCursorAdapter implements OnItemClickListener {
        public MessageListItemAdapter(Context context, int layout, Cursor cursor) {
            super(context, layout, cursor);
        }

        // Met à jour avec un nouveau contenu un objet de cache du pool de view utilisées par la ListView 
        // TODO SRombauts : tenter d'optimiser à mort cette méthode qui consomme beaucoup,
        //                   par exemple en remplaçant les getColumnIndexOrThrow par des constantes
        // => pour cela, commencer par ajouter qq traces pour voir ce qui se passe exactement !
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final MessageListItemCache  cache = (MessageListItemCache) view.getTag();
            
            // Fixe la barre d'informations du message 
            String  pseudo = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.User.PSEUDO)); // on utilise le champ "PSEUDO" issu du croisement avec la table 
            cache.pseudoView.setText(pseudo);
            String  strDate = ForumMessage.getDateString (new Date(cursor.getLong(cursor.getColumnIndexOrThrow(SJLB.Msg.DATE)))) ;
            cache.dateView.setText(strDate);
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
            
            // Récupère un curseur sur les données (les fichiers) en filtrant sur l'id du sujet sélectionné
            // TODO SRombauts : utiliser l'argument "projection" pour filtrer les résultats et ainsi optimiser l'utilisation mémoire
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
                                                                        R.layout.file_item,
                                                                        arrayFileListItem);

            cache.fileListView.setAdapter (adapterFiles);
            cache.fileListView.setOnItemClickListener (this);
        }

        // Création d'une nouvelle View et de son objet de cache (vide) pour le pool
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            
            // Construction d'un nouvel objet de cache
            MessageListItemCache cache = new MessageListItemCache();
            // et binding sur les View décrites par le Layout
            cache.quickContactView  = (QuickContactBadge)   view.findViewById(R.id.msgBadge);
            cache.pseudoView        = (TextView)            view.findViewById(R.id.msgPseudo);
            cache.dateView          = (TextView)            view.findViewById(R.id.msgDate);
            cache.textView          = (TextView)            view.findViewById(R.id.msgText);
            cache.imageViewNew      = (ImageView)           view.findViewById(R.id.msgNew);
            cache.fileListView      = (ListView)            view.findViewById(R.id.msgFileListview);
            // enregistre cet objet de cache
            view.setTag(cache);

            return view;
        }
        
        /**
         *  Sur clic sur un fichier, l'affiche ou le télécharge
         */
        public void onItemClick(AdapterView<?> parent, View view, int index, long arg3) {
            // lien vers le fichier sur le Site Web :
            final FileListItem  file = (FileListItem) view.getTag();
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_fichiers_attaches) + file.filename));
            Log.d (LOG_TAG, "onClick: " + getString(R.string.sjlb_fichiers_attaches) + file.filename );                
            startActivity(intent);
        }
    }
    

    // Objet utilisé comme cache des données d'une View, dans un pool d'objets utilisés par la ListView
    final static class MessageListItemCache {
        public QuickContactBadge    quickContactView;
        public TextView             pseudoView;
        public TextView             dateView;
        public TextView             textView;
        public ImageView            imageViewNew;
        public ListView             fileListView;
    }

    
    
    // TODO SRombauts : documentation !
    private class FileListItemAdapter extends ArrayAdapter<FileListItem> {

        private FileListItem[] mListeItem;

        public FileListItemAdapter(Context context, int textViewResourceId, FileListItem[] items) {
            super(context, textViewResourceId, items);
            mListeItem = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Log.d (LOG_TAG, "getView(" + position + "," + convertView + "," + parent + ")" );

            View view = convertView;
            if (view == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.file_item, null);

                // TODO SRombauts : tests en cours
                FileListItem it = mListeItem[position];
                if (it != null) {
                    Log.i (LOG_TAG, "getView(" + it.filename + ")" );
                    
                    it.imageViewFile = (ImageView) view.findViewById(R.id.fileImage);
                    
                    // TODO SRombauts : il faut lancer ici le téléchargement du fichier en tache de fond (SSI il s'agit bien d'une image !)
                    if (null == it.imageBitmap) {
                        AsynchTaskDownloadImage ImageDownloader = new AsynchTaskDownloadImage(it);
                        ImageDownloader.execute(getString(R.string.sjlb_fichiers_attaches) + it.filename);
                    }
                    
                    // Affiche le nom du fichier
                    it.textViewFile  = (TextView) view.findViewById(R.id.filename);
                    it.textViewFile.setText (it.filename);
                }
                // Mémorise dans la vue les infos sous-jacentes
                view.setTag (it);

            }
            return view;
        }
    }    
        
    // Objet représentant une image et le nom du fichier associé
    final static class FileListItem implements CallbackImageDownload {
        public ImageView    imageViewFile;
        public TextView     textViewFile;
        public Bitmap       imageBitmap;
        public String       filename;
        
        public void onImageDownloaded(Bitmap aBitmap) {
            if (null != aBitmap) {
                Log.i (LOG_TAG, "onImageDownloaded(" + aBitmap + ")" );
                textViewFile.setVisibility(TextView.GONE);
                imageViewFile.setImageBitmap(aBitmap);
                imageViewFile.setAdjustViewBounds(true);
                imageViewFile.setHorizontalScrollBarEnabled(true);
                //imageViewFile.getSuggestedMinimumHeight(); 
                // TODO SRombauts : tests en cours
                imageViewFile.invalidate ();
                imageViewFile.requestLayout();
                imageViewFile.getParent().requestLayout();
                imageViewFile.getParent().recomputeViewAttributes (imageViewFile);
                mMsgListView.invalidate();
                mMsgListView.requestLayout();
            } else {
                Log.e (LOG_TAG, "onImageDownloaded(" + aBitmap + ")" );
            }
        }
        
    }
}
