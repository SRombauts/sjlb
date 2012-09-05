package fr.srombauts.sjlb.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import fr.srombauts.sjlb.BuildConfig;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.ContentProviderFile;
import fr.srombauts.sjlb.db.ContentProviderMsg;
import fr.srombauts.sjlb.db.ContentProviderPM;
import fr.srombauts.sjlb.db.ContentProviderSubj;
import fr.srombauts.sjlb.db.ContentProviderUser;
import fr.srombauts.sjlb.db.DBOpenHelper;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.model.PrefsLoginPassword;
import fr.srombauts.sjlb.service.API;
import fr.srombauts.sjlb.service.IntentReceiverStartService;
import fr.srombauts.sjlb.service.OnServiceResponseListener;
import fr.srombauts.sjlb.service.ResponseReceiver;
import fr.srombauts.sjlb.service.ServiceSJLB;
import fr.srombauts.sjlb.service.StartService;


/**
 * Activité du menu principal, qui lance le service si besoin et permet de naviguer entre PM et Msg
 * @author 27/06/2010 SRombauts
 */
public class ActivityMain extends ActivityTouchListener implements OnItemClickListener, OnItemLongClickListener, OnServiceResponseListener {
    private static final String LOG_TAG         = "ActivityMain";

    private static final String SAVE_FILENAME   = "SavedIntent";

    // Liste des catégories du forum
    private ListView            mCategoriesListView     = null;
    private ArrayAdapter<String>mAA                     = null;
    private ArrayList<String>   mCategories             = new ArrayList<String>();

    private long                mSelectedCategoryId     = 0;
    private String              mSelectedCategoryLabel  = "";

    private Intent              mSavedIntent            = null;
    
    private ResponseReceiver    mResponseReceiver       = null;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d (LOG_TAG, "onCreate...");
        
        // Layout de l'activité
        setContentView(R.layout.main_list);

        // Demande si nécessaire les infos de login/mot de passe
        final boolean bPasswordAsked = new LoginPasswordPopup(this).show();
        if (false == bPasswordAsked) {
            // A la première ouverture de l'application sans demande de renseignement de mot de passe,
            // affiche la descriptions des nouvelles fonctionnalités et correction de la version logicielle.
            new WhatsNewScreen(this).show();
        }
        
