package fr.srombauts.sjlb.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Encapsulation d'un message privé
 * @author 14/06/2010 SRombauts
 */
public class PrivateMessage {
    private int     mId;
    private Date    mDate;
    private int     mAuthorId;
    private String  mAuthor;
    private String  mText;

    public PrivateMessage(int aId, Date aDate, int aAuthorId, String aAuthor, String aText) {
        mId         = aId;
        mDate       = aDate;
        mAuthorId   = aAuthorId;
        mAuthor     = aAuthor;
        mText       = aText;
    }
    
    public int getId () {
        return mId;
    }
    public Date getDate () {
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
        return mAuthor + " (" + getDateString (mDate) + ") : " + mText;
    }

    // Formatte une date en chaîne de caractère
    static public String getDateString (Date aDate) {
        SimpleDateFormat    sdf         = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String              dateString  = sdf.format(aDate);

        return dateString;
    }

}
