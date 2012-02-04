package fr.srombauts.sjlb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 srombauts
 */
public class ActivityForumSubjects extends Activity {
    private static final String LOG_TAG = "ActivitySubj";
    
    public  static final String START_INTENT_EXTRA_CAT_ID = "CategoryId";
    
    private Cursor              mCursor             = null;
    private SimpleCursorAdapter mAdapter            = null;
    private ListView            mSubjectsListView   = null;
    
    private long                mSelectedCategoryId = 0;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout de l'activité
        setContentView(R.layout.subj_list);
        
        // Récupère l'éventuel paramètre de lancement (id de la catégorie du forum sélectionnée)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            mSelectedCategoryId = startIntent.getExtras().getLong(START_INTENT_EXTRA_CAT_ID);
            Log.i(LOG_TAG, "getExtras (" + START_INTENT_EXTRA_CAT_ID + ", " + mSelectedCategoryId + ")");
        }        
        
        // Récupére un curseur sur les données (les sujets) en filtrant sur l'id de la catégorie sélectionnée
        mCursor = managedQuery( SJLB.Subj.CONTENT_URI, null,
                                SJLB.Subj.CAT_ID + "=" + mSelectedCategoryId,
                                null, null);

        // Les colonnes à mapper :
        String[]    from = new String[] { SJLB.Subj.TEXT };
        
        // Les ID des views sur lesquels les mapper :
        int[]       to   = new int[]    { R.id.subjText };

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new SimpleCursorAdapter( this,
                                            R.layout.subj,
                                            mCursor,
                                            from,
                                            to);
        
        mSubjectsListView = (ListView)findViewById(R.id.subj_listview);
        mSubjectsListView.setAdapter (mAdapter);

        mSubjectsListView.setOnItemClickListener(new OnItemClickListener() {
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView adapter, View view, int index, long arg3) {
                // Lance l'activité correspondante avec en paramètre l'id du sujet :
                Intent intent = new Intent(getContext(), ActivityForumMessages.class);
                mCursor.moveToPosition(index);
                long selectedSubjId  = mCursor.getLong(mCursor.getColumnIndex(SJLB.Msg.ID));
                intent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_ID, selectedSubjId);
                Log.d (LOG_TAG, "onItemClick: intent.putExtra(" + ActivityForumMessages.START_INTENT_EXTRA_SUBJ_ID + ", " + selectedSubjId + ")");
                startActivity (intent);
            }
        });
    }
    
    protected Context getContext() {
        return this;
    }

    protected void onResume () {
        super.onResume();
        
        mCursor.requery();
        mAdapter.notifyDataSetChanged();
    }
    
}