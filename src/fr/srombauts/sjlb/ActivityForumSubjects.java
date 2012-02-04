package fr.srombauts.sjlb;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 srombauts
 */
public class ActivityForumSubjects extends Activity implements OnItemClickListener, OnTouchListener {
    private static final String LOG_TAG = "ActivitySubj";
    
    public  static final String START_INTENT_EXTRA_CAT_ID       = "CategoryId";
    public  static final String START_INTENT_EXTRA_CAT_LABEL    = "CategoryLabel";
    
    private Cursor              mCursor             = null;
    private SimpleCursorAdapter mAdapter            = null;
    private ListView            mSubjectsListView   = null;
    
    private long                mSelectedCategoryId     = 0;
    private String              mSelectedCategoryLabel  = "";
    
    private float               mTouchStartPositionX    = 0;
    private float               mTouchStartPositionY    = 0;
    
    private Intent              mSavedIntent            = null;
    
    
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

        // Enregister les listener d'IHM que la classe implémente        
        mSubjectsListView.setOnItemClickListener(this);
        mSubjectsListView.setOnTouchListener(this);
        mSubjectsListView.getRootView().setOnTouchListener(this);
    }
    
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.w (LOG_TAG, "onSaveInstanceState");       
    }
    
    protected void onResume () {
        super.onResume();
        
        mCursor.requery();
        mAdapter.notifyDataSetChanged();
    }

    /**
     *  Lance l'activité correspondante avec en paramètre l'id du sujet :
     */
    @SuppressWarnings("unchecked")
    public void onItemClick(AdapterView adapter, View view, int index, long arg3) {
        mSavedIntent = new Intent(this, ActivityForumMessages.class);
        mCursor.moveToPosition(index);
        long    selectedCategoryId  = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.CAT_ID));
        long    selectedSubjId      = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.ID));
        long    selectedGroupId     = mCursor.getLong  (mCursor.getColumnIndexOrThrow(SJLB.Subj.GROUP_ID));
        String  selectedSubjLabel   = mCursor.getString(mCursor.getColumnIndexOrThrow(SJLB.Subj.TEXT));
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_CAT_ID,        selectedCategoryId);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_ID,       selectedSubjId);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_SUBJ_LABEL,    selectedSubjLabel);
        mSavedIntent.putExtra(ActivityForumMessages.START_INTENT_EXTRA_GROUP_ID,      selectedGroupId);
        Log.d (LOG_TAG, "onItemClick: mSavedIntent.putExtra(" + selectedSubjId + ", " + selectedSubjLabel + ")");
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
        
}