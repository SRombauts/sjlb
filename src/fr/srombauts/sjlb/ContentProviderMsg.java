package fr.srombauts.sjlb;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;


/**
 * Encapsulation des données pour stockage en base de données
 * 
 * @author seb
 */
public class ContentProviderMsg extends ContentProvider {
    private static final int MSG_ALL         = 1;
    private static final int MSG_ID          = 2;
    private static final int MSG_LIVE_FOLDER = 3;

    private DBOpenHelper    mDBHelper   = null;

    // Le matcher d'URI pour fournir une API de type ContentProvider
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SJLB.Msg.AUTHORITY, SJLB.Msg.MATCHER_ALL,            MSG_ALL);
        sUriMatcher.addURI(SJLB.Msg.AUTHORITY, SJLB.Msg.MATCHER_ONE,            MSG_ID);
        sUriMatcher.addURI(SJLB.Msg.AUTHORITY, SJLB.Msg.MATCHER_LIVE_FOLDER,    MSG_LIVE_FOLDER);
    }
    
    // TODO ce constructeur semble nécessaire : pour une instanciation de content provider  on dirait ?!
    public ContentProviderMsg () {
        mDBHelper = null;
    }
    
    // TODO : ce constructeur est conservé tant qu'on conserve un accès directe à cette classe
    //           (au lieu d'utiliser uniquement comme content provider)
    public ContentProviderMsg (Context aContext) {
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
        // TODO SRO : virer cette complexité en ne croisant plus les deux tables ?
        if (null == selection) selection = ""; 
        String selectionCplx = "(" + selection + ") AND (" + SJLB.Msg.TABLE_NAME+"."+SJLB.Msg.AUTHOR_ID+"="+SJLB.User.TABLE_NAME+"."+SJLB.User._ID + ")"; 
        //Log.e ("ContentProvider", selectionCplx);
        return mDBHelper.getReadableDatabase().query(
                    SJLB.Msg.TABLE_NAME + ", " + SJLB.User.TABLE_NAME,
                    projection,
                    selectionCplx,
                    selectionArgs,
                    null, // groupBy
                    null, // having
                    (null!=sortOrder)?sortOrder:SJLB.Msg.DEFAULT_SORT_ORDER
                    );
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
        return mDBHelper.getWritableDatabase().update(SJLB.Msg.TABLE_NAME, values, where, selectionArgs);
    }
    
    public void close () {
        mDBHelper.close();
    }
    
    /**
     * Insert un nouveau Msg complet (ne fonctionne que si Msg précédemment inconnu)
     * @param aMsg le Msg à insérer
     * @return true si succès
     */
    public boolean insertMsg(ForumMessage aMsg) {
      ContentValues newMsgValues = new ContentValues();
      newMsgValues.put(SJLB.Msg._ID,       aMsg.getId());
      newMsgValues.put(SJLB.Msg.DATE,      aMsg.getDate().getTime());
      newMsgValues.put(SJLB.Msg.AUTHOR_ID, aMsg.getAuthorId());
      newMsgValues.put(SJLB.Msg.SUBJECT_ID,aMsg.getSubjectId());
      newMsgValues.put(SJLB.Msg.UNREAD,    aMsg.isUnread());
      newMsgValues.put(SJLB.Msg.TEXT,      aMsg.getText());
      return mDBHelper.getWritableDatabase().insert(SJLB.Msg.TABLE_NAME, null, newMsgValues) > 0;
    }

    // Met à jout un Msg qui a été édité (ne fonctionne que si Msg déjà connu)
    public boolean updateMsg(ForumMessage aMsg) {
      ContentValues newMsgValues = new ContentValues();
      newMsgValues.put(SJLB.Msg._ID,       aMsg.getId());
      newMsgValues.put(SJLB.Msg.DATE,      aMsg.getDate().getTime());
      newMsgValues.put(SJLB.Msg.AUTHOR_ID, aMsg.getAuthorId());
      newMsgValues.put(SJLB.Msg.SUBJECT_ID,aMsg.getSubjectId());
      newMsgValues.put(SJLB.Msg.UNREAD,    aMsg.isUnread());
      newMsgValues.put(SJLB.Msg.TEXT,      aMsg.getText());
      return mDBHelper.getWritableDatabase().update(SJLB.Msg.TABLE_NAME, newMsgValues, SJLB.Msg._ID + "=" + aMsg.getId(), null) > 0;
    }
    
    // Récupère la date du dernier message
    public long getLastMsgDate () {
        long                nbSeconds = 0;
        Cursor cursor = getAllMsg ();
        if (0 < cursor.getCount())
        {
            cursor.moveToLast();
            nbSeconds = cursor.getLong(cursor.getColumnIndex(SJLB.Msg.DATE))/1000;
        }
        cursor.close();
        return nbSeconds;
    }
    
    // récupère un cursor avec la liste de tous les Msg
    public Cursor getAllMsg () {
        return mDBHelper.getReadableDatabase().query(   SJLB.Msg.TABLE_NAME,
                                                       new String[] {   SJLB.Msg._ID,
                                                                        SJLB.Msg.DATE,
                                                                        SJLB.Msg.AUTHOR_ID,
                                                                        SJLB.Msg.SUBJECT_ID,
                                                                        SJLB.Msg.UNREAD,
                                                                        SJLB.Msg.TEXT},
                                                       null,
                                                       null, null, null, null);
        // TODO SRO : mDBHelper.close(); ?
    }

    // récupère un cursor avec la liste des Msg marqués UNREAD_LOCALY non lus mais localement lus 
    public Cursor getMsgUnread () {
        return mDBHelper.getReadableDatabase().query(   SJLB.Msg.TABLE_NAME,
                                                       new String[] { SJLB.Msg._ID },
                                                       SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_LOCALY,
                                                       null, null, null, null);
    }
    
    // Efface les flags UNREAD_LOCALY des messages lus localement
    public int clearMsgUnread () {
        ContentValues values = new ContentValues();
        values.put(SJLB.Msg.UNREAD, SJLB.Msg.UNREAD_FALSE); // on les passe à UNREAD_LOCALY ce qui indique qu'il faut encore signaler le site Web SJLB du fait qu'on les a lu !
        String where = SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_LOCALY;
        return mDBHelper.getWritableDatabase ().update(SJLB.Msg.TABLE_NAME, values, where, null);
    }

/*    
    // récupère un cursor sur un Msg particulier
    public Cursor getMsg (int aId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.Msg.TABLE_NAME,
                                                                new String[]{   SJLB.Msg.DATE,
                                                                                SJLB.Msg.AUTHOR_ID,
                                                                                SJLB.Msg.SUBJECT_ID,
                                                                                SJLB.Msg.UNREAD,
                                                                                SJLB.Msg.TEXT},
                                                                SJLB.Msg._ID + "=" + aId,
                                                                null, null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de Msg pour l'Id " + aId);
        }
        return cursor;
    }
*/

    // teste l'existence d'un Msg particulier
    public Boolean isExist (int aId) {
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.Msg.TABLE_NAME,
                                                                new String[]{SJLB.Msg._ID},
                                                                SJLB.Msg._ID + "=" + aId,
                                                                null, null, null, null, null);
        boolean bIsExist = (0 < cursor.getCount());
        cursor.close ();
        return bIsExist;
    }
       
    // vide la table des Msg
    public boolean clearMsg() {
      return mDBHelper.getWritableDatabase().delete(SJLB.Msg.TABLE_NAME, null, null) > 0;
    }
    
}
