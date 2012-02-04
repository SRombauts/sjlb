package fr.srombauts.sjlb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;


/**
 * Gestionnaire de création/mise à jour de la base de données SQLite
 * 
 * @author 17/06/2010 srombauts
 */
public class DBOpenHelper extends SQLiteOpenHelper {
    private static final String  LOG_TAG = "DBAdapter";

    private static final String DATABASE_PM_CREATE  = "create table " + SJLB.PM.TABLE_NAME + " ("
                                                    + SJLB.PM.ID        + " integer primary key, "
                                                    + SJLB.PM.DATE      + " text, "
                                                    + SJLB.PM.AUTHOR_ID + " integer, "
                                                    + SJLB.PM.AUTHOR    + " text, "
                                                    + SJLB.PM.TEXT      + " text);";

    private static final String DATABASE_SUBJ_CREATE= "create table " + SJLB.Subj.TABLE_NAME + " ("
                                                   + SJLB.Subj.ID       + " integer primary key, "
                                                   + SJLB.Subj.CAT_ID   + " integer, "
                                                   + SJLB.Subj.GROUP_ID + " integer, "
                                                   + SJLB.Subj.LAST_DATE+ " date, "
                                                   + SJLB.Subj.TEXT     + " text);";

    private static final String DATABASE_MSG_CREATE= "create table " + SJLB.Msg.TABLE_NAME + " ("
                                                   + SJLB.Msg.ID        + " integer primary key, "
                                                   + SJLB.Msg.DATE      + " text, "
                                                   + SJLB.Msg.AUTHOR_ID + " integer, "
                                                   + SJLB.Msg.AUTHOR    + " text, "
                                                   + SJLB.Msg.SUBJECT_ID+ " integer, "
                                                   + SJLB.Msg.TEXT      + " text);";

    private static final String DATABASE_USER_CREATE= "create table " + SJLB.User.TABLE_NAME + " ("
                                                    + SJLB.User.ID      + " integer primary key, "
                                                    + SJLB.User.PSEUDO  + " text);";

    private static final String DATABASE_PM_DROP    = "DROP TABLE IF EXISTS " + SJLB.PM.TABLE_NAME;

    private static final String DATABASE_SUBJ_DROP  = "DROP TABLE IF EXISTS " + SJLB.Subj.TABLE_NAME;

    private static final String DATABASE_MSG_DROP   = "DROP TABLE IF EXISTS " + SJLB.Msg.TABLE_NAME;

    private static final String DATABASE_USER_DROP  = "DROP TABLE IF EXISTS " + SJLB.User.TABLE_NAME;

    
    public DBOpenHelper (Context        aContext,
                         String         aName,
                         CursorFactory  aFactory,
                         int            aVersion) {
        super(aContext, aName, aFactory, aVersion);
    }
    
    // Création des tables de la base si celle si est inexistante (ou n'était pas à jour)
    public void onCreate(SQLiteDatabase aDatabase) {
        Log.w(LOG_TAG, "onCreate");
        aDatabase.execSQL(DATABASE_PM_CREATE);
        aDatabase.execSQL(DATABASE_SUBJ_CREATE);
        aDatabase.execSQL(DATABASE_MSG_CREATE);
        aDatabase.execSQL(DATABASE_USER_CREATE);
    }

    // Upgrade de la version du schéma de base : détruit et recréé !
    public void onUpgrade(SQLiteDatabase aDatabase, int aOldVersion, int aNewVersion) {
        if (   (2 == aOldVersion)
            && (3 == aNewVersion) )
        {
            Log.i(LOG_TAG, "Upgrading from version 2 to 3, wich will preserve data");
            aDatabase.execSQL(DATABASE_MSG_DROP);
            aDatabase.execSQL(DATABASE_SUBJ_CREATE);
            aDatabase.execSQL(DATABASE_MSG_CREATE);
        } else {
            Log.w(LOG_TAG, "Upgrading from version" + aOldVersion + " to " + aNewVersion + ", wich will destroy all data");
            aDatabase.execSQL(DATABASE_PM_DROP);
            aDatabase.execSQL(DATABASE_SUBJ_DROP);
            aDatabase.execSQL(DATABASE_MSG_DROP);
            aDatabase.execSQL(DATABASE_USER_DROP);
            onCreate(aDatabase);
        }
    }

}
