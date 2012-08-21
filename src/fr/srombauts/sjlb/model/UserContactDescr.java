package fr.srombauts.sjlb.model;

import android.graphics.Bitmap;
import android.net.Uri;


// Objet utilisé comme descripteur du contact associé à l'utilisateur
public class UserContactDescr {
    public String   mPseudo;
    public Uri      mLookupUri;
    public Bitmap   mPhoto;

    public UserContactDescr (String aPseudo) {
        mPseudo     = aPseudo;
        mLookupUri  = null;
        mPhoto      = null;
    }    
}
