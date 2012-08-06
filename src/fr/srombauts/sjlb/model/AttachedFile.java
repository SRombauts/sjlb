package fr.srombauts.sjlb.model;


/**
 * Encapsulation d'un message privé
 * @author 23/03/2011 srombauts
 */
public class AttachedFile {
    private int     mMessageId;
    private String  mFilename;

    public AttachedFile(int aMessageId, String aFilename) {
        mMessageId  = aMessageId;
        mFilename   = aFilename;
    }
    
    public int getMessageId () {
        return mMessageId;
    }
    public String getFilename () {
        return mFilename;
    }
    
    /**
     * Retourne une description résumée en une unique chaine
     */
    public String toString () {
        return mMessageId + " : " + mFilename;
    }


}
