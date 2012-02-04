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
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 srombauts
 */
public class ActivityForumSubjects extends Activity {
    private static final String LOG_TAG = "ActivitySubj";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_CAT_LABEL    = "CategoryLabel";
    
    private Cursor              mCursor             = null;
    private SimpleCursorAdapter mAdapter            = null;
    private ListView            mSubjectsListView   = null;
    
    private long                mSelectedCategoryId     = 0;
    private String              mSelectedCategoryLabel  = "";
    
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
            mSelectedCategoryId     = startIntent.getExtras().getLong(START_INTENT_EXTRA_CAT_ID);
            mSelectedCategoryLabel  = startIntent.getExtras().getString(START_INTENT_EXTRA_CAT_LABEL);
            Log.i(LOG_TAG, "SelectedCategory (" + mSelectedCategoryId + ") : " + mSelectedCategoryLabel);
        }
        
        // Map la description de la catégorie pour la renseigner
        TextView CategoryDescription = (TextView)findViewById(R.id.category_label);
        CategoryDescription.setText(mSelectedCategoryLabel);        
        
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

        // TODO SRO : ne pas créer de lister directement dans le code comme ça !
        mSubjectsListView.setOnItemClickListener(new OnItemClickListener() {
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView adapter, View view, int index, long arg3) {
                // Lance l'activité correspondante avec en paramètre l'id du sujet :
                Intent intent = new Intent(getContext(), ActivityForumMessages.class);
                mCursor.moveToPosition(index);
                long    selectedCategoryId  = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.CAT_ID));
                long    selectedSubjId      = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.ID));
                long    selectedGroupId     = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.GROUP_ID));
                String  selectedSubjLabel   = mCursor.getString(mCursor.getColumnIndexOrThrow(SJLB.Subj.TEXT));
                intent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_CAT_ID,        selectedCategoryId);
                intent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_ID,       selectedSubjId);
                intent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_LABEL,    selectedSubjLabel);
                intent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_GROUP_ID,      selectedGroupId);
                Log.d (LOG_TAG, "onItemClick: intent.putExtra(" + selectedSubjId + ", " + selectedSubjLabel + ")");
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