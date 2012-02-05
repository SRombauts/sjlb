package fr.srombauts.sjlb;

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Activité du menu principal, qui lance le service si besoin et permet de naviguer entre PM et Msg
 * @author 27/06/2010 srombauts
 */
public class ActivityMain extends ActivityTouchListener implements OnItemClickListener, OnItemLongClickListener {
    private static final String LOG_TAG         = "ActivityMain";

    private static final String SAVE_FILENAME   = "SavedIntent";

    // Liste des catégories du forum
    private ListView        mCategoriesListView     = null;
    ArrayAdapter<String>    mAA;
    ArrayList<String>       mCategories             = new ArrayList<String>();

    private long            mSelectedCategoryId     = 0;
    private String          mSelectedCategoryLabel  = "";

    private Intent          mSavedIntent            = null;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout de l'activité
        setContentView(R.layout.main);

        // Affiche si nécessaire les modifications de la nouvelle version
        new WhatsNewScreen(this).show();

        // Demande si nécessaire les infos de login/mot de passe
        new LoginPasswordPopup(this).show();
        
        // binding de la liste des catégories
        mCategoriesListView     = (ListView)findViewById(R.id.categoriesListView);
        // Create the array adapter to bind the array to the listview
        mAA = new ArrayAdapter<String>( this,
                                        android.R.layout.simple_list_item_1,
                                        mCategories);
        mCategoriesListView.setAdapter(mAA);
        
        // binding du champ de version
        TextView    VersionView = (TextView)findViewById(R.id.versionView);
        
        // Lit les informations de version du package courant
        PackageManager  manager = getPackageManager();
        PackageInfo     info    = null;
        try {
            info = manager.getPackageInfo(getPackageName(), 0);
            VersionView.setText("version " + info.versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        
        // Enregistre les listener d'IHM que la classe implémente
        mCategoriesListView.setOnItemClickListener(this);
        mCategoriesListView.setOnItemLongClickListener(this);
        mCategoriesListView.setOnTouchListener(this);
        mCategoriesListView.getRootView().setOnTouchListener(this);

        // Restaure les valeurs du dernier intent
        SharedPreferences settings = getSharedPreferences(SAVE_FILENAME, 0);
        mSelectedCategoryId     = settings.getLong  ("mSelectedCategoryId",     0);
        mSelectedCategoryLabel  = settings.getString("mSelectedCategoryLabel",  "");
        mSavedIntent = new Intent(this, ActivityForumSubjects.class);
        mSavedIntent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_ID,    mSelectedCategoryId);
        mSavedIntent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_LABEL, mSelectedCategoryLabel);
        Log.i (LOG_TAG, "onCreate: restaure l'intent sauvegarde (" + mSelectedCategoryId +", " + mSelectedCategoryLabel + ")" );

        getApplicationContext ();
        
