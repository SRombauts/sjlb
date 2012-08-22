package fr.srombauts.sjlb.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;


/**
 * Gestionnaire de création/mise à jour de la base de données SQLite
 * 
 * @author 17/06/2010 SRombauts
 */
public class DBOpenHelper extends SQLiteOpenHelper {
    private static final String  LOG_TAG = "DBAdapter";

    public DBOpenHelper (Context        aContext,
                         String         aName,
                         CursorFactory  aFactory,
                         int            aVersion) {
        super(aContext, aName, aFactory, aVersion);
    }
    
    /**
     * Création des tables de la base si celle si est inexistante.
     * 
     * Voir onUpgrade() pour les cas où le schéma de bdd n'est pas à jour
     */
    public void onCreate(SQLiteDatabase aDatabase) {
        Log.d(LOG_TAG, "onCreate");
        aDatabase.execSQL(SJLB.PM.TABLE_CREATE);
        aDatabase.execSQL(SJLB.Subj.TABLE_CREATE);
        aDatabase.execSQL(SJLB.Msg.TABLE_CREATE);
        aDatabase.execSQL(SJLB.File.TABLE_CREATE);
        aDatabase.execSQL(SJLB.User.TABLE_CREATE);
    }

    /**
     * Upgrade de la version du schéma de base : détruit et recréé !
     *
     * La version de la BDD est définie dans SJLB.DATABASE_VERSION
    */     
    public void onUpgrade(SQLiteDatabase aDatabase, int aOldVersion, int aNewVersion) {
        if (   (8                     == aOldVersion)
            && (SJLB.DATABASE_VERSION == aNewVersion) ) {
            Log.i(LOG_TAG, "Upgrading from version 8 to 9, wich will preserve users table but not PM table");
            // La table des utilisateurs doit être complétée de 3 champs
            aDatabase.execSQL("ALTER TABLE " + SJLB.User.TABLE_NAME + " ADD " + SJLB.User.ADDRESS + " text");
            aDatabase.execSQL("ALTER TABLE " + SJLB.User.TABLE_NAME + " ADD " + SJLB.User.NOTES + " text");
            aDatabase.execSQL("ALTER TABLE " + SJLB.User.TABLE_NAME + " ADD " + SJLB.User.DATE_MAJ + " integer");
            // La table des Msg doit être complétée d'un champ
            aDatabase.execSQL("ALTER TABLE " + SJLB.Msg.TABLE_NAME + " ADD " + SJLB.Msg.DATE_EDIT + " integer");
            // La table des PM doit être reconstruite, le plus simple c'est de l'effacer
            aDatabase.execSQL(SJLB.PM.TABLE_DROP);
            aDatabase.execSQL(SJLB.PM.TABLE_CREATE);
        } else if (   (7 == aOldVersion)
                   && (8 == aNewVersion) ) {
            Log.i(LOG_TAG, "Upgrading from version 7 to 8, wich will preserve data");
            aDatabase.execSQL("ALTER TABLE " + SJLB.Subj.TABLE_NAME + " ADD " + SJLB.Subj.NB_UNREAD + " integer");
        } else if (   (6 == aOldVersion)
                   && (7 == aNewVersion) ) {
            Log.i(LOG_TAG, "Upgrading from version 6 to 7, wich will preserve data");
            aDatabase.execSQL(SJLB.File.TABLE_CREATE);
        } else if (   (2 == aOldVersion)
                   && (3 == aNewVersion) ) {
            Log.i(LOG_TAG, "Upgrading from version 2 to 3, wich will preserve data");
            aDatabase.execSQL(SJLB.Subj.TABLE_CREATE);
            aDatabase.execSQL(SJLB.Msg.TABLE_CREATE);
        } else {
            Log.w(LOG_TAG, "Upgrading from version" + aOldVersion + " to " + aNewVersion + ", wich will destroy all data");
            aDatabase.execSQL(SJLB.PM.TABLE_DROP);
            aDatabase.execSQL(SJLB.Subj.TABLE_DROP);
            aDatabase.execSQL(SJLB.Msg.TABLE_DROP);
            aDatabase.execSQL(SJLB.File.TABLE_CREATE);
            aDatabase.execSQL(SJLB.User.TABLE_DROP);
            onCreate(aDatabase);
        }
    }

}
