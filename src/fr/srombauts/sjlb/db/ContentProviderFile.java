package fr.srombauts.sjlb.db;

import fr.srombauts.sjlb.model.AttachedFile;
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
public class ContentProviderFile extends ContentProvider {

    private static final int FILE_ALL         = 1;
    private static final int FILE_ID          = 2;
    private static final int FILE_LIVE_FOLDER = 3;

    private DBOpenHelper    mDBHelper   = null;

    // Le matcher d'URI pour fournir une API de type ContentProvider
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SJLB.File.AUTHORITY, SJLB.File.MATCHER_ALL,            FILE_ALL);
        sUriMatcher.addURI(SJLB.File.AUTHORITY, SJLB.File.MATCHER_ONE,            FILE_ID);
        sUriMatcher.addURI(SJLB.File.AUTHORITY, SJLB.File.MATCHER_LIVE_FOLDER,    FILE_LIVE_FOLDER);
    }
    
    // Un constructeur public par défaut est nécessaire dès lorsque que le provider est déclaré dans le Manifeste
    public ContentProviderFile () {
        mDBHelper = null;
    }
    
    // TODO : ce constructeur est conservé tant qu'on conserve un accès directe à cette classe
    //           (au lieu d'utiliser uniquement comme content provider)
    public ContentProviderFile (Context aContext) {
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
        // TODO SRombauts Auto-generated method stub
        return null;
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Requète générique sur les sujets
     *
     * @todo SRombauts : ajouter un filtrage sur un "id" donné lorsque l'utilisateur fourni une URI de type "content:path/id"
     */
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return mDBHelper.getReadableDatabase().query(
                    SJLB.File.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null, // groupBy
                    null, // having
                    (null!=sortOrder)?sortOrder:SJLB.File.DEFAULT_SORT_ORDER
                    );
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
     * Insert un nouveau fichier attaché complet (ne fonctionne que si fichier non déjà connu)
     * @param aAttachedFile le fichier à insérer
     * @return true si succès
     */
    public boolean insertFile(AttachedFile aFile) {
      ContentValues newFileValues = new ContentValues();
      newFileValues.put(SJLB.File.MSG_ID,    aFile.getMessageId());
      newFileValues.put(SJLB.File.FILENAME,  aFile.getFilename());
      return mDBHelper.getWritableDatabase().insert(SJLB.File.TABLE_NAME, null, newFileValues) > 0;
    }

    // retire tous les fichiers attachés d'un message, par son ID
    public boolean removeFiles(long aMessageId) {
      return mDBHelper.getWritableDatabase().delete(SJLB.File.TABLE_NAME, SJLB.File.MSG_ID + "=" + aMessageId, null) > 0;
    }

    // vide la table des fichiers attachés
    public boolean clearFiles() {
      return mDBHelper.getWritableDatabase().delete(SJLB.File.TABLE_NAME, null, null) > 0;
    }
    
    // récupère un cursor avec la liste de tous les fichiers attachés
    public Cursor getAllFiles () {
        return mDBHelper.getReadableDatabase().query(   SJLB.File.TABLE_NAME,
                                                       new String[] {   SJLB.File.MSG_ID,
                                                                        SJLB.File.FILENAME},
                                                       null, null, null, null, null);
    }
    
    // récupère un cursor sur les fichiers attachés d'un message en particulier
    public Cursor getFiles (int aMessageId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  true, SJLB.File.TABLE_NAME,
                                                                new String[]{   SJLB.File.MSG_ID,
                                                                                SJLB.File.FILENAME},
                                                                SJLB.File.MSG_ID + "=" + aMessageId,
                                                                null, null, null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de fichier attaché pour le message d'Id " + aMessageId);
        }
        return cursor;
    }

    // teste l'existence d'au moins un fichier pour un message particulier
    public Boolean isExist (int aMessageId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  true, SJLB.File.TABLE_NAME,
                                                                new String[]{SJLB.File.FILENAME},
                                                                SJLB.File.MSG_ID + "=" + aMessageId,
                                                                null, null, null, null, null);
        boolean bIsExist = (0 < cursor.getCount());
        cursor.close ();
        return bIsExist;
    }
    
    // compte les fichiers attachés
    public long countFiles () {
        return DatabaseUtils.queryNumEntries(mDBHelper.getReadableDatabase(), SJLB.File.TABLE_NAME);
    }
    
}
