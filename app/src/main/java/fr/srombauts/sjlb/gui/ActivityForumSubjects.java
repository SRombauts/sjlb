package fr.srombauts.sjlb.gui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import fr.srombauts.sjlb.BuildConfig;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.service.OnServiceResponseListener;
import fr.srombauts.sjlb.service.ResponseReceiver;
import fr.srombauts.sjlb.service.ServiceSJLB;
import fr.srombauts.sjlb.service.StartService;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 SRombauts
 */
public class ActivityForumSubjects extends ActivityTouchListener implements OnItemClickListener, OnItemLongClickListener, OnServiceResponseListener {
    private static final String LOG_TAG         = "ActivitySubj";
    
    private static final String SAVE_FILENAME   = "SavedIntent";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_CAT_LABEL    = "CategoryLabel";
    
    private Cursor                  mCursor             = null;
    private SubjectListItemAdapter  mAdapter            = null;
    private ListView                mSubjectsListView   = null;
    
    private long                mSelectedCategoryId     = 0;
    private String              mSelectedCategoryLabel  = "";
    private long                mSelectedSubjId         = 0;
    private long                mSelectedGroupId        = 0;
    private String              mSelectedSubjLabel      = "";
    
    private Intent              mSavedIntent            = null;
        
    private ResponseReceiver    mResponseReceiver       = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d (LOG_TAG, "onCreate...");

        // Layout de l'activité
        setContentView(R.layout.activity_list);
        
