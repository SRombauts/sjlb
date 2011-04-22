package fr.srombauts.sjlb;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.util.Log;


/**
 * Encapsulation des données pour stockage en base de données
 * 
 * @author seb
 */
public class PMContentProvider extends ContentProvider {

    private static final int PM_ALL         = 1;
    private static final int PM_ID          = 2;
    private static final int PM_LIVE_FOLDER = 3;

    private SJLBDBOpenHelper    mDBHelper   = null;

    // Le matcher d'URI pour fournir une API de type ContentProvider
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SJLB.AUTHORITY, "pm",                PM_ALL);
        sUriMatcher.addURI(SJLB.AUTHORITY, "pm/#",              PM_ID);
        sUriMatcher.addURI(SJLB.AUTHORITY, "live_folders/pm",   PM_LIVE_FOLDER);
    }
    
    public PMContentProvider () {
        int test = 1;
    }
    // TODO : ce constructeur est conservé tant qu'on conserve un accès directe à cette classe
    //           (au lieu d'utiliser uniquement comme content provider)
    public PMContentProvider (Context aContext) {
        mDBHelper   = new SJLBDBOpenHelper(aContext, SJLB.DATABASE_NAME, null, SJLB.DATABASE_VERSION);
    }

    @Override
    public boolean onCreate() {
        // TODO : Est ce que cette bidouille sert à qqch !?
        if (null == mDBHelper)
        {
            // est ce que getContext fonctionne dans ce contexte là ?
            mDBHelper   = new SJLBDBOpenHelper(getContext(), SJLB.DATABASE_NAME, null, SJLB.DATABASE_VERSION);
        }
        return true;
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
            String arg4) {
        // TODO prendre en compte les paramètres pour faire la bonne requète !
        return getAllPM ();
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void close () {
        mDBHelper.close();
    }
    
    /**
     * Insert un nouveau PM juste par son ID (ne fonctionne que si PM non déjà connu)
     * @param aId l'Id du PM à insérer
     * @return true si succès
     */
    public long insertPM(int aId) {
      ContentValues newPMValues = new ContentValues();
      newPMValues.put(SJLB.PM.ID, aId);
      return mDBHelper.getWritableDatabase().insert(SJLB.PM.TABLE_NAME, null, newPMValues);
    }

    /**
     * Insert un nouveau PM complet (ne fonctionne que si PM non déjà connu)
     * @param aPM le PM à insérer
     * @return true si succès
     */
    public boolean insertPM(PrivateMessage aPM) {
      ContentValues newPMValues = new ContentValues();
      newPMValues.put(SJLB.PM.ID,        aPM.getId());
      newPMValues.put(SJLB.PM.DATE,      aPM.getDate().getTime());
      newPMValues.put(SJLB.PM.AUTHOR,    aPM.getAuthor());
      newPMValues.put(SJLB.PM.TEXT,      aPM.getText());
      return mDBHelper.getWritableDatabase().insert(SJLB.PM.TABLE_NAME, null, newPMValues) > 0;
    }

    // complète un PM à partir de son ID
    public boolean updatePM(PrivateMessage aPM) {
      ContentValues newPMValues = new ContentValues();
      newPMValues.put(SJLB.PM.DATE,      aPM.getDate().getTime());
      newPMValues.put(SJLB.PM.AUTHOR,    aPM.getAuthor());
      newPMValues.put(SJLB.PM.TEXT,      aPM.getText());
      return mDBHelper.getWritableDatabase().update(SJLB.PM.TABLE_NAME, newPMValues, SJLB.PM.ID + "=" + aPM.getId(), null) > 0;
    }
    
    // retire un PM juste par son ID
    public boolean removePM(long aId) {
      return mDBHelper.getWritableDatabase().delete(SJLB.PM.TABLE_NAME, SJLB.PM.ID + "=" + aId, null) > 0;
    }

    // vide la table des PM
    public boolean clearPM() {
      return mDBHelper.getWritableDatabase().delete(SJLB.PM.TABLE_NAME, null, null) > 0;
    }
    
    // récupère un cursor avec la liste de tous les PM
    public Cursor getAllPM () {
        return mDBHelper.getReadableDatabase().query(   SJLB.PM.TABLE_NAME,
                                                       new String[] { SJLB.PM.ID, SJLB.PM.DATE, SJLB.PM.AUTHOR, SJLB.PM.TEXT},
                                                       null, null, null, null, null);
    }
    
    // récupère un cursor sur un PM particulier
    public Cursor getPM (int aId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  true, SJLB.PM.TABLE_NAME,
                                                                new String[]{SJLB.PM.DATE, SJLB.PM.AUTHOR, SJLB.PM.TEXT},
                                                                SJLB.PM.ID + "=" + aId,
                                                                null, null, null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de PM pour l'Id " + aId);
        }
        return cursor;
    }

}
