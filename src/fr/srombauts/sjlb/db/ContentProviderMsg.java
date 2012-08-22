package fr.srombauts.sjlb.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import fr.srombauts.sjlb.model.ForumMessage;


/**
 * Encapsulation des données pour stockage en base de données
 * 
 * @author SRombauts
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
    
    // Un constructeur public par défaut est nécessaire dès lorsque que le provider est déclaré dans le Manifeste
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
        // Auto-generated method stub
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
     * Requête générique sur les messages
     *
     * @todo SRombauts : ajouter un filtrage sur un "id" donné lorsque l'utilisateur fourni une URI de type "content:path/id"
     */
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return mDBHelper.getReadableDatabase().query(
                    SJLB.Msg.TABLE_NAME,
                    projection, selection, selectionArgs,
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
      newMsgValues.put(SJLB.Msg.DATE_EDIT, aMsg.getDateEdit().getTime());
      newMsgValues.put(SJLB.Msg.AUTHOR_ID, aMsg.getAuthorId());
      newMsgValues.put(SJLB.Msg.SUBJECT_ID,aMsg.getSubjectId());
      newMsgValues.put(SJLB.Msg.UNREAD,    aMsg.isUnread());
      newMsgValues.put(SJLB.Msg.TEXT,      aMsg.getText());
      return mDBHelper.getWritableDatabase().insert(SJLB.Msg.TABLE_NAME, null, newMsgValues) > 0;
    }

    // Met à jour un Msg qui a été édité (ne fonctionne que si Msg déjà connu)
    public boolean updateMsg(ForumMessage aMsg) {
      ContentValues newMsgValues = new ContentValues();
      newMsgValues.put(SJLB.Msg._ID,       aMsg.getId());
      newMsgValues.put(SJLB.Msg.DATE,      aMsg.getDate().getTime());
      newMsgValues.put(SJLB.Msg.DATE_EDIT, aMsg.getDateEdit().getTime());
      newMsgValues.put(SJLB.Msg.AUTHOR_ID, aMsg.getAuthorId());
      newMsgValues.put(SJLB.Msg.SUBJECT_ID,aMsg.getSubjectId());
      newMsgValues.put(SJLB.Msg.UNREAD,    aMsg.isUnread());
      newMsgValues.put(SJLB.Msg.TEXT,      aMsg.getText());
      final String   selection      = SJLB.Msg._ID + "=?";
      final String[] selectionArgs  = {Long.toString (aMsg.getId())};
      return mDBHelper.getWritableDatabase().update(SJLB.Msg.TABLE_NAME, newMsgValues, selection, selectionArgs) > 0;
    }
    
    // Récupère la date du premier (plus vieux) message
    public long getDateFirstMsg () {
        long dateFirstMsgSecondes = 0;
        final String[] columns  = {"min(" + SJLB.Msg.DATE + ")"};
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.Msg.TABLE_NAME,
                                                                columns,
                                                                null, null, // selection, selectionArgs
                                                                null, null, null);
        if (1 == cursor.getCount())
        {
            cursor.moveToFirst();
            dateFirstMsgSecondes = cursor.getLong(0)/1000;
        }
        cursor.close();
        return dateFirstMsgSecondes;
    }
    
    // Récupère la date du dernier (plus récent) message, ou la date d'édition la plus récente le cas échéant
    public long getDateLastMsg () {
        long dateLastMsgSecondes = 0;
        final String[] columns  = {"max(" + SJLB.Msg.DATE + ")", "max(" + SJLB.Msg.DATE_EDIT + ")"};
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.Msg.TABLE_NAME,
                                                                columns,
                                                                null, null, // selection, selectionArgs
                                                                null, null, null);
        if (1 == cursor.getCount())
        {
            cursor.moveToFirst();
            // Récupère la date de création ou d'édition la plus récente
            dateLastMsgSecondes = Math.max(cursor.getLong(0), cursor.getLong(1))/1000;
        }
        cursor.close();
        return dateLastMsgSecondes;
    }
    
    // récupère un cursor avec la liste des Msg marqués UNREAD_LOCALY non lus mais localement lus 
    public Cursor getMsgUnreadLocaly () {
        final String[] columns  = {SJLB.Msg._ID};
        final String   selection= SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_LOCALY;
        return mDBHelper.getReadableDatabase().query(   SJLB.Msg.TABLE_NAME,
                                                        columns,
                                                        selection, null,
                                                        null, null, null);
    }
    
    // Établit la liste des messages lus localement
    public String getListMsgUnreadLocaly() {
        String listMsgUnread = "";
        Cursor cursor = getMsgUnreadLocaly ();
        int    nbMsgLus = cursor.getCount ();
        if (0 < nbMsgLus) {
            if (cursor.moveToFirst ()) {
                do {
                    if (listMsgUnread != "") {
                        listMsgUnread += ",";
                    }
                    listMsgUnread += cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.Msg._ID));
                } while (cursor.moveToNext ());
            }
        }
        cursor.close ();
        return listMsgUnread;
    }
    
    // Efface les flags UNREAD_LOCALY des messages lus localement
    public int clearMsgUnread () {
        ContentValues values = new ContentValues();
        values.put(SJLB.Msg.UNREAD, SJLB.Msg.UNREAD_FALSE); // on les passe à UNREAD_FALSE ce qui indique qu'ils ont été déclarés lus au site Web SJLB
        String where = SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_LOCALY;
        return mDBHelper.getWritableDatabase ().update(SJLB.Msg.TABLE_NAME, values, where, null);
    }

    // teste l'existence d'un Msg particulier
    public Boolean isExist (int aId) {
        final String   sql          = "SELECT 1 FROM " + SJLB.Msg.TABLE_NAME + " WHERE " + SJLB.Msg._ID + "=?";
        final String[] selectionArgs= {Integer.toString (aId)};
        Cursor cursor = mDBHelper.getReadableDatabase().rawQuery(sql, selectionArgs);
        boolean bIsExist = (0 < cursor.getCount());
        cursor.close ();
        return bIsExist;
    }
       
    // vide la table des Msg
    public boolean clearMsg() {
      return mDBHelper.getWritableDatabase().delete(SJLB.Msg.TABLE_NAME, null, null) > 0;
    }

    // compte tous les messages
    public long getCount () {
        long nbMsgs = DatabaseUtils.queryNumEntries(mDBHelper.getReadableDatabase(), SJLB.Msg.TABLE_NAME);
        mDBHelper.getReadableDatabase().close();
        return nbMsgs;
    }

    // compte les messages non lus d'un sujet donné
    public int getNbUnread (int aSubjectId) {
        final String[] columns      = {SJLB.Msg._ID};
        final String   selection    = "(" + SJLB.Msg.SUBJECT_ID + "=?" + " AND "
                                          + SJLB.Msg.UNREAD + "=" + SJLB.Msg.UNREAD_TRUE + ")";
        final String[] selectionArgs= {Integer.toString(aSubjectId)};
        Cursor cursor = mDBHelper.getReadableDatabase().query(  SJLB.Msg.TABLE_NAME,
                                                                columns,
                                                                selection,
                                                                selectionArgs,
                                                                null, null, null);
        int nbMsgs = cursor.getCount();
        cursor.close ();
        return nbMsgs;
    }

}
