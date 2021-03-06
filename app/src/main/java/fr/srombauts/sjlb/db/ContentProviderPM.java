package fr.srombauts.sjlb.db;

import fr.srombauts.sjlb.model.PrivateMessage;
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
public class ContentProviderPM extends ContentProvider {

    private static final int PM_ALL         = 1;
    private static final int PM_ID          = 2;
    private static final int PM_LIVE_FOLDER = 3;

    private DBOpenHelper    mDBHelper   = null;

    // Le matcher d'URI pour fournir une API de type ContentProvider
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SJLB.PM.AUTHORITY, SJLB.PM.MATCHER_ALL,            PM_ALL);
        sUriMatcher.addURI(SJLB.PM.AUTHORITY, SJLB.PM.MATCHER_ONE,            PM_ID);
        sUriMatcher.addURI(SJLB.PM.AUTHORITY, SJLB.PM.MATCHER_LIVE_FOLDER,    PM_LIVE_FOLDER);
    }
    
    // Un constructeur public par défaut est nécessaire dès lorsque que le provider est déclaré dans le Manifeste
    public ContentProviderPM () {
        mDBHelper = null;
    }
    
    // TODO : ce constructeur est conservé tant qu'on conserve un accès directe à cette classe
    //           (au lieu d'utiliser uniquement comme content provider)
    public ContentProviderPM (Context aContext) {
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
    public String getType(Uri arg0) {
        // SRombauts Auto-generated method stub
        return null;
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // Auto-generated method stub
        return 0;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        // Auto-generated method stub
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return mDBHelper.getReadableDatabase().query(
                SJLB.PM.TABLE_NAME,
                projection, selection, selectionArgs,
                null, // groupBy
                null, // having
                (null!=sortOrder)?sortOrder:SJLB.PM.DEFAULT_SORT_ORDER
                );
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }
    
    public void close () {
        mDBHelper.close();
    }
    
    /**
     * Insert un nouveau PM complet (ne fonctionne que si PM non déjà connu)
     * @param aPM le PM à insérer
     * @return true si succès
     */
    public boolean insertPM(PrivateMessage aPM) {
        ContentValues newPMValues = new ContentValues();
        newPMValues.put(SJLB.PM._ID,       aPM.getId());
        newPMValues.put(SJLB.PM.DATE,      aPM.getDate().getTime());
        newPMValues.put(SJLB.PM.AUTHOR_ID, aPM.getAuthorId());
        newPMValues.put(SJLB.PM.DEST_ID,   aPM.getDestId());
        newPMValues.put(SJLB.PM.TEXT,      aPM.getText());
        return (0 < mDBHelper.getWritableDatabase().insert(SJLB.PM.TABLE_NAME, null, newPMValues));
    }

    // supprime un PM particulier
    public boolean delete(int aId) {
        final String    whereClause = SJLB.PM._ID + "=?";
        final String[]  whereArgs   = {Integer.toString(aId)};
        return mDBHelper.getWritableDatabase().delete(SJLB.PM.TABLE_NAME, whereClause, whereArgs) > 0;
    }

    // vide la table des PM
    public boolean clearPM() {
        return (0 < mDBHelper.getWritableDatabase().delete(SJLB.PM.TABLE_NAME, null, null));
    }
    
    // récupère un cursor sur un PM particulier
    public Cursor getPM (int aId) {
        final String[] columns      = {SJLB.PM.DATE, SJLB.PM.AUTHOR_ID, SJLB.PM.DEST_ID, SJLB.PM.TEXT};
        final String   selection    = SJLB.PM._ID + "=?";
        final String[] selectionArgs= {Integer.toString(aId)};
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.PM.TABLE_NAME,
                                                                columns, selection, selectionArgs,
                                                                null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de PM pour l'Id " + aId);
        }
        return cursor;
    }

    // teste l'existence d'un PM particulier
    public Boolean isExist (int aId) {
        final String   sql          = "SELECT 1 FROM " + SJLB.PM.TABLE_NAME + " WHERE " + SJLB.PM._ID + "=?";
        final String[] selectionArgs= {Integer.toString(aId)};
        Cursor cursor = mDBHelper.getReadableDatabase().rawQuery(sql, selectionArgs);
        boolean bIsExist = (0 < cursor.getCount());
        cursor.close ();
        return bIsExist;
    }
    
    // compte les PM
    public long countPM () {
        return DatabaseUtils.queryNumEntries(mDBHelper.getReadableDatabase(), SJLB.PM.TABLE_NAME);
    }

    // Récupère l'ID du dernier (plus récent) PM (plus grand id)
    public long getIdLastPM () {
        long idLastPM = 0;
        final String[] columns  = {"max(" + SJLB.PM._ID + ")"};
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.PM.TABLE_NAME,
                                                                columns,
                                                                null, null, // selection, selectionArgs
                                                                null, null, null);
        if (1 == cursor.getCount())
        {
            cursor.moveToFirst();
            idLastPM = cursor.getLong(0);
        }
        cursor.close();
        return idLastPM;
    }
}
