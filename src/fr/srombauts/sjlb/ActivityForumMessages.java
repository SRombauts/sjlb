package fr.srombauts.sjlb;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 srombauts
 */
public class ActivityForumMessages extends Activity {
    private static final String LOG_TAG = "ActivityMsg";
    
    public  static final String START_INTENT_EXTRA_SUBJ_ID = "SubjectId";
    
    private Cursor              mCursor         = null;
    private SimpleCursorAdapter mAdapter        = null;
    private ListView            mMsgListView    = null;
    
    private long                mSelectedSubjectId  = 0;
    
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
            mSelectedSubjectId = startIntent.getExtras().getLong(START_INTENT_EXTRA_SUBJ_ID);
            Log.i(LOG_TAG, "getExtras (" + START_INTENT_EXTRA_SUBJ_ID + ", " + mSelectedSubjectId + ")");
        }        
        
        // Récupére un curseur sur les données (les messages) en filtrant sur l'id du sujet sélectionné
        mCursor = managedQuery( SJLB.Msg.CONTENT_URI, null,
                                SJLB.Msg.SUBJECT_ID + "=" + mSelectedSubjectId,
                                null, null);

        // Les colonnes à mapper :
        String[]    from = new String[] { SJLB.Msg.AUTHOR_ID, SJLB.Msg.AUTHOR, SJLB.Msg.DATE, SJLB.Msg.TEXT };
        
        // Les ID des views sur lesquels les mapper :
        int[]       to   = new int[]    { R.id.msgAuthorId, R.id.msgAuthor, R.id.msgDate, R.id.msgText };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SimpleCursorAdapter( this,
                                            R.layout.msg,
                                            mCursor,
                                            from,
                                            to);
        
        mMsgListView = (ListView)findViewById(R.id.msg_listview);
        mMsgListView.setAdapter (mAdapter);
    }
    
    protected void onResume () {
        super.onResume();
        
        mCursor.requery();
        mAdapter.notifyDataSetChanged();
    }
    
}