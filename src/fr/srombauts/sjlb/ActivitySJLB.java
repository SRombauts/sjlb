package fr.srombauts.sjlb;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
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
public class ActivitySJLB extends Activity {
    private static final String LOG_TAG = "SJLBMain";

    static final private int MENU_ID_SHOW_PM    = Menu.FIRST;
    static final private int MENU_ID_SHOW_MSG   = Menu.FIRST + 1;
    static final private int MENU_ID_UPDATE     = Menu.FIRST + 2;
    static final private int MENU_ID_RESET      = Menu.FIRST + 3;
    static final private int MENU_ID_PREFS      = Menu.FIRST + 4;
    static final private int MENU_ID_QUIT       = Menu.FIRST + 5;

    // Liste des catégories du forum
    private ListView        mCategoriesListView = null;
    ArrayAdapter<String>    mAA;
    ArrayList<String>       mCategories = new ArrayList<String>();
    
    
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // bind la liste des catégories
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
        
        mCategoriesListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView adpter, View view, int index, long arg3) {
                // Lance un intent correspondant à la catégorie du forum
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_cat_uri)+(index+1)));
                startActivity(intent);                
            }
        });
      
        // Lance le service, si pas déjà lancé, et provoque un rafraichissement
        IntentReceiverStartService.startService (this, LOG_TAG);
    }

    /**
     * Création du menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ID_SHOW_PM,  Menu.NONE, R.string.menu_show_pm);
        menu.add(0, MENU_ID_SHOW_MSG, Menu.NONE, R.string.menu_show_msg);
        menu.add(0, MENU_ID_UPDATE,   Menu.NONE, R.string.menu_update);
        menu.add(0, MENU_ID_RESET,    Menu.NONE, R.string.menu_reset);
        menu.add(0, MENU_ID_PREFS,    Menu.NONE, R.string.menu_prefs);
        menu.add(0, MENU_ID_QUIT,     Menu.NONE, R.string.menu_quit);
        
        return true;
    }
    
    /**
     * Sur sélection dans le menu
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case (MENU_ID_SHOW_PM): {
                Intent intent = new Intent(this, ActivityPrivateMessages.class);
                startActivity(intent);
                break;
            }
            case (MENU_ID_SHOW_MSG): {
                // TODO lien temporaire : à implémenter correctement
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(getString(R.string.sjlb_forum_uri)));
                startActivity(intent);                
                /*
                Intent intent = new Intent(this, SJLBForumMessages.class);
                */
                break;
            }
            case (MENU_ID_UPDATE): {
                // Utilise les préférences pour voir si le login et mot de passe sont renseignés  :
                SharedPreferences   prefs       = PreferenceManager.getDefaultSharedPreferences(this);
                String              login       = prefs.getString(SJLB.PREFS.LOGIN,    "");
                String              password    = prefs.getString(SJLB.PREFS.PASSWORD, "");

                if (   (false == login.contentEquals(""))
                    && (false == password.contentEquals("")) )
                {
                    // Toast notification de début de rafraichissement
                    Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
                    // TODO voir si c'est la meilleurs manière de faire...
                    IntentReceiverStartService.startService (this, LOG_TAG);
                }
                else
                {
                    // Toast notification signalant l'absence de login/password
                    Toast.makeText(this, getString(R.string.refresh_impossible), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case (MENU_ID_RESET): {
                ContentProviderPM   pms = new ContentProviderPM (this);
                pms.clearPM();
                ContentProviderMsg  msgs = new ContentProviderMsg (this);
                msgs.clearMsg();
                break;
            }
            case (MENU_ID_PREFS): {
                Intent intent = new Intent(this, ActivityPreferences.class);
                startActivity(intent);
                break;
            }
            case (MENU_ID_QUIT): {
                finish ();
                break;
            }
            default:
                return false;
        }
        return true;
    }

    public void onShowPM (View v) {
        Intent intent = new Intent(this, ActivityPrivateMessages.class);
        startActivity(intent);        
    }
}
