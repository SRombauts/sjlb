package fr.srombauts.sjlb;


/**
 * Encapsulation d'un utilisateur
 * @author 17/08/2010 srombauts
 */
public class User {
    private int     mId;
    private String  mPseudo;
    private String  mNom;

    public User(int aId, String aPseudo, String aNom) {
        mId     = aId;
        mPseudo = aPseudo;
        mNom    = aNom;
    }
    
    public int getId () {
        return mId;
    }
    public String getPseudo () {
        return mPseudo;
    }
    public String getNom () {
        return mNom;
    }
    
    /**
     * Retourne une description résumée en une unique chaine
     */
    public String toString () {
        return mNom + " (" + mPseudo + ", id=" + mId + ")";
    }
}
