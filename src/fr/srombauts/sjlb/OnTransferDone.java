package fr.srombauts.sjlb;

public interface OnTransferDone
{
    // Appelée lorsqu'un transfert s'est terminé (post d'un nouveau messages, effacement d'un PM...)
    public void onTransferDone (boolean abResult);
}
