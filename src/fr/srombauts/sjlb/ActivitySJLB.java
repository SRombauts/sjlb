package fr.srombauts.sjlb;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


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
    
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Lance le service, si pas déjà lancé, et provoque un rafraichissement
        startService ();
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
            /* TODO à complèter
            case (MENU_ID_SHOW_MSG): {
                Intent intent = new Intent(this, SJLBForumMessages.class);

                break;
            }*/
            case (MENU_ID_UPDATE): {
                // Toast notification de début de rafraichissement (pour le debug uniquement !)
                Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
                // TODO voir si c'est la meilleurs manière de faire...
                startService ();
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

    /**
     * Lance le service de rafraichissemment, si pas déjà lancé
     */
    private void startService () {
        Intent  intentService = new Intent();
        intentService.setClassName( "fr.srombauts.sjlb", "fr.srombauts.sjlb.ServiceRefresh");
        ComponentName cname = startService(intentService);
        if (cname == null)
            Log.e(LOG_TAG, "SJLB Service was not started");
        else
            Log.d(LOG_TAG, "SJLB Service started");
    }
}