        // Lance le service, si pas déjà lancé, et provoque un rafraichissement
        IntentReceiverStartService.startService (this, LOG_TAG);
    }

    // Appelée lorsque l'activité était déjà lancée (par exemple clic sur une notification de nouveau Msg)
    protected void onNewIntent (Intent intent) {
    }
    
    // Appelée lorsque l'activité passe de "en pause/cachée" à "au premier plan"
    protected void onResume () {
        super.onResume();

        DBOpenHelper DBHelper = new DBOpenHelper(this, SJLB.DATABASE_NAME, null, SJLB.DATABASE_VERSION);
        
        // rafraichi la liste des category 
        // Récupération de la liste des catégories, avec le nombre de msg non lus :
        mCategories.clear();
        String [] categories = getResources().getStringArray(R.array.category_labels);
        for (int catIdx = 0; catIdx < categories.length; catIdx++) {
            Cursor cursor = DBHelper.getReadableDatabase().query(
                    SJLB.Subj.TABLE_NAME + ", " + SJLB.Msg.TABLE_NAME,
                    null,
                      "(" + SJLB.Subj.TABLE_NAME + "." + SJLB.Subj._ID + "=" + SJLB.Msg.SUBJECT_ID + ")"
                    + " AND "
                    + "(" + SJLB.Subj.CAT_ID + "=" + (catIdx+1) + ")"
                    + " AND "
                    + "(" + SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_TRUE + ")",
                    null,
                    null,
                    null,
                    null);
            String titre = categories[catIdx];
            if (0 < cursor.getCount()) {
                titre += " (" + cursor.getCount() + ")"; 
            }
            cursor.close();
            mCategories.add(titre);            
        }
        DBHelper.close();
        
        mAA.notifyDataSetChanged();
    }
    
    @Override
    public void onDestroy () {
        super.onDestroy();
        
        // Sauvegarde les valeurs du dernier intent
        Log.d (LOG_TAG, "onDestroy: Sauvegarde les valeurs du dernier intent (" + mSelectedCategoryId +", " + mSelectedCategoryLabel + ")" );
        SharedPreferences           settings    = getSharedPreferences(SAVE_FILENAME, 0);
        SharedPreferences.Editor    editor      = settings.edit();
        editor.putLong  ("mSelectedCategoryId",     mSelectedCategoryId);
        editor.putString("mSelectedCategoryLabel",  mSelectedCategoryLabel);
        editor.commit();
        
        // Provoque un rafraichissement des infos anticipé,
        // TODO SRO : ce qui permettra aussi de signaler au site web SJLB la lecture des messages  
        // TODO voir si c'est la meilleurs manière de faire...
        IntentReceiverStartService.startService (this, LOG_TAG);
    }

    public void onItemClick(AdapterView adpter, View view, int index, long arg3) {
        // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
        if (PrefsLoginPassword.AreFilled (this)) {
            // Lance l'activité correspondante avec en paramètre l'id et le label de la catégorie sélectionnée
            mSavedIntent = new Intent(this, ActivityForumSubjects.class);
            String[] categoryLabels = getResources().getStringArray(R.array.category_labels);
            mSelectedCategoryId     = index+1;
            mSelectedCategoryLabel  = categoryLabels[index];
            mSavedIntent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_ID,    mSelectedCategoryId);
            mSavedIntent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_LABEL, mSelectedCategoryLabel);
            Log.d (LOG_TAG, "onItemClick: mSavedIntent.putExtra(" + mSelectedCategoryId + ", " + mSelectedCategoryLabel + ")");
            startActivity (mSavedIntent);
        } else {
            // Toast notification signalant l'absence de login/password
            Toast.makeText(this, getString(R.string.toast_auth_needed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  Sur long clic sur un sujet, envoie sur le site Web sur la catégorie concernée
     */
    public boolean onItemLongClick(AdapterView adapter, View view, int index, long id) {
        // lien vers le Forum sur le Site Web, à la catégorie correspondant à l'index cliqué dans la liste des catégories :
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_cat_uri) + (index+1)));
        Log.d (LOG_TAG, "onItemLongClick: show_online: " + getString(R.string.sjlb_forum_subj_uri) + (index+1));                
        startActivity(intent);
        return true;
    }

    /**
     * Création du menu général
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
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
                // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
                if (PrefsLoginPassword.AreFilled (this)) {
    				// lien vers le Forum sur le Site Web :
    				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_uri)));
    				startActivity(intent);
                } else {
                    // Toast notification signalant l'absence de login/password
                    Toast.makeText(this, getString(R.string.toast_auth_needed), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case (R.id.menu_show_pm): {
                startActivityPM ();
                break;
            }
            case (R.id.menu_update): {
                // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
                if (PrefsLoginPassword.AreFilled (this)) {
                    // Rafraichissement des infos
                    // TODO voir si c'est la meilleurs manière de faire...
                    IntentReceiverStartService.startService (this, LOG_TAG);
                    // Toast notification de début de rafraichissement
                    Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
                    // rafraichi la liste des categories 
                    mAA.notifyDataSetChanged();
                } else {
                    // Toast notification signalant l'absence de login/password
                    Toast.makeText(this, getString(R.string.toast_auth_needed), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case (R.id.menu_reset): {
                ContentProviderUser users   = new ContentProviderUser (this);
                users.clearUser();
                users.close();
                ContentProviderPM   pms     = new ContentProviderPM (this);
                pms.clearPM();
                pms.close();
                ContentProviderSubj subjs   = new ContentProviderSubj (this);
                subjs.clearSubj();
                subjs.close();
                ContentProviderMsg  msgs    = new ContentProviderMsg (this);
                msgs.clearMsg();
                msgs.close();
                ContentProviderFile files   = new ContentProviderFile (this);
                files.clearFiles();
                files.close();
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
     * Sur click du bouton correspondant, lance l'activité d'affichage des PM
     */
    public void onShowPM (View v) {
        startActivityPM ();
    }

    /**
     * Lance l'activité présentant la liste des PM, sur click du bouton correspondant 
     */
    public void startActivityPM () {
        // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
        if (PrefsLoginPassword.AreFilled (this)) {
            // Lance l'activité lisant les PM
            Intent intent = new Intent(this, ActivityPrivateMessages.class);
            startActivity(intent);
        } else {
            // Toast notification signalant l'absence de login/password
            Toast.makeText(this, getString(R.string.toast_auth_needed), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activite et l'application !");
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
