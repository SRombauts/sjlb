package fr.srombauts.sjlb.gui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.service.AsynchTaskNewPM;


/**
 * Activité permettant d'envoyer un nouveau message privé
 * @author 14/08/2010 SRombauts
 */
public class ActivityNewPrivateMessage extends Activity {
    private static final String LOG_TAG = "ActivityNewPM";
    
    public  static final String START_INTENT_EXTRA_AUTHOR_ID = "AuthorId";
    
    private Cursor              mCursor         = null;
    private SimpleCursorAdapter mAdapter        = null;
    private Spinner             mUsersSpinner   = null;
    
    private EditText            mText           = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout de l'activité, et titre
        setContentView(R.layout.pm_new);
        setTitle(getString(R.string.pm_description));

        // Récupère un curseur sur les données (les utilisateurs) 
        mCursor = managedQuery( SJLB.User.CONTENT_URI, null,
                                null,
                                null, null);

        // Les colonnes à mapper :
        final String[] from = { SJLB.User.PSEUDO };
        // Les ID des views sur lesquels les mapper :
        final int[]    to   = { android.R.id.text1 };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SimpleCursorAdapter( this,
                                            android.R.layout.simple_spinner_item,
                                            mCursor,
                                            from,
                                            to);
        
        mUsersSpinner = (Spinner)findViewById(R.id.destinataireListView);
        mUsersSpinner.setAdapter (mAdapter);
        
        // Récupère l'éventuel paramètre de lancement (id de l'auteur du message auquel on souhaite répondre)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            // Sélectionne le destinataire du PM pour répondre
            int authorId = startIntent.getExtras().getInt(START_INTENT_EXTRA_AUTHOR_ID);
            mUsersSpinner.setSelection(authorId-1);
        }
        
        // Binding de la zone de saisie du message
        mText = (EditText)findViewById(R.id.textEditText);
    }
    
    /**
     * Création du menu général
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pm_new, menu);
        return true;
    }

    /**
     * Création du menu contextuel
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pm_context, menu);
    }
    
    /**
     * Sur sélection dans le menu
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case (R.id.menu_send_pm): {
                onSendPM (null);
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
     * Envoie au server le nouveau PM 
     */
    public void onSendPM (View v) {
        long destinataireId = mUsersSpinner.getSelectedItemId();
        Log.d (LOG_TAG, "onSendPM ("+ destinataireId +") : " + mText.getText().toString());
        AsynchTaskNewPM TaskSendPM = new AsynchTaskNewPM(this);
        // Envoie le message en le passant en paramètres
        TaskSendPM.execute(Long.toString(destinataireId), mText.getText().toString());
        finish ();
    }
}