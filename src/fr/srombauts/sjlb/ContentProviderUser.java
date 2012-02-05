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
    
    // TODO ce constructeur semble nécessaire : pour une instanciation de content provider  on dirait ?!
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
        // TODO SRO Auto-generated method stub
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

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
            String arg4) {
        // TODO prendre en compte les paramètres pour faire la bonne requète !
        return getAllUsers ();
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
     * Insert un nouveau User complet (ne fonctionne que si User non déjà connu)
     * @param aUser le User à insérer
     * @return true si succès
     */
    public boolean insertUser(User aUser) {
        ContentValues newUserValues = new ContentValues();
        newUserValues.put(SJLB.User._ID,       aUser.getId());
        newUserValues.put(SJLB.User.PSEUDO,    aUser.getPseudo());
        newUserValues.put(SJLB.User.NAME,      aUser.getNom());
        return mDBHelper.getWritableDatabase().insert(SJLB.User.TABLE_NAME, null, newUserValues) > 0;
    }

    // retire un User juste par son ID
    public boolean removeUser(long aId) {
        return mDBHelper.getWritableDatabase().delete(SJLB.User.TABLE_NAME, SJLB.User._ID + "=" + aId, null) > 0;
    }

    // vide la table des User
    public boolean clearUser() {
        return mDBHelper.getWritableDatabase().delete(SJLB.User.TABLE_NAME, null, null) > 0;
    }
    
    // récupère un cursor avec la liste de tous les User
    public Cursor getAllUsers () {
        return mDBHelper.getReadableDatabase().query(   SJLB.User.TABLE_NAME,
                                                       new String[] {   SJLB.User._ID,
                                                                        SJLB.User.PSEUDO,
                                                                        SJLB.User.NAME},
                                                       null, null, null, null, null);
    }
    
    // récupère un cursor sur un User particulier
    public Cursor getUser (int aId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  true, SJLB.User.TABLE_NAME,
                                                                new String[]{   SJLB.User.PSEUDO,
                                                                                SJLB.User.NAME},
                                                                SJLB.User._ID + "=" + aId,
                                                                null, null, null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de User pour l'Id " + aId);
        }
        return cursor;
    }

    // compte les utilisateurs
    public long countUsers () {
        // TODO SRO : comment fermer le curseur !?
        long nbUsers = DatabaseUtils.queryNumEntries(mDBHelper.getReadableDatabase(), SJLB.User.TABLE_NAME);
        mDBHelper.getReadableDatabase().close();
        return nbUsers;
    }
    
}
