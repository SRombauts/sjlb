package fr.srombauts.sjlb.model;

import java.util.Date;

/**
 * Encapsulation d'un sujet du forum
 * @author 18/08/2010 SRombauts
 */
public class ForumSubject {
    private int     mId;
    private int     mCategoryId;
    private int     mGroupId;
    private Date    mLastDate;
    private String  mText;

    public ForumSubject(int aId, int aCategoryId, int aGroupId, Date aLastDate, String aText) {
        mId         = aId;
        mCategoryId = aCategoryId;
        mGroupId    = aGroupId;
        mLastDate   = aLastDate;
        mText       = aText;
    }
    
    public int getId () {
        return mId;
    }
    public int getCategoryId () {
        return mCategoryId;
    }
    public int getGroupId () {
        return mGroupId;
    }
    public Date getLastDate () {
        return mLastDate;
    }
    public String getText () {
        return mText;
    }
    

    // Résumé du contenu de l'objet sous forme d'une chaîne de caractère
    public String toString () {
        return mId + ": cat=" + mCategoryId + " group=" + mGroupId + " (" + mLastDate + ") : " + mText;
    }    
    
    
}
