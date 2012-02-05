package fr.srombauts.sjlb;

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


/**
 * Activité du menu principal, qui lance le service si besoin et permet de naviguer entre PM et Msg
 * @author 27/06/2010 srombauts
 */
public class ActivitySJLB extends ActivityTouchListener implements OnItemClickListener {
    private static final String LOG_TAG         = "ActivitySJLB";

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
        
        // binding de la liste des catégories et du champ de version
        mCategoriesListView     = (ListView)findViewById(R.id.categoriesListView);
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
        
        // Enregister les listener d'IHM que la classe implémente
        mCategoriesListView.setOnItemClickListener(this);
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
        
        // Lance le service, si pas déjà lancé, et provoque un rafraichissement
        IntentReceiverStartService.startService (this, LOG_TAG);
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
    }

    @SuppressWarnings("unchecked")
    public void onItemClick(AdapterView adpter, View view, int index, long arg3) {
        // Lance l'activité correspondante avec en paramètre l'id et le label de la catégorie sélectionnée
        mSavedIntent = new Intent(this, ActivityForumSubjects.class);
        String[] categoryLabels = getResources().getStringArray(R.array.category_labels);
        mSelectedCategoryId     = index+1;
        mSelectedCategoryLabel  = categoryLabels[index];
        mSavedIntent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_ID,    mSelectedCategoryId);
        mSavedIntent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_LABEL, mSelectedCategoryLabel);
        Log.d (LOG_TAG, "onItemClick: mSavedIntent.putExtra(" + mSelectedCategoryId + ", " + mSelectedCategoryLabel + ")");
        startActivity (mSavedIntent);
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
				// lien vers le Forum sur le Site Web :
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_uri)));
				startActivity(intent);
                break;
            }
            case (R.id.menu_show_pm): {
                startActivityPM ();
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
            case (R.id.menu_reset): {
                ContentProviderUser users = new ContentProviderUser (this);
                users.clearUser();
                ContentProviderPM   pms = new ContentProviderPM (this);
                pms.clearPM();
                ContentProviderSubj subjs = new ContentProviderSubj (this);
                subjs.clearSubj();
                ContentProviderMsg  msgs = new ContentProviderMsg (this);
                msgs.clearMsg();
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