        // Récupère l'éventuel paramètre de lancement (id de la catégorie du forum sélectionnée)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            mSelectedCategoryId     = startIntent.getExtras().getLong(START_INTENT_EXTRA_CAT_ID);
            mSelectedCategoryLabel  = startIntent.getExtras().getString(START_INTENT_EXTRA_CAT_LABEL);
            Log.i(LOG_TAG, "SelectedCategory (" + mSelectedCategoryId + ") : " + mSelectedCategoryLabel);
        }
        
        // Map la description de la catégorie pour la renseigner dans le titre
        setTitle(mSelectedCategoryLabel);
        
        // Récupère un curseur sur les données (les sujets) en filtrant sur l'id de la catégorie sélectionnée
        mCursor = managedQuery( SJLB.Subj.CONTENT_URI,
                                null, // Pas d'argument "projection" pour filtrer les colonnes de résultats car elles sont toutes utiles
                                SJLB.Subj.CAT_ID + "=" + mSelectedCategoryId,
                                null, null);

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SubjectListItemAdapter(  this,
                                                R.layout.subj_item,
                                                mCursor);
        
        mSubjectsListView = (ListView)findViewById(R.id.activity_listview);
        mSubjectsListView.setAdapter (mAdapter);

        // Enregistrer les listener d'IHM que la classe implémente        
        mSubjectsListView.setOnItemClickListener(this);
        mSubjectsListView.setOnItemLongClickListener(this);
        mSubjectsListView.setOnTouchListener(this);

        // Restaure les valeurs du dernier intent seulement si elles correspondent à la même catégorie que celle sélectionnée
        SharedPreferences settings = getSharedPreferences(SAVE_FILENAME, 0);
        if (settings.getLong ("mSelectedCategoryId", 0) == mSelectedCategoryId) {
            mSavedIntent = new Intent(this, ActivityForumMessages.class);
            mSelectedSubjId     = settings.getLong   ("mSelectedSubjId",    0);
            mSelectedSubjLabel  = settings.getString ("mSelectedSubjLabel", "");
            mSelectedGroupId    = settings.getLong   ("mSelectedGroupId",   0);
            mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_CAT_ID,        mSelectedCategoryId);
            mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_ID,       mSelectedSubjId);
            mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_LABEL,    mSelectedSubjLabel);
            mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_GROUP_ID,      mSelectedGroupId);
            Log.d (LOG_TAG, "onCreate: restaure l'intent sauvegarde (" + mSelectedCategoryId + ", " + settings.getLong   ("mSelectedSubjId",    0) + ", " + settings.getString ("mSelectedSubjLabel", "") + ", " + settings.getLong   ("mSelectedGroupId",   0) + ")");
        }
    }
    
    @Override
    protected void onResume () {
        super.onResume();
        Log.d (LOG_TAG, "onResume");
        
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
    
    @Override
    public void onStop () {
        super.onStop();
        Log.d (LOG_TAG, "onStop: Sauvegarde les valeurs du dernier intent (" + mSelectedCategoryId + ", " + mSelectedSubjId + ", " + mSelectedGroupId + ", " + mSelectedSubjLabel + ")");
        SharedPreferences           settings    = getSharedPreferences(SAVE_FILENAME, 0);
        SharedPreferences.Editor    editor      = settings.edit();
        editor.putLong  ("mSelectedCategoryId", mSelectedCategoryId);
        editor.putLong  ("mSelectedSubjId",     mSelectedSubjId);
        editor.putString("mSelectedSubjLabel",  mSelectedSubjLabel);
        editor.putLong  ("mSelectedGroupId",    mSelectedGroupId);
        editor.commit();
    }

    /**
     * Sur réception d'une réponse du service SJLB
     * 
     * @param aIntent Informations sur le type d'action traitée et le résultat obtenu
     */
    @Override
    public void onServiceResponse(Intent intent) {
      //String  responseType    = intent.getStringExtra(ServiceSJLB.RESPONSE_INTENT_EXTRA_TYPE);
        boolean bReponseResult   = intent.getBooleanExtra(ServiceSJLB.RESPONSE_INTENT_EXTRA_RESULT, false);
        if (bReponseResult) {
            if (false != BuildConfig.DEBUG) {
                // En mise au point uniquement : Toast notification signalant la réponse
                Toast.makeText(this, "refresh", Toast.LENGTH_SHORT).show();
            }
            // Rafraîchit la liste des sujets (il y a peut être de nouveaux messages non lus)
            mCursor.requery();
        }
    }
        
    /**
     * Création du menu général
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.subj, menu);
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
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_cat_uri) + mSelectedCategoryId));
                Log.d (LOG_TAG, "onOptionsItemSelected: menu_show_online: " + getString(R.string.sjlb_forum_cat_uri) + mSelectedCategoryId);                
                startActivity(intent);
                break;
            }
            case (R.id.menu_new_subj): {
                // TODO SRombauts : lancer l'activité chargée de créer un nouveau sujet dans la catégorie
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
     *  Sur sélection d'un sujet, lance l'activité "messages du forum" avec en paramètre l'id du sujet :
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSavedIntent = new Intent(this, ActivityForumMessages.class);
        mCursor.moveToPosition(position);
        mSelectedSubjId      = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj._ID));
        mSelectedSubjLabel   = mCursor.getString(mCursor.getColumnIndexOrThrow(SJLB.Subj.TEXT));
        mSelectedGroupId     = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.GROUP_ID));
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_CAT_ID,        mSelectedCategoryId);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_ID,       mSelectedSubjId);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_LABEL,    mSelectedSubjLabel);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_GROUP_ID,      mSelectedGroupId);
        Log.d (LOG_TAG, "onItemClick: mSavedIntent.putExtra(" + mSelectedSubjId + ", " + mSelectedSubjLabel + ")");
        startActivity (mSavedIntent);
    }

    
    /**
     *  Sur long clic sur un sujet, envoie sur le site Web sur le sujet concerné
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view, int index, long id) {
        // lien vers le Forum sur le Site Web :
        mCursor.moveToPosition(index);
        long selectedSubjectId = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj._ID));
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + selectedSubjectId + getString(R.string.sjlb_forum_subj_dernier)));
        Log.d (LOG_TAG, "onItemLongClick: show_online: " + getString(R.string.sjlb_forum_subj_uri) + mSelectedCategoryId + getString(R.string.sjlb_forum_subj_param) + selectedSubjectId + getString(R.string.sjlb_forum_subj_dernier));                
        startActivity(intent);
        return true;
    }
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'écran de gauche... quitte l'activité courante pour retour à l'écran principal");
        // Quitte l'activité
        finish ();
        return true;
    }
    
    @Override
    protected boolean onRightGesture () {
        boolean bActionTraitee = false;
        
        // Ne restaure l'Intent sauvegardé que s'il existe et que l'Id du sujet sélectionné n'est pas 0
        if ( (null != mSavedIntent) && (0 != mSelectedSubjId) ) {
            Log.d (LOG_TAG, "onTouch: va a l'écran de droite... relance le dernier intent sauvegardé");
            startActivity (mSavedIntent);
            bActionTraitee = true;
        } else {
           Log.w (LOG_TAG, "onTouch: pas d'intent sauvegardé, ou sujet null");
        }
        
        return bActionTraitee;
    }
    
    
    // Adaptateur mappant les données du curseur dans des objets du cache du pool d'objets View utilisés par la ListView :
    // ceci remplace un SimpleCursorAdaptor pour permettre de customiser l'affichage du nombre de messages non lus (SSI != 0)
    // TODO SRO : revoir complètement sont fonctionnement, on dirait que le cache n'est pas utilisé et qu'en plus les sujets sont chacun appelés/construits 3 fois ! 
    private final class SubjectListItemAdapter extends ResourceCursorAdapter {
        public SubjectListItemAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c);
        }

        // Met à jour avec un nouveau contenu un objet de cache du pool de view utilisées par la ListView 
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final SubjectListItemCache  cache = (SubjectListItemCache)view.getTag();

            // Récupère le titre du sujet
            String  title = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.Subj.TEXT));
            // et le nb de messages non lus
            final int NbUnread = cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Subj.NB_UNREAD));
            if (0 < NbUnread) {
                // et ajoute au titre le nb de messages non lus si non nul
                title += " (" + NbUnread + ")";
            }
            cache.nameView.setText(title);
        }

        // Création d'une nouvelle View et de son objet de cache (vide) pour le pool
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            
            // Construction d'un nouvel objet de cache
            SubjectListItemCache cache = new SubjectListItemCache();
            // et binding sur les View décrites par le Layout
            cache.nameView  = (TextView)view.findViewById(R.id.subjText);
            // enregistre cet objet de cache
            view.setTag(cache);

            return view;
        }
    }
    
    // Objet utilisé comme cache des données d'une View, dans un pool d'objets utilisés par la ListView
    final static class SubjectListItemCache {
        public TextView nameView;
    }    
}