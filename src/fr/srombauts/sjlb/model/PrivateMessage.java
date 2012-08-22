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
    private int     mDestId;
    private String  mText;

    // Constructeur
    public PrivateMessage(int aId, Date aDate, int aAuthorId, int aDestId, String aText) {
        mId         = aId;
        mDate       = aDate;
        mAuthorId   = aAuthorId;
        mDestId     = aDestId;
        mText       = aText;
    }
    
    // Getters basics (optimisés en release par ProGuard)
    public int getId() {
        return mId;
    }
    public Date getDate() {
        return mDate;
    }
    public int getAuthorId() {
        return mAuthorId;
    }
    public int getDestId() {
        return mDestId;
    }
    public String getText() {
        return mText;
    }
    
    /**
     * Retourne une description résumée en une unique chaîne
     */
    public String toString() {
        return mAuthorId + " (" + getDateString (mDate) + ") -> " + mDestId + " : " + mText;
    }

    // Formate une date en chaîne de caractère
    static public String getDateString (Date aDate) {
        SimpleDateFormat    sdf         = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String              dateString  = sdf.format(aDate);

        return dateString;
    }
}
