package fr.srombauts.sjlb.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import fr.srombauts.sjlb.model.User;


/**
 * Encapsulation des données pour stockage en base de données
 * 
 * @author SRombauts
 */
public class ContentProviderUser extends ContentProvider {

    private static final int USER_ALL         = 1;
    private static final int USER_ID          = 2;
    private static final int USER_LIVE_FOLDER = 3;

    private DBOpenHelper    mDBHelper   = null;

    // Le matcher d'URI pour fournir une API de type ContentProvider
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SJLB.User.AUTHORITY, SJLB.User.MATCHER_ALL,           USER_ALL);
        sUriMatcher.addURI(SJLB.User.AUTHORITY, SJLB.User.MATCHER_ONE,           USER_ID);
        sUriMatcher.addURI(SJLB.User.AUTHORITY, SJLB.User.MATCHER_LIVE_FOLDER,   USER_LIVE_FOLDER);
    }
    
    // Un constructeur public par défaut est nécessaire dès lorsque que le provider est déclaré dans le Manifeste
    public ContentProviderUser () {
        mDBHelper = null;
    }
    
    // TODO : ce constructeur est conservé tant qu'on conserve un accès directe à cette classe
    //           (au lieu d'utiliser uniquement comme content provider)
    public ContentProviderUser (Context aContext) {
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
        // Auto-generated method stub
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
                SJLB.User.TABLE_NAME,
                projection, selection, selectionArgs,
                null, // groupBy
                null, // having
                (null!=sortOrder)?sortOrder:SJLB.User.DEFAULT_SORT_ORDER
                );
    }
    
    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        // Auto-generated method stub
        return 0;
    }
    
    public void close () {
        mDBHelper.close();
    }
    
    /**
     * Insert un nouveau User complet (ne fonctionne que si User non déjà connu)
     * @param aUser le User à insérer
     * @return true si succès
     */
    public boolean insertUser(User aUser) {
        ContentValues newUserValues = new ContentValues();
        newUserValues.put(SJLB.User._ID,       aUser.getId());
        newUserValues.put(SJLB.User.PSEUDO,    aUser.getPseudo());
        newUserValues.put(SJLB.User.NAME,      aUser.getName());
        newUserValues.put(SJLB.User.ADDRESS,   aUser.getAddress());
        newUserValues.put(SJLB.User.NOTES,     aUser.getNotes());
        newUserValues.put(SJLB.User.DATE_MAJ,  aUser.getDateMaj().getTime());
        return mDBHelper.getWritableDatabase().insert(SJLB.User.TABLE_NAME, null, newUserValues) > 0;
    }

    /**
     * Met à jour un User complet (ne fonctionne que si User déjà connu)
     * @param aUser le User à mettre à jour
     * @return true si succès
     */
    public boolean updateUser(User aUser) {
        ContentValues newUserValues = new ContentValues();
        newUserValues.put(SJLB.User._ID,       aUser.getId());
        newUserValues.put(SJLB.User.PSEUDO,    aUser.getPseudo());
        newUserValues.put(SJLB.User.NAME,      aUser.getName());
        newUserValues.put(SJLB.User.ADDRESS,   aUser.getAddress());
        newUserValues.put(SJLB.User.NOTES,     aUser.getNotes());
        newUserValues.put(SJLB.User.DATE_MAJ,  aUser.getDateMaj().getTime());
        final String   selection      = SJLB.User._ID + "=?";
        final String[] selectionArgs  = {Long.toString (aUser.getId())};
        return mDBHelper.getWritableDatabase().update(SJLB.User.TABLE_NAME, newUserValues, selection, selectionArgs) > 0;
    }
    
    // retire un User juste par son ID
    public boolean removeUser(long aId) {
        return mDBHelper.getWritableDatabase().delete(SJLB.User.TABLE_NAME, SJLB.User._ID + "=?", null) > 0;
    }

    // vide la table des User
    public boolean clearUser() {
        return mDBHelper.getWritableDatabase().delete(SJLB.User.TABLE_NAME, null, null) > 0;
    }
        
    // teste l'existence d'un User particulier
    public Boolean isExist (int aId) {
        final String   sql          = "SELECT 1 FROM " + SJLB.User.TABLE_NAME + " WHERE " + SJLB.User._ID + "=?";
        final String[] selectionArgs= {Integer.toString (aId)};
        Cursor cursor = mDBHelper.getReadableDatabase().rawQuery(sql, selectionArgs);
        boolean bIsExist = (0 < cursor.getCount());
        cursor.close ();
        return bIsExist;
    }
           
    // compte les utilisateurs
    public long countUsers () {
        long nbUsers = DatabaseUtils.queryNumEntries(mDBHelper.getReadableDatabase(), SJLB.User.TABLE_NAME);
        mDBHelper.getReadableDatabase().close();
        return nbUsers;
    }

    // Récupère la date de la dernière (plus récente) mise à jour des infos d'un utilisateur
    public long getDateLastUpdateUser () {
        long dateLastMsgSecondes = 0;
        final String[] columns  = {"max(" + SJLB.User.DATE_MAJ + ")"};
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.User.TABLE_NAME,
                                                                columns,
                                                                null, null, // selection, selectionArgs
                                                                null, null, null);
        if (1 == cursor.getCount())
        {
            cursor.moveToFirst();
            dateLastMsgSecondes = cursor.getLong(0)/1000;
        }
        cursor.close();
        return dateLastMsgSecondes;
    }
}
