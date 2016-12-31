package fr.srombauts.sjlb.model;

import android.graphics.Bitmap;
import android.net.Uri;


// Objet utilisé comme descripteur du contact associé à l'utilisateur
public class UserContactDescr {
    private String   mPseudo    = null;
    private Uri      mLookupUri = null;
    private Bitmap   mPhoto     = null;

    public String getPseudo() {
        return mPseudo;
    }

    public void setPseudo(String mPseudo) {
        this.mPseudo = mPseudo;
    }

    public Uri getLookupUri() {
        return mLookupUri;
    }

    public void setLookupUri(Uri mLookupUri) {
        this.mLookupUri = mLookupUri;
    }

    public Bitmap getPhoto() {
        return mPhoto;
    }

    public void setPhoto(Bitmap mPhoto) {
        this.mPhoto = mPhoto;
    }

    public UserContactDescr (String aPseudo) {
        mPseudo     = aPseudo;
        mLookupUri  = null;
        mPhoto      = null;
    }    
}
