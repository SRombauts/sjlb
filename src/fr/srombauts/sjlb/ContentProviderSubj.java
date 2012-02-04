package fr.srombauts.sjlb;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.net.Uri;


/**
 * Encapsulation des données pour stockage en base de données
 * 
 * @author seb
 */
public class ContentProviderSubj extends ContentProvider {

    private static final int SUBJ_ALL         = 1;
    private static final int SUBJ_ID          = 2;
    private static final int SUBJ_LIVE_FOLDER = 3;

    private DBOpenHelper    mDBHelper   = null;

    // Le matcher d'URI pour fournir une API de type ContentProvider
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SJLB.Subj.AUTHORITY, SJLB.Subj.MATCHER_ALL,          SUBJ_ALL);
        sUriMatcher.addURI(SJLB.Subj.AUTHORITY, SJLB.Subj.MATCHER_ONE,          SUBJ_ID);
        sUriMatcher.addURI(SJLB.Subj.AUTHORITY, SJLB.Subj.MATCHER_LIVE_FOLDER,  SUBJ_LIVE_FOLDER);
    }
    
    // TODO ce constructeur semble nécessaire : pour une instanciation de content provider  on dirait ?!
    public ContentProviderSubj () {
        mDBHelper = null;
    }
    
    // TODO : ce constructeur est conservé tant qu'on conserve un accès directe à cette classe
    //           (au lieu d'utiliser uniquement comme content provider)
    public ContentProviderSubj (Context aContext) {
        mDBHelper   = new DBOpenHelper(aContext, SJLB.DATABASE_NAME, null, SJLB.DATABASE_VERSION);
    }

    @Override
    public boolean onCreate() {
        // TODO : Est ce que cette bidouille sert à qqch !?
        if (null == mDBHelper)
        {
            // est ce que getContext fonctionne dans ce contexte là ?
            mDBHelper   = new DBOpenHelper(getContext(), SJLB.DATABASE_NAME, null, SJLB.DATABASE_VERSION);
        }
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // TODO SRO Auto-generated method stub
        return null;
    }

    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues arg1) {
        // TODO Auto-generated method stub
        return null;
    }

	/**
	 * Requète générique sur les messages
	 *
	 * @todo SRO : ajouter un filtrage sur un "id" donné lorsque l'utilisateur fourni une URI de type "content:path/id"
	 */
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return mDBHelper.getReadableDatabase().query(
                    SJLB.Subj.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null, // groupBy
                    null, // having
                    (null!=sortOrder)?sortOrder:SJLB.Subj.DEFAULT_SORT_ORDER
                    );
    }

    @Override
    public int update(Uri uri, ContentValues arg1, String arg2, String[] arg3) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void close () {
        mDBHelper.close();
    }
    
    /**
     * Insert un nouveau Subj complet (ne fonctionne que si Subj non déjà connu)
     * @param aSubj le Subj à insérer
     * @return true si succès
     */
    public boolean insertSubj(ForumSubject aSubj) {
      ContentValues newSubjValues = new ContentValues();
      newSubjValues.put(SJLB.Subj.ID,        aSubj.getId());
      newSubjValues.put(SJLB.Subj.CAT_ID,    aSubj.getCategoryId());
      newSubjValues.put(SJLB.Subj.GROUP_ID,  aSubj.getGroupId());
      newSubjValues.put(SJLB.Subj.LAST_DATE, aSubj.getLastDate().getTime());
      newSubjValues.put(SJLB.Subj.TEXT,      aSubj.getText());
      return mDBHelper.getWritableDatabase().insert(SJLB.Subj.TABLE_NAME, null, newSubjValues) > 0;
    }

    public boolean updateSubj(ForumSubject aSubj) {
        ContentValues newSubjValues = new ContentValues();
        newSubjValues.put(SJLB.Subj.ID,        aSubj.getId());
        newSubjValues.put(SJLB.Subj.CAT_ID,    aSubj.getCategoryId());
        newSubjValues.put(SJLB.Subj.GROUP_ID,  aSubj.getGroupId());
        newSubjValues.put(SJLB.Subj.LAST_DATE, aSubj.getLastDate().getTime());
        newSubjValues.put(SJLB.Subj.TEXT,      aSubj.getText());
        return mDBHelper.getWritableDatabase().update(SJLB.Subj.TABLE_NAME, newSubjValues, SJLB.Subj.ID + "=" + aSubj.getId(), null) > 0;
    }
        
    // vide la table des Subj
    public boolean clearSubj() {
      return mDBHelper.getWritableDatabase().delete(SJLB.Subj.TABLE_NAME, null, null) > 0;
    }
    
    // récupère un cursor avec la liste de tous les Subj
    public Cursor getAllSubj () {
        return mDBHelper.getReadableDatabase().query(   SJLB.Subj.TABLE_NAME,
                                                       new String[] {  SJLB.Subj.ID,
                                                                       SJLB.Subj.CAT_ID,
                                                                       SJLB.Subj.GROUP_ID,
                                                                       SJLB.Subj.LAST_DATE,
                                                                       SJLB.Subj.TEXT},
                                                       null, null, null, null,
                                                       SJLB.Subj.DEFAULT_SORT_ORDER);
    }
    
    // récupère un cursor sur un Subj particulier
    public Cursor getSubj (int aId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  true, SJLB.Subj.TABLE_NAME,
                                                                new String[]{   SJLB.Subj.CAT_ID,
                                                                                SJLB.Subj.GROUP_ID,
                                                                                SJLB.Subj.LAST_DATE,
                                                                                SJLB.Subj.TEXT},
                                                                SJLB.Subj.ID + "=" + aId,
                                                                null, null, null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de Subj pour l'Id " + aId);
        }
        return cursor;
    }

    // teste l'existence d'un Sujet particulier
    public Boolean isExist (int aId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  true, SJLB.Subj.TABLE_NAME,
                                                                new String[]{SJLB.Subj.ID},
                                                                SJLB.Subj.ID + "=" + aId,
                                                                null, null, null, null, null);
        return (0 < cursor.getCount());
    }

    // compte les Subj
    public long countSubj () {
        return DatabaseUtils.queryNumEntries(mDBHelper.getReadableDatabase(), SJLB.Subj.TABLE_NAME);
    }

}
