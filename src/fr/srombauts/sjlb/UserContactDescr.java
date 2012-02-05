package fr.srombauts.sjlb;

import android.graphics.Bitmap;
import android.net.Uri;


// Objet utilisé comme descripteur du contact associé à l'utilisateur
public class UserContactDescr {
    public Uri    lookupUri;
    public Bitmap photo;

    public UserContactDescr () {
        lookupUri   = null;
        photo       = null;
    }    
}
