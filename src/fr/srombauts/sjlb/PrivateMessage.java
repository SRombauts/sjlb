package fr.srombauts.sjlb;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Encapsulation d'un message privé
 * @author 14/06/2010 srombauts
 */
public class PrivateMessage {
    private int     mId;
    private String  mDate;
    private int     mAuthorId;
    private String  mAuthor;
    private String  mText;

    public PrivateMessage(int aId, Date aDate, int aAuthorId, String aAuthor, String aText) {
        SimpleDateFormat    sdf         = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        String              dateString  = sdf.format(aDate);

        mId         = aId;
        mDate       = dateString;
        mAuthorId   = aAuthorId;
        mAuthor     = aAuthor;
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
    public String getText () {
        return mText;
    }
    
    /**
     * Retourne une description résumée en une unique chaine
     */
    public String toString () {
        return mAuthor + " (" + mDate + ") : " + mText;
    }
}
