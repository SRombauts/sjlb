package fr.srombauts.sjlb;

import java.util.Date;

/**
 * Encapsulation d'un sujet du forum
 * @author 18/08/2010 srombauts
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
    
}