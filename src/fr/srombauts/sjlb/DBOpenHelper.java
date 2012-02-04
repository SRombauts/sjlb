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

    public DBOpenHelper (Context        aContext,
                         String         aName,
                         CursorFactory  aFactory,
                         int            aVersion) {
        super(aContext, aName, aFactory, aVersion);
    }
    
    // Création des tables de la base si celle si est inexistante (ou n'était pas à jour)
    public void onCreate(SQLiteDatabase aDatabase) {
        Log.w(LOG_TAG, "onCreate");
        aDatabase.execSQL(SJLB.PM.TABLE_CREATE);
        aDatabase.execSQL(SJLB.Subj.TABLE_CREATE);
        aDatabase.execSQL(SJLB.Msg.TABLE_CREATE);
        aDatabase.execSQL(SJLB.User.TABLE_CREATE);
    }

    // Upgrade de la version du schéma de base : détruit et recréé !
    public void onUpgrade(SQLiteDatabase aDatabase, int aOldVersion, int aNewVersion) {
        if (   (2 == aOldVersion)
            && (3 == aNewVersion) )
        {
            Log.i(LOG_TAG, "Upgrading from version 2 to 3, wich will preserve data");
            aDatabase.execSQL(SJLB.Msg.TABLE_DROP);
            aDatabase.execSQL(SJLB.Subj.TABLE_CREATE);
            aDatabase.execSQL(SJLB.Msg.TABLE_CREATE);
        } else {
            Log.w(LOG_TAG, "Upgrading from version" + aOldVersion + " to " + aNewVersion + ", wich will destroy all data");
            aDatabase.execSQL(SJLB.PM.TABLE_DROP);
            aDatabase.execSQL(SJLB.Subj.TABLE_DROP);
            aDatabase.execSQL(SJLB.Msg.TABLE_DROP);
            aDatabase.execSQL(SJLB.User.TABLE_DROP);
            onCreate(aDatabase);
        }
    }

}
