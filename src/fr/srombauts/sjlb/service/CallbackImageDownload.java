package fr.srombauts.sjlb.service;

import android.graphics.Bitmap;

public interface CallbackImageDownload
{
    /**
     * Appelée lorsqu'un téléchargement d'une image s'est terminé
     * dans le contexte du thread de GUI (méthode dite synchronisée)
     * 
     * @param abResult
     */
    public void onImageDownloaded (Bitmap aBitmap);
}
