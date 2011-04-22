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
    
    /**
     * Retourne une description résumée en une unique chaine
     */
    public String toString () {
        SimpleDateFormat    sdf         = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        String              dateString  = sdf.format(mDate);
                
        return mAuthor + " (" + dateString + ") : " + mText;
    }
}
