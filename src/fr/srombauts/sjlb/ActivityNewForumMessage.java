package fr.srombauts.sjlb;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.TextView;


/**
 * Activité permettant d'envoyer un nouveau message du forum
 * @author 24/08/2010 srombauts
 */
public class ActivityNewForumMessage extends Activity {
    private static final String LOG_TAG = "ActivityNewMsg";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_SUBJ_ID      = "SubjectId";
    public  static final String START_INTENT_EXTRA_SUBJ_LABEL   = "SubjectLabel";
    public  static final String START_INTENT_EXTRA_GROUP_ID     = "GroupId";
    
    private long                mSelectedCategoryId     = 0;
    private long                mSelectedSubjectId      = 0;
    private String              mSelectedSubjectLabel   = "";
    private long                mSelectedGroupId        = 0;

    private EditText            mText   = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout de l'activité
        setContentView(R.layout.msg_new);
        
        // Récupère les paramètres de lancement (id du sujet auquel on souhaite répondre)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            mSelectedCategoryId     = startIntent.getExtras().getLong  (START_INTENT_EXTRA_CAT_ID);
            mSelectedSubjectId      = startIntent.getExtras().getLong  (START_INTENT_EXTRA_SUBJ_ID);
            mSelectedSubjectLabel   = startIntent.getExtras().getString(START_INTENT_EXTRA_SUBJ_LABEL);
            mSelectedGroupId        = startIntent.getExtras().getLong  (START_INTENT_EXTRA_GROUP_ID);
            Log.i(LOG_TAG, "SelectedSubject (" + mSelectedCategoryId + ", " + mSelectedSubjectId + " [" + mSelectedGroupId + "]) : " + mSelectedSubjectLabel);
        }

        // Map la description du sujet pour la renseigner
        TextView SubjectsDescription = (TextView)findViewById(R.id.msg_new_label);
        SubjectsDescription.setText(mSelectedSubjectLabel);        
        
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
                onSendMsg (null);
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
     * Envoie au server le nouveau Msg 
     */
    public void onSendMsg (View v) {
        Log.d (LOG_TAG, "onSendMsg ("+ mSelectedCategoryId +", " + mSelectedSubjectId + ", [" + mSelectedGroupId + "]) : " + mText.getText().toString());
        AsynchTaskNewMsg TaskSendMsg = new AsynchTaskNewMsg(this);
        // Envoie le message en le passant en paramètres
        TaskSendMsg.execute(Long.toString(mSelectedCategoryId),
                            Long.toString(mSelectedSubjectId),
                            Long.toString(mSelectedGroupId),
                            mText.getText().toString());
        finish ();
    }
}