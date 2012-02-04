package fr.srombauts.sjlb;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 srombauts
 */
public class ActivityForumMessages extends Activity implements OnTouchListener {
    private static final String LOG_TAG = "ActivityMsg";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_SUBJ_ID      = "SubjectId";
    public  static final String START_INTENT_EXTRA_SUBJ_LABEL   = "SubjectLabel";
    public  static final String START_INTENT_EXTRA_GROUP_ID     = "GroupId";
    
    private Cursor              mCursor         = null;
    private SimpleCursorAdapter mAdapter        = null;
    private ListView            mMsgListView    = null;
    
    private long                mSelectedCategoryId     = 0;
    private long                mSelectedSubjectId      = 0;
    private String              mSelectedSubjectLabel   = "";
    private long                mSelectedGroupId        = 0;
   
    private float               mTouchStartPositionX    = 0;
    private float               mTouchStartPositionY    = 0;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout de l'activité
        setContentView(R.layout.msg_list);
        
        // Récupère l'éventuel paramètre de lancement (id de du sujet du forum sélectionnée)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            mSelectedCategoryId     = startIntent.getExtras().getLong  (START_INTENT_EXTRA_CAT_ID);
            mSelectedSubjectId      = startIntent.getExtras().getLong  (START_INTENT_EXTRA_SUBJ_ID);
            mSelectedSubjectLabel   = startIntent.getExtras().getString(START_INTENT_EXTRA_SUBJ_LABEL);
            mSelectedGroupId        = startIntent.getExtras().getLong  (START_INTENT_EXTRA_GROUP_ID);
            Log.i(LOG_TAG, "SelectedSubject (" + mSelectedSubjectId +", " + mSelectedGroupId + " [" + mSelectedCategoryId + "]) : " + mSelectedSubjectLabel);
        }        
        
        // Map la description du sujet pour la renseigner
        TextView SubjectsDescription = (TextView)findViewById(R.id.subject_label);
        SubjectsDescription.setText(mSelectedSubjectLabel);        
        
        // Récupére un curseur sur les données (les messages) en filtrant sur l'id du sujet sélectionné
        mCursor = managedQuery( SJLB.Msg.CONTENT_URI, null,
                                SJLB.Msg.SUBJECT_ID + "=" + mSelectedSubjectId,
                                null, null);

        // Les colonnes à mapper :
        String[]    from = new String[] { SJLB.Msg.AUTHOR, SJLB.Msg.DATE, SJLB.Msg.TEXT };
        
        // Les ID des views sur lesquels les mapper :
        int[]       to   = new int[]    { R.id.msgAuthor, R.id.msgDate, R.id.msgText };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SimpleCursorAdapter( this,
                                            R.layout.msg,
                                            mCursor,
                                            from,
                                            to);
        
        mMsgListView = (ListView)findViewById(R.id.msg_listview);
        mMsgListView.setAdapter (mAdapter);
        // Scroll tout en bas de la liste des messages
        mMsgListView.setSelection(mMsgListView.getCount()-1);
        
        // Enregister les listener d'IHM que la classe implémente        
        mMsgListView.setOnTouchListener(this);
        mMsgListView.getRootView().setOnTouchListener(this);
    }
    
    protected void onResume () {
        super.onResume();
        
        clearNotificationMsg ();
        
        mCursor.requery();
        mAdapter.notifyDataSetChanged();
    }
    
    /**
     * Annule l'éventuelle notification de Msg non lus
     */
    private void clearNotificationMsg () {
        String              ns                      = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager     = (NotificationManager) getSystemService(ns);
        notificationManager.cancel(AsynchTaskRefresh.NOTIFICATION_NEW_MSG_ID);
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
                        Log.d (LOG_TAG, "onTouch: mouvement vers la droite");
                        
                        // TODO SRO : action intelligente : passer au prochain sujet contenant des messages non lus ? 
                        //bActionTraitee = true;
                        
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
        inflater.inflate(R.menu.msg, menu);
        return true;
    }

    /**
     * Sur sélection dans le menu
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case (R.id.menu_new_msg): {
                Intent intent = new Intent(this, ActivityNewForumMessage.class);
                // TODO SRO : passer les paramètre requis !
                intent.putExtra(ActivityNewForumMessage.START_INTENT_EXTRA_CAT_ID,        mSelectedCategoryId);
                intent.putExtra(ActivityNewForumMessage.START_INTENT_EXTRA_SUBJ_ID,       mSelectedSubjectId);
                intent.putExtra(ActivityNewForumMessage.START_INTENT_EXTRA_SUBJ_LABEL,    mSelectedSubjectLabel);
                intent.putExtra(ActivityNewForumMessage.START_INTENT_EXTRA_GROUP_ID,      mSelectedGroupId);
                Log.d (LOG_TAG, "onItemClick: intent.putExtra(" + mSelectedSubjectId + ", " + mSelectedSubjectLabel + ")");                
                startActivity(intent);
                break;
            }
            case (R.id.menu_update): {
                // Toast notification de début de rafraichissement
                Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
                // TODO voir si c'est la meilleurs manière de faire : donnerait plus de contrôle si l'on pouvait faire un accès direct à la AsynchTask...
                IntentReceiverStartService.startService (this, LOG_TAG);
                // TODO SRO : trouver un moyen de rafraichir la liste à l'échéance de la tache de rafraichissement
                mCursor.requery();
                mAdapter.notifyDataSetChanged();
                break;            }
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
   
}