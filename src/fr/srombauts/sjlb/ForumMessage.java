package fr.srombauts.sjlb;

import java.util.Date;

/**
 * Encapsulation d'un message du forum
 * @author 14/06/2010 srombauts
 */
public class ForumMessage {
    private int     mId;
    private Date    mDate;
    private String  mAuthor;
    private String  mText;

    public ForumMessage(int aId, Date aDate, String aAuthor, String aText) {
        mId     = aId;
        mDate   = aDate;
        mAuthor = aAuthor;
        mText   = aText;
    }
    
    public int getId () {
        return mId;
    }
    public Date getDate () {
        return mDate;
    }
    public String getAuthor () {
        return mAuthor;
    }
    public String getText () {
        return mText;
    }
    
}
