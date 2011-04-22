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
public class SJLBContentProvider extends ContentProvider {
    private static final String DATABASE_NAME       = "SJLB.db";
    private static final int    DATABASE_VERSION    = 1;
    
    private static final String DATABASE_PM_TABLE   = "private_messages";

    private static final String DATABASE_MSG_TABLE  = "forum_messages";

    private static final int PM_ALL         = 1;
    private static final int PM_ID          = 2;
    private static final int PM_LIVE_FOLDER = 3;

    private SQLiteDatabase      mDatabase;
    private SJLBDBOpenHelper    mDBHelper;

    // Le matcher d'URI pour fournir une API de type ContentProvider
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SJLB.AUTHORITY, "pm",                PM_ALL);
        sUriMatcher.addURI(SJLB.AUTHORITY, "pm/#",              PM_ID);
        sUriMatcher.addURI(SJLB.AUTHORITY, "live_folders/pm",   PM_LIVE_FOLDER);
    }
    
    public SJLBContentProvider () {
        mDBHelper   = new SJLBDBOpenHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        return false;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    // Tente d'ouvrir la base de données
    public void open () throws SQLiteException {
        try {
            mDatabase = mDBHelper.getWritableDatabase();
        } catch (SQLiteException ex) {
            mDatabase = mDBHelper.getReadableDatabase();
        }
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
      return mDatabase.insert(DATABASE_PM_TABLE, null, newPMValues);
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
      return mDatabase.insert(DATABASE_PM_TABLE, null, newPMValues) > 0;
    }

    // complète un PM à partir de son ID
    public boolean updatePM(PrivateMessage aPM) {
      ContentValues newPMValues = new ContentValues();
      newPMValues.put(SJLB.PM.DATE,      aPM.getDate().getTime());
      newPMValues.put(SJLB.PM.AUTHOR,    aPM.getAuthor());
      newPMValues.put(SJLB.PM.TEXT,      aPM.getText());
      return mDatabase.update(DATABASE_PM_TABLE, newPMValues, SJLB.PM.ID + "=" + aPM.getId(), null) > 0;
    }
    
    // retire un PM juste par son ID
    public boolean removePM(long aId) {
      return mDatabase.delete(DATABASE_PM_TABLE, SJLB.PM.ID + "=" + aId, null) > 0;
    }

    // vide la table des PM
    public boolean clearPM() {
      return mDatabase.delete(DATABASE_PM_TABLE, null, null) > 0;
    }
    
    // récupère un cursor avec la liste de tous les PM
    public Cursor getAllPM () {
        return mDatabase.query(DATABASE_PM_TABLE,
                               new String[] { SJLB.PM.ID, SJLB.PM.DATE, SJLB.PM.AUTHOR, SJLB.PM.DATE},
                               null, null, null, null, null);
    }
    
    // récupère un cursor sur un PM particulier
    public Cursor getPM (int aId) {
        Cursor cursor = mDatabase.query(true, DATABASE_PM_TABLE,
                                        new String[]{SJLB.PM.DATE, SJLB.PM.AUTHOR, SJLB.PM.TEXT},
                                        SJLB.PM.ID + "=" + aId,
                                        null, null, null, null, null);
        if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
            throw new SQLException("Pas de PM pour l'Id " + aId);
        }
        return cursor;
    }

    /**
     * Insert un nouveau message juste par son ID (ne fonctionne que si msg non déjà connu)
     * @param aId
     * @return l'identifiant de la ligne insérée en BDD si succès
     */
    public long insertMsg(int aId) {
      ContentValues newMsgValues = new ContentValues();
      newMsgValues.put(SJLB.Msg.ID, aId);
      return mDatabase.insert(DATABASE_MSG_TABLE, null, newMsgValues);
    }
    
    
    /**
     * Gestionnaire de création/mise à jour de la base de données SQLite
     * 
     * @author seb
     */
    public class SJLBDBOpenHelper extends SQLiteOpenHelper {

        private static final String DATABASE_PM_CREATE = "create table " + DATABASE_PM_TABLE + " ("
                                                       + SJLB.PM.ID + " integer primary key, "
                                                       + SJLB.PM.DATE + " long, "
                                                       + SJLB.PM.AUTHOR + " text, "
                                                       + SJLB.PM.TEXT + " text);";
        
        private static final String DATABASE_MSG_CREATE = "create table " + DATABASE_MSG_TABLE + " ("
                                                       + SJLB.Msg.ID + " integer primary key, "
                                                       + SJLB.Msg.DATE + " long, "
                                                       + SJLB.Msg.AUTHOR + " text, "
                                                       + SJLB.Msg.TEXT + " text);";

        private static final String DATABASE_PM_DROP    = "DROP TABLE IF EXISTS " + DATABASE_PM_TABLE;
        private static final String DATABASE_MSG_DROP   = "DROP TABLE IF EXISTS " + DATABASE_MSG_TABLE;

        public SJLBDBOpenHelper (Context        aContext,
                                 String         aName,
                                 CursorFactory  aFactory,
                                 int            aVersion) {
            super(aContext, aName, aFactory, aVersion);
        }
        
        public void onCreate(SQLiteDatabase aDatabase) {
            Log.w("DBAdapter", DATABASE_PM_CREATE);
            Log.w("DBAdapter", DATABASE_MSG_CREATE);
            aDatabase.execSQL(DATABASE_PM_CREATE);
            aDatabase.execSQL(DATABASE_MSG_CREATE);
        }

        // Upgrade : détruit et recréé !
        public void onUpgrade(SQLiteDatabase aDatabase, int aOldVersion, int aNewVersion) {
            Log.w("DBAdapter", "Upgrading from version" + aOldVersion 
                                    + " to " + aNewVersion + ", wich will destroy all data");
            aDatabase.execSQL(DATABASE_PM_DROP);
            aDatabase.execSQL(DATABASE_MSG_DROP);
            onCreate(aDatabase);
        }

    }


}
