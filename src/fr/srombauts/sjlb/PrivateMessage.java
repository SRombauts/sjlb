package fr.srombauts.sjlb;

import java.util.Date;

/**
 * Encapsulation d'un message privé
 * @author seb
 */
public class PrivateMessage {
    private int     mId;
    private Date    mDate;
    private String  mAuthor;
    private String  mText;

    public PrivateMessage(int aId, Date aDate, String aAuthor, String aText) {
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