        // binding de la liste des catégories
        mCategoriesListView     = (ListView)findViewById(R.id.categoriesListView);
        // Create the array adapter to bind the array to the listview
        mAA = new ArrayAdapter<String>( this,
                                        R.layout.main_item,
                                        mCategories);
        mCategoriesListView.setAdapter(mAA);

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
    }

    // Appelée lorsque l'activité était déjà lancée (par exemple clic sur une notification de nouveau Msg)
    protected void onNewIntent(Intent intent) {
        Log.d (LOG_TAG, "onNewIntent...");
    }
    
    // Appelée lorsque l'activité passe de "en pause/cachée" à "au premier plan"
    protected void onResume() {
        super.onResume();
        Log.d (LOG_TAG, "onResume...");

        // Rafraîchi l'affichage
        refreshCategoryList ();
        
        // Demande à être notifié des résultats des actions réalisées par le service
        mResponseReceiver = new ResponseReceiver(this);

        // Lance l'alarme périodique, et le service, si pas déjà lancé, et provoque un rafraîchissement
        IntentReceiverStartService.startAlarm(this, LOG_TAG);
    }
    
    // Appelée lorsque l'activité passe de "au premier plan" à "en pause/cachée" 
    protected void onPause() {
        super.onPause();
        
        // Plus de notification de résultat du service, vu qu'on se met en pause !
        mResponseReceiver.unregister(this);
        mResponseReceiver = null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Sauvegarde les valeurs du dernier intent
        Log.d (LOG_TAG, "onDestroy: Sauvegarde les valeurs du dernier intent (" + mSelectedCategoryId +", " + mSelectedCategoryLabel + ")" );
        SharedPreferences           settings    = getSharedPreferences(SAVE_FILENAME, 0);
        SharedPreferences.Editor    editor      = settings.edit();
        editor.putLong  ("mSelectedCategoryId",     mSelectedCategoryId);
        editor.putString("mSelectedCategoryLabel",  mSelectedCategoryLabel);
        editor.commit();
        
        // Provoque un rafraîchissement des infos anticipé,
        // qui permet de signaler au site web SJLB les messages qui ont été lus   
        StartService.refresh(this);
    }

    /**
     *  Rafraîchit la liste des categories :
     *  récupération de la liste des catégories, avec le nombre de msg non lus,
     *  et la transforme en un tableau pour servir un ArrayAdapter standard.
     */
    private void refreshCategoryList() {
        DBOpenHelper DBHelper = new DBOpenHelper(this, SJLB.DATABASE_NAME, null, SJLB.DATABASE_VERSION);
        
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

        // Signale à l'adapteur que les données ont changés
        mAA.notifyDataSetChanged();
        
        
        // Compte le nombre de messages récupérés en base locale
        ContentProviderMsg  msgs    = new ContentProviderMsg (this);
        final long NbMsg = msgs.getCount();
        msgs.close();
        
        // Lit les informations de version du package courant
        PackageManager  manager = getPackageManager();
        PackageInfo     info    = null;
        try {
            info = manager.getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // Puis affiches ces infos dans le titre
        setTitle(getString(R.string.app_name) + " " + info.versionName + "   (" + NbMsg + " " + getString(R.string.msg_description) + ")");
        
        // Récupère l'état et la date de la dernière synchronisation avec le serveur SJLB 
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String lastSynchroStatus  = prefs.getString(API.PREFS_STATUS_LAST_SYNCHRO, "");
        final long   lastSynchroTime    = prefs.getLong(API.PREFS_DATE_LAST_SYNCHRO, 0);
        final Date   lastSynchroDate    = new Date(lastSynchroTime*1000);
        final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        // Puis affiches ces infos sous la liste
        String textSynchStatus = dateFormat.format(lastSynchroDate) + " (" + lastSynchroStatus + ")";
        TextView textSynchStatusView = (TextView)findViewById(R.id.textSynchStatus);
        textSynchStatusView.setText(textSynchStatus);
    }    
    
    /**
     * Sur réception d'une réponse du service SJLB
     * 
     * @param aIntent Informations sur le type d'action traitée et le résultat obtenu
     */
    @Override
    public void onServiceResponse(Intent intent) {
        // Rafraîchi dans tous les cas l'affichage (même s'il y a eu une erreur, ce qui permet d'afficher le status de l'erreur)
        if (false != BuildConfig.DEBUG) {
            // En mise au point uniquement : Toast notification signalant la réponse
            Toast.makeText(this, "refresh", Toast.LENGTH_SHORT).show();
        }
        // Met à jour l'affichage de la liste des catégories (il y a peut être de nouveaux messages)
        refreshCategoryList();
    }
    
    // Sur clic sur l'une des catégories
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
        if (PrefsLoginPassword.AreFilled (this)) {
            // Lance l'activité correspondante avec en paramètre l'id et le label de la catégorie sélectionnée
            mSavedIntent = new Intent(this, ActivityForumSubjects.class);
            String[] categoryLabels = getResources().getStringArray(R.array.category_labels);
            mSelectedCategoryId     = position+1;
            mSelectedCategoryLabel  = categoryLabels[position];
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
    public boolean onItemLongClick(AdapterView<?> parent, View view, int index, long id) {
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
            case (R.id.menu_update): {
                // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
                if (PrefsLoginPassword.AreFilled (this)) {
                    // Demande de rafraîchissement asynchrone des informations
                    StartService.refresh(this);
                    Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
                } else {
                    // Toast notification signalant l'absence de login/password
                    Toast.makeText(this, getString(R.string.toast_auth_needed), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            // TODO SRombauts : en mise au point uniquement
            case (R.id.menu_reset): {
                Log.d (LOG_TAG, "onOptionsItemSelected(menu_reset)");
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

    /**
     * Sur click du bouton correspondant, lance l'activité d'affichage des membres du site SJLB
     */
    public void onShowUserList (View v) {
        // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
        if (PrefsLoginPassword.AreFilled (this)) {
            // Lance l'activité lisant la liste des utilisateurs
            Intent intent = new Intent(this, ActivityUsers.class);
            startActivity(intent);
        } else {
            // Toast notification signalant l'absence de login/password
            Toast.makeText(this, getString(R.string.toast_auth_needed), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activité principale et donc l'application !");
        finish ();
        return true;
    }
    
    @Override
    protected boolean onRightGesture () {
        boolean bActionTraitee = false;
        
        if (null != mSavedIntent) {
            Log.d (LOG_TAG, "onTouch: va a l'écran de droite... relance le dernier intent sauvegardé");
            startActivity (mSavedIntent);
            bActionTraitee = true;
        } else {
           Log.w (LOG_TAG, "onTouch: pas d'intent sauvegardé");
        }
        
        return bActionTraitee;
    }
}
