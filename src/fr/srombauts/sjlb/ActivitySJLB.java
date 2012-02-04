package fr.srombauts.sjlb;

import java.util.ArrayList;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
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
public class ActivitySJLB extends Activity {
    private static final String LOG_TAG = "ActivitySJLB";

    // Liste des catégories du forum
    private ListView        mCategoriesListView = null;
    ArrayAdapter<String>    mAA;
    ArrayList<String>       mCategories = new ArrayList<String>();
    
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
        
        // TODO SRO : ne pas créer de lister directement dans le code comme ça !
        mCategoriesListView.setOnItemClickListener(new OnItemClickListener() {
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView adpter, View view, int index, long arg3) {
                // Lance l'activité correspondante avec en paramètre l'id et le label de la catégorie sélectionnée
                Intent intent = new Intent(getContext(), ActivityForumSubjects.class);
                String[] categoryLabels = getResources().getStringArray(R.array.category_labels);
                long selectedCategoryId  = index+1;
                intent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_ID,    selectedCategoryId);
                intent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_LABEL, categoryLabels[index]);
                Log.d (LOG_TAG, "onItemClick: intent.putExtra(" + selectedCategoryId + ", " + categoryLabels[index] + ")");
                startActivity (intent);
            }
        });
        
        // Lance le service, si pas déjà lancé, et provoque un rafraichissement
        IntentReceiverStartService.startService (this, LOG_TAG);
    }

    protected void onResume () {
        super.onResume();
        
        clearNotificationMsg ();
    }

    private void clearNotificationMsg () {
        // Annule l'éventuelle notification de Msg non lus
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(AsynchTaskRefresh.NOTIFICATION_NEW_MSG_ID);
    }

    protected Context getContext() {
        return this;
    }

    public boolean onTouch (View v, MotionEvent event) {
        return false;
    }
        
    /**
     * Création du menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    
    /**
     * Sur sélection dans le menu
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
        LoginPassword loginPassword = null;
        try {
            loginPassword = new LoginPassword(this);
        } catch (LoginPasswordException e) {
            e.printStackTrace();
        }
        
        switch (item.getItemId()) {
            case (R.id.menu_show_pm): {
                if (null != loginPassword) {
                    Intent intent = new Intent(this, ActivityPrivateMessages.class);
                    startActivity(intent);
                } else {
                    // Toast notification signalant l'absence de login/password
                    Toast.makeText(this, getString(R.string.authentication_needed), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case (R.id.menu_update): {
                if (null != loginPassword) {
                    // Toast notification de début de rafraichissement
                    Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
                    // TODO voir si c'est la meilleurs manière de faire...
                    IntentReceiverStartService.startService (this, LOG_TAG);
                } else {
                    // Toast notification signalant l'absence de login/password
                    Toast.makeText(this, getString(R.string.authentication_needed), Toast.LENGTH_SHORT).show();
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
     * Lance l'activité présentant la liste des PM, sur click du bouton correspondant 
     */
    public void onShowPM (View v) {
        // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
        try {
            new LoginPassword(this);
            // Lance l'activité lisant les PM
            Intent intent = new Intent(this, ActivityPrivateMessages.class);
            startActivity(intent);
        } catch (LoginPasswordException e) {
            e.printStackTrace();
            // Toast notification signalant l'absence de login/password
            Toast.makeText(this, getString(R.string.authentication_needed), Toast.LENGTH_SHORT).show();
        }
    }
}
