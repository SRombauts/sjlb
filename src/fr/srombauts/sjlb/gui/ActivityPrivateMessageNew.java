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
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.service.OnServiceResponseListener;
import fr.srombauts.sjlb.service.ResponseReceiver;
import fr.srombauts.sjlb.service.ServiceSJLB;
import fr.srombauts.sjlb.service.StartService;


/**
 * Activité permettant d'envoyer un nouveau message privé
 * @author 14/08/2010 SRombauts
 */
public class ActivityPrivateMessageNew extends Activity implements OnServiceResponseListener {
    private static final String LOG_TAG = "ActivityNewPM";
    
    public  static final String START_INTENT_EXTRA_AUTHOR_ID = "AuthorId";
    
    private Spinner             mUsersSpinner       = null;
    
    private ResponseReceiver    mResponseReceiver   = null;
    
    private boolean             mbIsSending         = false;
    private EditText            mEditText           = null;
    private Button              mEditButton         = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout de l'activité, et titre
        setContentView(R.layout.pm_new);
        setTitle(getString(R.string.pm_description));

        // Récupère un curseur sur les données (les utilisateurs) 
        Cursor cursor = managedQuery(SJLB.User.CONTENT_URI, null,
                                     null,
                                     null, null);

        // Les colonnes à mapper :
        final String[] from = { SJLB.User.PSEUDO };
        // Les ID des views sur lesquels les mapper :
        final int[]    to   = { android.R.id.text1 };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                                                              android.R.layout.simple_spinner_item,
                                                              cursor,
                                                              from,
                                                              to);
        
        mUsersSpinner = (Spinner)findViewById(R.id.destinataireListView);
        mUsersSpinner.setAdapter (adapter);
        
        // Récupère l'éventuel paramètre de lancement (id de l'auteur du message auquel on souhaite répondre)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            // Sélectionne le destinataire du PM pour répondre
            int authorId = startIntent.getExtras().getInt(START_INTENT_EXTRA_AUTHOR_ID);
            mUsersSpinner.setSelection(authorId-1);
        }
        
        // Binding de la zone de saisie du message
        mEditText   = (EditText)findViewById(R.id.textEditText);
        mEditButton = (Button)findViewById(R.id.buttonSendPm);
        
        // Restaure un éventuel état sauvegardé (état de la boîte d'édition) suite à un changement d'orientation  :
        if (null != savedInstanceState) {
            mbIsSending = savedInstanceState.getBoolean("mbIsSending");
            Log.i(LOG_TAG, "mbIsSending=" + mbIsSending);
            if (mbIsSending) {
                // Sur envoi en cours, verrouille le bouton et le champ texte pour éviter les envois multiples
                mEditText.setEnabled(false);
                mEditButton.setEnabled(false);
            }
        }        
                
        // Demande à être notifié des résultats des actions réalisées par le service
        // (ceci pour toute la durée de vie de l'activité car on ne peut se permettre de louper un ack sous peine d'état incohérent)
        mResponseReceiver = new ResponseReceiver(this);
    }
    
    // Appelée lorsque l'activité se termine ("back")
    protected void onDestroy() {
        super.onDestroy();
        Log.d (LOG_TAG, "onDestroy...");
        
        // Plus de notification de résultat du service
        mResponseReceiver.unregister(this);
    }

    // Sauvegarde l'état de la boîte d'édition (pour restauration suite à un changement d'orientation par exemple)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i (LOG_TAG, "onSaveInstanceState");
        outState.putBoolean("mbIsSending", mbIsSending);        
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
        Log.d (LOG_TAG, "onSendPM ("+ destinataireId +") : " + mEditText.getText().toString());
        // Met dans la fifo du service les données du pm à envoyer
        StartService.newPM(this, destinataireId, mEditText.getText().toString());
        Toast.makeText(this, getString(R.string.toast_sending), Toast.LENGTH_SHORT).show();
        // Sur tentative d'envoi, verrouille le bouton et le champ texte pour éviter les envois multiples
        mbIsSending = true;
        mEditText.setEnabled(false);
        mEditButton.setEnabled(false);
    }
    
    /**
     * Sur réception d'une réponse du service SJLB
     * 
     * @param aIntent Informations sur le type d'action traitée et le résultat obtenu
     */
    @Override
    public void onServiceResponse(Intent intent) {
        String  responseType    = intent.getStringExtra(ServiceSJLB.RESPONSE_INTENT_EXTRA_TYPE);
        boolean bReponseResult  = intent.getBooleanExtra(ServiceSJLB.RESPONSE_INTENT_EXTRA_RESULT, false);
        if (responseType.equals(ServiceSJLB.ACTION_NEW_PM)) {
            mbIsSending = false;
            if (bReponseResult) {
                Toast.makeText(this, getString(R.string.toast_sent), Toast.LENGTH_SHORT).show();
                // Lance l'activité listant les PM envoyés
                Intent intentPmEnvoyes = new Intent(this, ActivityPrivateMessagesSent.class);
                startActivity(intentPmEnvoyes);
                // puis termine l'activité courante pour ne plus repasser par ici
                finish ();
            } else {
                Toast.makeText(this, getString(R.string.toast_not_sent), Toast.LENGTH_SHORT).show();
                // Sur échec d'envoi, déverrouille le bouton et le champ texte pour permettre de retenter l'envoi
                mEditText.setEnabled(true);
                mEditButton.setEnabled(true);
            }
        }
    }
 }