package fr.srombauts.sjlb.db;

import fr.srombauts.sjlb.model.ForumSubject;
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
 * @author SRombauts
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
    
    // Un constructeur public par défaut est nécessaire dès lorsque que le provider est déclaré dans le Manifeste
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
        // SRombauts Auto-generated method stub
        return null;
    }

    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        // Auto-generated method stub
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues arg1) {
        // Auto-generated method stub
        return null;
    }

    /**
     * Requête générique sur les sujets
     *
     * @todo SRombauts : ajouter un filtrage sur un "id" donné lorsque l'utilisateur fourni une URI de type "content:path/id"
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
    public int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
        return mDBHelper.getWritableDatabase().update(SJLB.Subj.TABLE_NAME, values, where, selectionArgs);
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
      newSubjValues.put(SJLB.Subj._ID,       aSubj.getId());
      newSubjValues.put(SJLB.Subj.CAT_ID,    aSubj.getCategoryId());
      newSubjValues.put(SJLB.Subj.GROUP_ID,  aSubj.getGroupId());
      newSubjValues.put(SJLB.Subj.LAST_DATE, aSubj.getLastDate().getTime());
      newSubjValues.put(SJLB.Subj.TEXT,      aSubj.getText());
      newSubjValues.put(SJLB.Subj.NB_UNREAD, 0);
      return mDBHelper.getWritableDatabase().insert(SJLB.Subj.TABLE_NAME, null, newSubjValues) > 0;
    }

    public boolean updateSubj(ForumSubject aSubj) {
        ContentValues newSubjValues = new ContentValues();
        newSubjValues.put(SJLB.Subj._ID,       aSubj.getId());
        newSubjValues.put(SJLB.Subj.CAT_ID,    aSubj.getCategoryId());
        newSubjValues.put(SJLB.Subj.GROUP_ID,  aSubj.getGroupId());
        newSubjValues.put(SJLB.Subj.LAST_DATE, aSubj.getLastDate().getTime());
        newSubjValues.put(SJLB.Subj.TEXT,      aSubj.getText());
        final String[] selectionArgs = {Long.toString (aSubj.getId())};
        return mDBHelper.getWritableDatabase().update(SJLB.Subj.TABLE_NAME, newSubjValues, SJLB.Subj._ID + "=?", selectionArgs) > 0;
    }
        
    public boolean updateNbUnread(int aId, int aNbUnread) {
        ContentValues newSubjValues = new ContentValues();
        newSubjValues.put(SJLB.Subj.NB_UNREAD, aNbUnread);
        final String[] selectionArgs = {Long.toString (aId)};
        return mDBHelper.getWritableDatabase().update(SJLB.Subj.TABLE_NAME, newSubjValues, SJLB.Subj._ID + "=?", selectionArgs) > 0;
    }
        
    // vide la table des Subj
    public boolean clearSubj() {
      return mDBHelper.getWritableDatabase().delete(SJLB.Subj.TABLE_NAME, null, null) > 0;
    }
    
    // récupère un cursor sur un Subj particulier
    public Cursor getSubj (int aId) {
        final String[] columns      = {SJLB.Subj.CAT_ID, SJLB.Subj.GROUP_ID, SJLB.Subj.LAST_DATE, SJLB.Subj.TEXT};
        final String   selection    = SJLB.Subj._ID + "=?";
        final String[] selectionArgs= {Integer.toString(aId)};
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.Subj.TABLE_NAME,
                                                                columns, selection, selectionArgs,
                                                                null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de Subj pour l'Id " + aId);
        }
        return cursor;
    }

    // teste l'existence d'un Sujet particulier
    public Boolean isExist (int aId) {
        final String[] columns      = {SJLB.Subj._ID};
        final String   selection    = SJLB.Subj._ID + "=?";
        final String[] selectionArgs= {Integer.toString(aId)};
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.Subj.TABLE_NAME,
                                                                columns, selection, selectionArgs,
                                                                null, null, null);
        boolean bIsExist = (0 < cursor.getCount());
        cursor.close ();
        return bIsExist;
    }

    // compte les Subj
    public long countSubj () {
        return DatabaseUtils.queryNumEntries(mDBHelper.getReadableDatabase(), SJLB.Subj.TABLE_NAME);
    }

}
