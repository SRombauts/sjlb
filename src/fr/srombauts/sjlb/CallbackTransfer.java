package fr.srombauts.sjlb;

public interface CallbackTransfer
{
    /**
     * Appelée lorsqu'un transfert s'est terminé (post d'un nouveau messages, effacement d'un PM...),
     * dans le contexte du thread de GUI (méthode dite synchronisée)
     * 
     * @param abResult
     */
    public void onTransferDone (boolean abResult);
}