package fr.srombauts.sjlb.gui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.service.AsynchTaskDownloadImage;
import fr.srombauts.sjlb.service.CallbackImageDownload;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 SRombauts
 */
public class ActivityFiles extends ActivityTouchListener implements CallbackImageDownload {
    private static final String LOG_TAG = "ActivityFiles";
    
    public static final String  START_INTENT_EXTRA_MSG_ID           = "MessageId";
    
    private static final String URI_REPERTOIRE_FICHIERS_ATTACHES    = "http://www.sjlb.fr/FichiersAttaches/";

    private FileListItemAdapter mAdapter            = null;
    private FileListItem[]      mFileListItem;

    private long                mSelectedMessageId  = 0;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d (LOG_TAG, "onCreate...");
        
        // Layout de l'activité
        setContentView(R.layout.activity_list);
        
        // Récupère l'éventuel paramètre de lancement (id de du sujet du forum sélectionnée)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            mSelectedMessageId     = startIntent.getExtras().getLong  (START_INTENT_EXTRA_MSG_ID);
            Log.i(LOG_TAG, "SelectedMessage (" + mSelectedMessageId + ")");
        }
        
        // Map la description du sujet pour la renseigner dans le titre
        // TODO SRombauts : réserver un champ texte pour afficher aussi le message
        setTitle(getString(R.string.files_description));
        
        // Récupère un curseur sur les données (les fichiers attachés) en filtrant sur l'id du message sélectionné
        final String[] columns = {SJLB.File.FILENAME};
        Cursor cursor = managedQuery( SJLB.File.CONTENT_URI,
                                columns, // ne récupère que le filename
                                SJLB.File.MSG_ID + "=" + mSelectedMessageId,
                                null, null);

        // Constitue le tableau de fichiers
        mFileListItem = new FileListItem [cursor.getCount()];
        int nbFiles = cursor.getCount();
        //Log.v(LOG_TAG, "mSelectedMessageId=" + mSelectedMessageId + " cursor.getCount()=" + cursor.getCount());
        for (int position = 0; position < nbFiles; position++) {
            if (cursor.moveToPosition(position)) {
                // Récupère le nom du fichier
                FileListItem fileItem = new FileListItem();
                fileItem.fileName = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.File.FILENAME));
                mFileListItem[position] = fileItem;
                // Lance ici le téléchargement du fichier en tache de fond
                // TODO SRombauts : mémoriser les bitmaps au niveau de l'application pour pouvoir les restaurer rapidement (changement d'orientation) ou les partager entre activités
                // TODO SRombauts : ne faire ça que s'il s'agit d'une extension d'image reconnue !
                fileItem.fileDownloader = new AsynchTaskDownloadImage(this);
                Log.i(LOG_TAG, "ImageDownloader.execute(" + fileItem.fileName + ", " + position + ")");
                fileItem.fileDownloader.execute(URI_REPERTOIRE_FICHIERS_ATTACHES + fileItem.fileName,
                                        Long.toString(position));
            }
        }

        // Créer l'adapteur entre la liste de fichiers et le layout et les informations sur le mapping des colonnes
        mAdapter = new FileListItemAdapter( this,
                                            R.layout.file_item,
                                            mFileListItem);

        ListView    fileListView = (ListView)findViewById(R.id.activity_listview);
        fileListView.setAdapter (mAdapter);

        // Enregistre les listener d'IHM que la classe implémente
        // TODO SRombauts
        //mFileListView.setOnItemClickListener (this);
    }
    
    @Override
    protected void onResume () {
        super.onResume();
        Log.d (LOG_TAG, "onResume... clear UNREAD flags");
    }
    
    // Appelée lorsque l'activité passe de "au premier plan" à "en pause/cachée" 
    protected void onPause() {
        super.onPause();
        Log.d (LOG_TAG, "onPause...");
     }
    
    protected void onStop() {
        super.onStop();
        Log.d (LOG_TAG, "onStop... : cancel downloads");
        // Interrompt tout chargement éventuellement en cours
        int nbFiles = mFileListItem.length;
        for (int position = 0; position < nbFiles; position++) {
            mFileListItem[position].fileDownloader.cancel();
        }
    }
    
    /**
     * Appelée lorsqu'un téléchargement d'une image s'est terminé
     * dans le contexte du thread de GUI (méthode dite synchronisée)
     * 
     * @param aBitmap   Image correspondant au fichier téléchargé
     * @param aPosition Position du fichier dans la liste des fichiers attachés d'un message
     */
    public void onImageDownloaded(Bitmap aBitmap, int aPosition) {
        FileListItem fileItem = mFileListItem[aPosition];
        if (null != aBitmap) {
            // TODO SRombauts : mémoriser les bitmaps au niveau de l'application pour pouvoir les restaurer rapidement (changement d'orientation) ou les partager entre activités
            fileItem.fileBitmap = aBitmap;
            Log.i (LOG_TAG, "onImageDownloaded(" + fileItem.fileName + ", " + aPosition + ")=" + fileItem.fileBitmap);
            mAdapter.notifyDataSetChanged();
        } else {
            Log.e (LOG_TAG, "onImageDownloaded(" + fileItem.fileName + ", " + aPosition + ")= not an image !?");
        }
    }
    
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activité pour retour à la liste des message");
        finish ();
        return true;
    }

    // NOTE SRombauts : pas besoin de onRightGesture()
    
    
    /** 
     * @brief ArayAdapter gérant l'affichage de la liste des images téléchargées
     */
    private class FileListItemAdapter extends ArrayAdapter<FileListItem> {
        private LayoutInflater mInflater;

        public FileListItemAdapter(Context context, int textViewResourceId, FileListItem[] items) {
            super(context, textViewResourceId, items);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Log.d (LOG_TAG, "getView(" + position + "," + convertView + "," + parent + ")");
            
            View        view;
            TextView    fileTextView;
            ImageView   fileImageView;

            // Construit ou récupère la vue appropriée
            if (convertView == null) {
                Log.v (LOG_TAG, "inflate");
                view = mInflater.inflate(R.layout.file_item, parent, false);
            } else {
                view = convertView;
            }

            // Récupère l'item correspondant à la position
            FileListItem fileItem = getItem(position);
                    
            fileTextView  = (TextView) view.findViewById(R.id.filename);

            if (null == fileItem.fileBitmap) {
                // Affiche le nom du fichier tant que l'image n'est pas disponible
                fileTextView.setText (fileItem.fileName);
                Log.d (LOG_TAG, "getView(" + fileItem.fileName + ", " + position + ") : fileBitmap=" + fileItem.fileBitmap);
            } else {
                // Cache le nom du fichier dès que le fichier est disponible
                fileTextView.setVisibility(View.GONE);
                // et affiche l'image téléchargée
                fileImageView = (ImageView) view.findViewById(R.id.fileImage);
                fileImageView.setImageBitmap(fileItem.fileBitmap);
                fileImageView.setAdjustViewBounds(true);
                Log.d (LOG_TAG, "getView(" + fileItem.fileName + ", " + position + ")");
            }

            return view;
        }
    }    
        
    // Objet représentant une image et le nom du fichier associé
    final static class FileListItem {
        public AsynchTaskDownloadImage  fileDownloader;
        public Bitmap                   fileBitmap;
        public String                   fileName;
    }
}
