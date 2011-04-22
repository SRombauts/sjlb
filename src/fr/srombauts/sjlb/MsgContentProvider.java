package fr.srombauts.sjlb;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;


/**
 * Encapsulation des données pour stockage en base de données
 * 
 * @author seb
 */
public class MsgContentProvider extends ContentProvider {

    private static final int MSG_ALL         = 1;
    private static final int MSG_ID          = 2;
    private static final int MSG_LIVE_FOLDER = 3;

    private DBOpenHelper    mDBHelper   = null;

    // Le matcher d'URI pour fournir une API de type ContentProvider
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SJLB.AUTHORITY, "msg",               MSG_ALL);
        sUriMatcher.addURI(SJLB.AUTHORITY, "msg/#",             MSG_ID);
        sUriMatcher.addURI(SJLB.AUTHORITY, "live_folders/msg",  MSG_LIVE_FOLDER);
    }
    
    // TODO : ce constructeur est conservé tant qu'on conserve un accès directe à cette classe
    //           (au lieu d'utiliser uniquement comme content provider)
    public MsgContentProvider (Context aContext) {
        mDBHelper   = new DBOpenHelper(aContext, SJLB.DATABASE_NAME, null, SJLB.DATABASE_VERSION);
    }

    @Override
    public boolean onCreate() {
        // TODO : Est ce que cette bidouille sert à qqch !?
        if (null == mDBHelper)
        {
            mDBHelper   = new DBOpenHelper(getContext(), SJLB.DATABASE_NAME, null, SJLB.DATABASE_VERSION);
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
        return getAllMsg ();
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
     * Insert un nouveau message juste par son ID (ne fonctionne que si msg non déjà connu)
     * @param aId
     * @return l'identifiant de la ligne insérée en BDD si succès
     */
    public long insertMsg(int aId) {
      ContentValues newMsgValues = new ContentValues();
      newMsgValues.put(SJLB.Msg.ID, aId);
      return mDBHelper.getWritableDatabase().insert(SJLB.Msg.TABLE_NAME, null, newMsgValues);
    }
    
    // récupère un cursor avec la liste de tous les Msg
    public Cursor getAllMsg () {
        return mDBHelper.getReadableDatabase().query(   SJLB.Msg.TABLE_NAME,
                                                       new String[] { SJLB.Msg.ID, SJLB.Msg.DATE, SJLB.Msg.AUTHOR, SJLB.Msg.DATE},
                                                       null, null, null, null, null);
    }
    
    // récupère un cursor sur un Msg particulier
    public Cursor getMsg (int aId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  true, SJLB.Msg.TABLE_NAME,
                                                                new String[]{SJLB.Msg.DATE, SJLB.Msg.AUTHOR, SJLB.Msg.TEXT},
                                                                SJLB.Msg.ID + "=" + aId,
                                                                null, null, null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de Msg pour l'Id " + aId);
        }
        return cursor;
    }
   
    // vide la table des PM
    public boolean clearMsg() {
      return mDBHelper.getWritableDatabase().delete(SJLB.Msg.TABLE_NAME, null, null) > 0;
    }
    
}
