package fr.srombauts.sjlb;

import java.io.InputStream;
import java.util.Vector;

import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.model.UserContactDescr;

import android.app.Application;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;


/**
 *  Classe gérant les objets et données dont le cycle de vie est celui de l'application,
 * c'est à dire au delas de celui d'une activité.
 * 
 * @author 01/09/2010 SRombauts
 */
public class ApplicationSJLB extends Application {
    private static final String LOG_TAG     = "ApplicationSJLB";

    // TODO SRombauts : implémenter comme ActivitySubjects un ResourceCursorAdapter, pour afficher une icone pour les messages non lus, et un Badge avec QuickView !
    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
        Contacts._ID, // 0
        Contacts.DISPLAY_NAME, // 1
        Contacts.STARRED, // 2
        Contacts.TIMES_CONTACTED, // 3
        Contacts.CONTACT_PRESENCE, // 4
        Contacts.PHOTO_ID, // 5
        Contacts.LOOKUP_KEY, // 6
        Contacts.HAS_PHONE_NUMBER, // 7
    };
    // TODO SRombauts : à supprimer !
    static final int SUMMARY_ID_COLUMN_INDEX = 0;
    static final int SUMMARY_NAME_COLUMN_INDEX = 1;
    static final int SUMMARY_STARRED_COLUMN_INDEX = 2;
    static final int SUMMARY_TIMES_CONTACTED_COLUMN_INDEX = 3;
    static final int SUMMARY_PRESENCE_STATUS_COLUMN_INDEX = 4;
    static final int SUMMARY_PHOTO_ID_COLUMN_INDEX = 5;
    static final int SUMMARY_LOOKUP_KEY = 6;
    static final int SUMMARY_HAS_PHONE_COLUMN_INDEX = 7;
    
    
    
    // Liste des contacts Google correspondant aux utilisateurs du site
    public Vector<UserContactDescr> mUserContactList    = null;
    
	/**
	 * Appelée lorsque l'application démarre, avant toute autre activité
	 */
	@Override
	public void onCreate () {
		super.onCreate();
		
		Log.d(LOG_TAG, "onCreate");
		
	    initUserContactList ();
	}
	
	
	// Renseigne la liste des contacts Google correspondant aux utilisateurs du site
    public void initUserContactList () {

        // Liste les utilisateurs du site
        Cursor cursor = getContentResolver().query (SJLB.User.CONTENT_URI, null, null, null, null);
        // Créer la liste des utilisateurs du site, avec comme clef leur ID, et comme donnée un objet avec leur "LookupUri" et leur photo pour optimiser l'affichage dans la liste
        mUserContactList = new Vector<UserContactDescr> (cursor.getCount()+1);
        // Ajoute un utilisateur null à l'index 0
        mUserContactList.add(0,null);
        
        // Boucle sur les utilisateurs pour récupérer le contact éventuellement associé
        if (cursor.moveToFirst ()) {
            do {
                Cursor subCursor = getContentResolver().query(Contacts.CONTENT_URI,
                                                              CONTACTS_SUMMARY_PROJECTION,
                                                              Contacts.DISPLAY_NAME + " LIKE '" + cursor.getString(cursor.getColumnIndexOrThrow(SJLB.User.NAME)) + "'",
                                                              null,
                                                              null);
                Log.d(LOG_TAG, cursor.getString(cursor.getColumnIndexOrThrow(SJLB.User.NAME)) + "' = " + subCursor.getCount());

                UserContactDescr user = new UserContactDescr();
                if (0 < subCursor.getCount()) {
                    subCursor.moveToFirst ();
                    
                    // Fixe la barre de QuickContact
                    final long      contactId = subCursor.getLong(subCursor.getColumnIndexOrThrow(Contacts._ID));
                    final String    lookupKey = subCursor.getString(subCursor.getColumnIndexOrThrow(Contacts.LOOKUP_KEY));
                    user.lookupUri = Contacts.getLookupUri(contactId, lookupKey);
                    
                    // Ouvre la photo du contact
                    Uri         uri             = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                    InputStream photoDataStream = Contacts.openContactPhotoInputStream(getContentResolver(), uri);
                    if (null != photoDataStream) {
                        user.photo = BitmapFactory.decodeStream(photoDataStream);
                    } else {
                        user.photo = null;
                    }
                }
                
                // Ajoute l'utilisateur dans la liste, avec les infos à "null" si le contact correspondant n'existe pas
                int index = cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.User._ID));
                mUserContactList.add(index, user);
                
            } while (cursor.moveToNext ());
        }
        
        cursor.close();
	}
}
