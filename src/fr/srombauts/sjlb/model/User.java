package fr.srombauts.sjlb.model;

import java.util.Date;


/**
 * Encapsulation d'un utilisateur
 * @author 17/08/2010 SRombauts
 */
public class User {
    private int     mId;
    private String  mPseudo;
    private String  mName;
    private String  mAddress;
    private String  mNotes;
    private Date    mDateMaj;

    // Constructeur
    public User(int aId, String aPseudo, String aName, String aAddress, String aNotes, Date aDateMaj) {
        mId         = aId;
        mPseudo     = aPseudo;
        mName       = aName;
        mAddress    = aAddress;
        mNotes      = aNotes;
        mDateMaj    = aDateMaj;
    }
    
    // Getters basics (optimisés en release par ProGuard)
    public int getId() {
        return mId;
    }
    public String getPseudo() {
        return mPseudo;
    }
    public String getName() {
        return mName;
    }
    public String getAddress() {
        return mAddress;
    }
    public String getNotes() {
        return mNotes;
    }
    public Date getDateMaj () {
        return mDateMaj;
    }
    
    /**
     * Retourne une description résumée en une unique chaîne
     */
    public String toString () {
        return mName + " (" + mPseudo + ", id=" + mId + ")";
    }
}
