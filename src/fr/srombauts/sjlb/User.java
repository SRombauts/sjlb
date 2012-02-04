package fr.srombauts.sjlb;


/**
 * Encapsulation d'un utilisateur
 * @author 17/08/2010 srombauts
 */
public class User {
    private int     mId;
    private String  mPseudo;

    public User(int aId,String aPseudo) {
        mId     = aId;
        mPseudo = aPseudo;
    }
    
    public int getId () {
        return mId;
    }
    public String getPseudo () {
        return mPseudo;
    }
    
    /**
     * Retourne une description résumée en une unique chaine
     */
    public String toString () {
        return mPseudo + " (" + mId + ")";
    }
}
