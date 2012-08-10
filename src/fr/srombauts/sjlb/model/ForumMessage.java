package fr.srombauts.sjlb.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Encapsulation d'un message du forum
 * @author 18/08/2010 SRombauts
 */
public class ForumMessage {
    private int     mId;
    private Date    mDate;
    private int     mAuthorId;
    private int     mSubjectId;
    private boolean mbUnread;
    private String  mText;

    public ForumMessage(int aId, Date aDate, int aAuthorId, String aAuthor, int aSubjectId, boolean abUnread, String aText) {
        mId         = aId;
        mDate       = aDate;
        mAuthorId   = aAuthorId;
        mSubjectId  = aSubjectId;
        mbUnread    = abUnread;
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
    public int getSubjectId () {
        return mSubjectId;
    }
    public boolean isUnread () {
        return mbUnread;
    }
    public String getText () {
        return mText;
    }

    // Résumé du contenu de l'objet sous forme d'une chaîne de caractère
    public String toString () {
		return mId + ": " + getDateString (mDate) + " " + mAuthorId + " subj=" + mSubjectId + " unread=" + mbUnread + " : " + mText;
    }    


    // Formatte une date en chaîne de caractère
    static public String getDateString (Date aDate) {
        SimpleDateFormat    sdf         = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String              dateString  = sdf.format(aDate);

        return dateString;
    }
}
