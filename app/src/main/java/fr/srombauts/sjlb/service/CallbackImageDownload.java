package fr.srombauts.sjlb.service;

import android.graphics.Bitmap;

public interface CallbackImageDownload
{
    /**
     * Appelée lorsqu'un téléchargement d'une image s'est terminé
     * dans le contexte du thread de GUI (méthode dite synchronisée)
     * 
     * @param aBitmap   Image correspondant au fichier téléchargé
     * @param aPosition Position du fichier dans la liste des fichiers attachés d'un message
     */
    public void onImageDownloaded (Bitmap aBitmap, int aPosition);
}
