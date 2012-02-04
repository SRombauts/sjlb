package fr.srombauts.sjlb;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Encapsulation d'un message du forum
 * @author 18/08/2010 srombauts
 */
public class ForumMessage {
    private int     mId;
    private String  mDate;
    private int     mAuthorId;
    private String  mAuthor;
    private int     mSubjectId;
    private String  mText;

    public ForumMessage(int aId, Date aDate, int aAuthorId, String aAuthor, int aSubjectId, String aText) {
        SimpleDateFormat    sdf         = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String              dateString  = sdf.format(aDate);

        mId         = aId;
        mDate       = dateString;
        mAuthorId   = aAuthorId;
        mAuthor     = aAuthor;
        mSubjectId  = aSubjectId;
        mText       = aText;
    }
    
    public int getId () {
        return mId;
    }
    public String getDate () {
        return mDate;
    }
    public int getAuthorId () {
        return mAuthorId;
    }
    public String getAuthor () {
        return mAuthor;
    }
    public int getSubjectId () {
        return mSubjectId;
    }
    public String getText () {
        return mText;
    }
    
    public String toString () {
		return "" + mId + ": " + mDate + " " + mAuthor + " (" + mAuthorId + ") " + mSubjectId + " : " + mText;
    }    
}
