package fr.srombauts.sjlb;

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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 srombauts
 */
public class ActivityForumSubjects extends ActivityTouchListener implements OnItemClickListener {
    private static final String LOG_TAG         = "ActivitySubj";
    
    private static final String SAVE_FILENAME   = "SavedIntent";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_CAT_LABEL    = "CategoryLabel";
    
    private Cursor              mCursor             = null;
    private SimpleCursorAdapter mAdapter            = null;
    private ListView            mSubjectsListView   = null;
    
    private long                mSelectedCategoryId     = 0;
    private String              mSelectedCategoryLabel  = "";
    private long                mSelectedSubjId         = 0;
    private long                mSelectedGroupId        = 0;
    private String              mSelectedSubjLabel      = "";
    
    private Intent              mSavedIntent            = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout de l'activité
        setContentView(R.layout.subj_list);
        
        // Récupère l'éventuel paramètre de lancement (id de la catégorie du forum sélectionnée)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            mSelectedCategoryId     = startIntent.getExtras().getLong(START_INTENT_EXTRA_CAT_ID);
            mSelectedCategoryLabel  = startIntent.getExtras().getString(START_INTENT_EXTRA_CAT_LABEL);
            Log.i(LOG_TAG, "SelectedCategory (" + mSelectedCategoryId + ") : " + mSelectedCategoryLabel);
        }
        
        // Map la description de la catégorie pour la renseigner
        TextView CategoryDescription = (TextView)findViewById(R.id.category_label);
        CategoryDescription.setText(mSelectedCategoryLabel);        
        
        // Récupére un curseur sur les données (les sujets) en filtrant sur l'id de la catégorie sélectionnée
        mCursor = managedQuery( SJLB.Subj.CONTENT_URI, null,
                                SJLB.Subj.CAT_ID + "=" + mSelectedCategoryId,
                                null, null);

        // Les colonnes à mapper :
        String[]    from = new String[] { SJLB.Subj.TEXT };
        
        // Les ID des views sur lesquels les mapper :
        int[]       to   = new int[]    { R.id.subjText };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SimpleCursorAdapter( this,
                                            R.layout.subj,
                                            mCursor,
                                            from,
                                            to);
        
        mSubjectsListView = (ListView)findViewById(R.id.subj_listview);
        mSubjectsListView.setAdapter (mAdapter);

        // Enregister les listener d'IHM que la classe implémente        
        mSubjectsListView.setOnItemClickListener(this);
        mSubjectsListView.setOnTouchListener(this);
        mSubjectsListView.getRootView().setOnTouchListener(this);

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

        mCursor.requery();
        mAdapter.notifyDataSetChanged();
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
                // TODO SRO : lancer l'activité chargée de créer un nouveau sujet dans la catégorie
                break;
            }
            case (R.id.menu_update): {
                // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
                if (PrefsLoginPassword.AreFilled (this)) {
                    // Toast notification de début de rafraichissement
                    Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
                    // TODO voir si c'est la meilleurs manière de faire...
                    IntentReceiverStartService.startService (this, LOG_TAG);
                } else {
                    // Toast notification signalant l'absence de login/password
                    Toast.makeText(this, getString(R.string.toast_auth_needed), Toast.LENGTH_SHORT).show();
                }
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
    @SuppressWarnings("unchecked")
    @Override
    public void onItemClick(AdapterView adapter, View view, int index, long arg3) {
        mSavedIntent = new Intent(this, ActivityForumMessages.class);
        mCursor.moveToPosition(index);
        mSelectedSubjId      = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.ID));
        mSelectedSubjLabel   = mCursor.getString(mCursor.getColumnIndexOrThrow(SJLB.Subj.TEXT));
        mSelectedGroupId     = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.GROUP_ID));
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_CAT_ID,        mSelectedCategoryId);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_ID,       mSelectedSubjId);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_LABEL,    mSelectedSubjLabel);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_GROUP_ID,      mSelectedGroupId);
        Log.d (LOG_TAG, "onItemClick: mSavedIntent.putExtra(" + mSelectedSubjId + ", " + mSelectedSubjLabel + ")");
        startActivity (mSavedIntent);
    }

    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... pour retour à l'ecran principal");
        // Quitte l'activité
        finish ();
        return true;
    }
    
    @Override
    protected boolean onRightGesture () {
        boolean bActionTraitee = false;
        
        if (null != mSavedIntent) {
            Log.d (LOG_TAG, "onTouch: va a l'ecran de droite... relance le dernier intent sauvegarde");
            startActivity (mSavedIntent);
            bActionTraitee = true;
        } else {
           Log.w (LOG_TAG, "onTouch: pas d'intent sauvegardé");
        }
        
        return bActionTraitee;
    }
    
}