package fr.srombauts.sjlb;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.util.Log;


/**
 * Gestionnaire de création/mise à jour de la base de données SQLite
 * 
 * @author seb
 */
public class SJLBDBOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_MSG_CREATE = "create table " + SJLB.Msg.TABLE_NAME + " ("
                                                   + SJLB.Msg.ID + " integer primary key, "
                                                   + SJLB.Msg.DATE + " long, "
                                                   + SJLB.Msg.AUTHOR + " text, "
                                                   + SJLB.Msg.TEXT + " text);";

    private static final String DATABASE_PM_CREATE = "create table " + SJLB.PM.TABLE_NAME + " ("
    + SJLB.PM.ID + " integer primary key, "
    + SJLB.PM.DATE + " long, "
    + SJLB.PM.AUTHOR + " text, "
    + SJLB.PM.TEXT + " text);";

    private static final String DATABASE_MSG_DROP   = "DROP TABLE IF EXISTS " + SJLB.Msg.TABLE_NAME;

    private static final String DATABASE_PM_DROP    = "DROP TABLE IF EXISTS " + SJLB.PM.TABLE_NAME;

    
    public SJLBDBOpenHelper (Context        aContext,
                             String         aName,
                             CursorFactory  aFactory,
                             int            aVersion) {
        super(aContext, aName, aFactory, aVersion);
    }
    
    public void onCreate(SQLiteDatabase aDatabase) {
        Log.w("DBAdapter", DATABASE_MSG_CREATE);
        aDatabase.execSQL(DATABASE_MSG_CREATE);
        aDatabase.execSQL(DATABASE_PM_CREATE);
    }

    // Upgrade : détruit et recréé !
    public void onUpgrade(SQLiteDatabase aDatabase, int aOldVersion, int aNewVersion) {
        Log.w("DBAdapter", "Upgrading from version" + aOldVersion 
                                + " to " + aNewVersion + ", wich will destroy all data");
        aDatabase.execSQL(DATABASE_MSG_DROP);
        aDatabase.execSQL(DATABASE_PM_DROP);
        onCreate(aDatabase);
    }

}
