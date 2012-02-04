package fr.srombauts.sjlb;

import java.util.ArrayList;

import android.app.Activity;
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
public class ActivitySJLB extends Activity implements OnItemClickListener, OnTouchListener {
    private static final String LOG_TAG = "ActivitySJLB";

    // Liste des catégories du forum
    private ListView        mCategoriesListView = null;
    ArrayAdapter<String>    mAA;
    ArrayList<String>       mCategories         = new ArrayList<String>();

    private float           mTouchStartPositionX = 0;
    private float           mTouchStartPositionY = 0;
    
    private Intent          mSavedIntent            = null;
    
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

        // Lance le service, si pas déjà lancé, et provoque un rafraichissement
        IntentReceiverStartService.startService (this, LOG_TAG);
    }

    @SuppressWarnings("unchecked")
    public void onItemClick(AdapterView adpter, View view, int index, long arg3) {
        // Lance l'activité correspondante avec en paramètre l'id et le label de la catégorie sélectionnée
        mSavedIntent = new Intent(this, ActivityForumSubjects.class);
        String[] categoryLabels = getResources().getStringArray(R.array.category_labels);
        long selectedCategoryId  = index+1;
        mSavedIntent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_ID,    selectedCategoryId);
        mSavedIntent.putExtra(ActivityForumSubjects.START_INTENT_EXTRA_CAT_LABEL, categoryLabels[index]);
        Log.d (LOG_TAG, "onItemClick: mSavedIntent.putExtra(" + selectedCategoryId + ", " + categoryLabels[index] + ")");
        startActivity (mSavedIntent);
    }

    // TODO SRO : callback d'évènement tactiles, à mutualiser entre les activités
    public boolean onTouch(View aView, MotionEvent aMotionEvent) {
        boolean     bActionTraitee = false;
        final int   touchAction = aMotionEvent.getAction();
        final float touchX      = aMotionEvent.getX();
        final float touchY      = aMotionEvent.getY();
        
        switch (touchAction)
        {
            case MotionEvent.ACTION_DOWN: {
                //Log.d (LOG_TAG, "onTouch (ACTION_DOWN) : touch (" + touchX + ", " + touchY + ")");
                mTouchStartPositionX = touchX;
                mTouchStartPositionY = touchY;
                break;
            }
            case MotionEvent.ACTION_UP: {
                //Log.d (LOG_TAG, "onTouch (ACTION_UP) : touch (" + touchX + ", " + touchY + ")");
                final float proportionalDeltaX = (touchX - mTouchStartPositionX) / (float)aView.getWidth();
                final float proportionalDeltaY = (touchY - mTouchStartPositionY) / (float)aView.getHeight();
                //Log.d (LOG_TAG, "onTouch: deltas proportionnels : (" + proportionalDeltaX + ", " + proportionalDeltaY + ")");
                
                // Teste si le mouvement correspond à un mouvement franc
                if (   (Math.abs(proportionalDeltaX) > 0.2)                                 // mouvement d'ampleur importante
                    && (Math.abs(proportionalDeltaX)/Math.abs(proportionalDeltaY) > 0.8) )  // mouvement plus latéral que vertical
                {
                    //Log.d (LOG_TAG, "onTouch: mouvement lateral franc");
                    
                    // Teste sa direction :
                    if (proportionalDeltaX > 0) {
                        if (null != mSavedIntent) {
                            Log.i (LOG_TAG, "onTouch: mouvement vers la droite, on relance le dernier intent sauvegardé");
                            startActivity (mSavedIntent);
                            bActionTraitee = true;
                        } else {
                           Log.w (LOG_TAG, "onTouch: mouvement vers la droite mais pas d'intent sauvegardé");
                        }
                    }
                    else {
                        Log.i (LOG_TAG, "onTouch: mouvement vers la gauche, on quitte l'activité");
                        bActionTraitee = true;
                        finish ();
                    }
                }
                break;
            }
            default: {
                //Log.d (LOG_TAG, "onTouch autre (" + touchAction  + ") : touch (" + touchX + ", " + touchY + ")");
            }
        }

        // Si on n'a pas déjà traité l'action, on passe la main à la Vue sous-jacente
        if (false == bActionTraitee) {
            aView.onTouchEvent(aMotionEvent);
        }
        
        // Si on retourne false, on n'est plus notifié des évènements suivants
        return true;
    }
    
    /**
     * Création du menu général
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
        
        
        switch (item.getItemId()) {
            case (R.id.menu_show_pm): {
                startActivityPM ();
                break;
            }
            case (R.id.menu_update): {
                // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
                if (LoginPassword.AreFilled (this)) {
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
        if (LoginPassword.AreFilled (this)) {
            // Lance l'activité lisant les PM
            Intent intent = new Intent(this, ActivityPrivateMessages.class);
            startActivity(intent);
        } else {
            // Toast notification signalant l'absence de login/password
            Toast.makeText(this, getString(R.string.authentication_needed), Toast.LENGTH_SHORT).show();
        }
    }
}
