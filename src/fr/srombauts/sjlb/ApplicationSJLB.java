package fr.srombauts.sjlb;

import java.io.InputStream;
import java.util.Vector;

import android.app.Application;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.model.LoginPasswordEmptyException;
import fr.srombauts.sjlb.model.PrefsLoginPassword;
import fr.srombauts.sjlb.model.UserContactDescr;
import fr.srombauts.sjlb.service.IntentReceiverStartService;
import fr.srombauts.sjlb.service.StartService;


/**
 *  Classe gérant les objets et données dont le cycle de vie est celui de l'application,
 * c'est à dire au delà de celui d'une activité.
 * 
 * @author 01/09/2010 SRombauts
 */
public class ApplicationSJLB extends Application {
    private static final String LOG_TAG     = "ApplicationSJLB";

    static final String[] CONTACTS_SUMMARY_PROJECTION = {
        Contacts._ID, // 0
        Contacts.DISPLAY_NAME, // 1
        Contacts.STARRED, // 2
        Contacts.TIMES_CONTACTED, // 3
        Contacts.CONTACT_PRESENCE, // 4
        Contacts.PHOTO_ID, // 5
        Contacts.LOOKUP_KEY, // 6
        Contacts.HAS_PHONE_NUMBER, // 7
    };
    /* NOTE SRO : pourrait servir à optimiser en remplaçant les getColumnIndexOrThrow()
    static final int SUMMARY_ID_COLUMN_INDEX = 0;
    static final int SUMMARY_NAME_COLUMN_INDEX = 1;
    static final int SUMMARY_STARRED_COLUMN_INDEX = 2;
    static final int SUMMARY_TIMES_CONTACTED_COLUMN_INDEX = 3;
    static final int SUMMARY_PRESENCE_STATUS_COLUMN_INDEX = 4;
    static final int SUMMARY_PHOTO_ID_COLUMN_INDEX = 5;
    static final int SUMMARY_LOOKUP_KEY = 6;
    static final int SUMMARY_HAS_PHONE_COLUMN_INDEX = 7;
    */
    
    // Liste des contacts Google correspondant aux utilisateurs du site
    private Vector<UserContactDescr>    mUserContactList    = null;

    // Id de l'utilisateur de l'application
    private int                         mUserId             = 0;
    
    // Accesseur simple (optimisé en release par ProGuard)
    synchronized public final int getUserId() {
        return mUserId;
    }

    synchronized public final UserContactDescr getUserContactById(int aUserId) {
        return mUserContactList.get(aUserId);
    }
    
    /**
     * Appelée lorsque l'application démarre, avant toute autre activité
     */
    @Override
    public void onCreate () {
        super.onCreate();
        Log.e(LOG_TAG, "onCreate");
        
        // Lance l'alarme périodique, et le service, si pas déjà lancé, et provoque un rafraîchissement
        IntentReceiverStartService.startAlarm(this, LOG_TAG);

        // Renseigne la liste des contacts Google correspondant aux utilisateurs du site
        initUserContactList ();
    }

    /**
     * Appelée lorsque l'application s'arrête, après la fin de toute autre activité
     *
     * NOTE SRombauts : la doc indique qu'il ne faut pas compter sur cette méthode, et effectivement...
     *                  => fait dans l'ActivityMain.onDestroy()
    */
    public void onTerminate () {
        super.onTerminate();
        Log.e(LOG_TAG, "onTerminate");
        
        // Provoque un rafraîchissement des infos anticipé,
        // qui permet de signaler au site web SJLB les messages qui ont été lus   
        StartService.refresh(this);
    }
    
    // Renseigne la liste des contacts Google correspondant aux utilisateurs du site
    synchronized public void initUserContactList () {
        Log.e(LOG_TAG, "initUserContactList");

        // Liste les utilisateurs du site
        Cursor cursor = getContentResolver().query (SJLB.User.CONTENT_URI,
                                                    null,       // projection "*"
                                                    null, null, // selection, selectionArgs
                                                    null);
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

                // Renseigne dans tous les cas le pseudo associé à l'Id de l'utilisateur, utilisé par la suite pour transcrire un Id en Pseudo 
                UserContactDescr user = new UserContactDescr(cursor.getString(cursor.getColumnIndexOrThrow(SJLB.User.PSEUDO)));
                if (0 < subCursor.getCount()) {
                    subCursor.moveToFirst ();
                    
                    // Fixe la barre de QuickContact
                    final long      contactId = subCursor.getLong(subCursor.getColumnIndexOrThrow(Contacts._ID));
                    final String    lookupKey = subCursor.getString(subCursor.getColumnIndexOrThrow(Contacts.LOOKUP_KEY));
                    user.setLookupUri(Contacts.getLookupUri(contactId, lookupKey));
                    
                    // Ouvre la photo du contact
                    Uri         uri             = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                    InputStream photoDataStream = Contacts.openContactPhotoInputStream(getContentResolver(), uri);
                    if (null != photoDataStream) {
                        user.setPhoto(BitmapFactory.decodeStream(photoDataStream));
                    } else {
                        user.setPhoto(null);
                    }
                }
                
                // Ajoute l'utilisateur dans la liste, avec les infos à "null" si le contact correspondant n'existe pas
                int index = cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.User._ID));
                mUserContactList.add(index, user);
                
            } while (cursor.moveToNext ());
        }
        
        cursor.close();
        
        
        // Recherche l'id de l'utilisateur de l'application à partir de son login
        try {
            // Utilise les préférences pour récupérer le login/mot de passe de l'utilisateur
            PrefsLoginPassword loginPassword = new PrefsLoginPassword(this);
            // (pour des problèmes de comparaison de chaîne sensible à la casse, on ne peut utiliser mUserContactList.indexOf(loginPassword.getLogin());)
            int i=1;
            while ( (0 == mUserId) && (i < mUserContactList.size()) ) {
                if (mUserContactList.get(i).getPseudo().equalsIgnoreCase(loginPassword.getLogin())) {
                    mUserId = i;
                }
                i++;
            }
            if (0 < mUserId) {
                Log.i(LOG_TAG, "L'utilisateur " + loginPassword.getLogin() + " dispose de l'id " + mUserId);
            } else {
                Log.e(LOG_TAG, "Pas d'utilisateur correspondant au login " + loginPassword.getLogin());
            }
        } catch (LoginPasswordEmptyException e) {
            e.printStackTrace();
        }

        Log.e(LOG_TAG, "initUserContactList done : " + mUserContactList.size() + " !");        
    }
}
