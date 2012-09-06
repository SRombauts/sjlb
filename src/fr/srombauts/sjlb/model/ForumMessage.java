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
    private Date    mDateEdit;
    private int     mAuthorId;
    private int     mSubjectId;
    private boolean mbUnread;
    private String  mText;

    // Constructeur
    public ForumMessage(int aId, Date aDate, Date aDateEdit, int aAuthorId, int aSubjectId, boolean abUnread, String aText) {
        mId         = aId;
        mDate       = aDate;
        mDateEdit   = aDateEdit;
        mAuthorId   = aAuthorId;
        mSubjectId  = aSubjectId;
        mbUnread    = abUnread;
        mText       = aText;
    }
    
    // Getters basics (optimisés en release par ProGuard)
    public int getId () {
        return mId;
    }
    public Date getDate () {
        return mDate;
    }
    public Date getDateEdit () {
        return mDateEdit;
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
        return mId + ": " + getDateString (mDate) + " (" + getDateString (mDateEdit) + ") " + mAuthorId + " subj=" + mSubjectId + " unread=" + mbUnread + " : " + mText;
    }    


    // Formate une date en chaîne de caractère
    static public String getDateString (Date aDate) {
        final SimpleDateFormat  dateFormat  = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        final String            dateString  = dateFormat.format(aDate);

        return dateString;
    }
}
