package fr.srombauts.sjlb.gui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
public class ActivityFiles extends ActivityTouchListener {
    private static final String LOG_TAG = "ActivityMsg";
    
    public static final String  START_INTENT_EXTRA_MSG_ID       = "MessageId";

    private Cursor              mCursor             = null;
    private FileListItemAdapter mAdapter            = null;
    static private ListView     mFileListView       = null; // TODO SRombauts "static" pour être accéder depuis la CallbackImageDownload
    
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
        mCursor = managedQuery( SJLB.File.CONTENT_URI,
                columns, // ne récupère que le filename
                                SJLB.File.MSG_ID + "=" + mSelectedMessageId,
                                null, null);

        // Constitue le tableau de fichiers
        FileListItem [] arrayFileListItem = new FileListItem [mCursor.getCount()];
        int count = mCursor.getCount();
        //Log.d(LOG_TAG, "msgId " + msgId + " mCursor.getCount()=" + mCursor.getCount());
        for (int i=0; i<count ;i++)
        {
            // Récupère le nom du fichier
            boolean bMoved = mCursor.moveToPosition(i);
            if (bMoved) {
                FileListItem file = new FileListItem();
                file.filename = mCursor.getString(mCursor.getColumnIndexOrThrow(SJLB.File.FILENAME));
                arrayFileListItem[i] = file;
            }
        }

        // Créer l'adapteur entre la liste de fichiers et le layout et les informations sur le mapping des colonnes
        mAdapter = new FileListItemAdapter( this,
                                            R.layout.file_item,
                                            arrayFileListItem);

        mFileListView = (ListView)findViewById(R.id.activity_listview);
        mFileListView.setAdapter (mAdapter);

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
    
    /**
     * Création du menu général
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.msg, menu);
        return true;
    }
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activité pour retour à la liste des message");
        finish ();
        return true;
    }

    // NOTE SRombauts : pas besoin de onRightGesture()
    
    
    // TODO SRombauts : documentation !
    private class FileListItemAdapter extends ArrayAdapter<FileListItem> {

        private FileListItem[] mListeItem;

        public FileListItemAdapter(Context context, int textViewResourceId, FileListItem[] items) {
            super(context, textViewResourceId, items);
            mListeItem = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Log.d (LOG_TAG, "getView(" + position + "," + convertView + "," + parent + ")" );

            View view = convertView;
            if (view == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.file_item, null);

                // TODO SRombauts : tests en cours
                FileListItem it = mListeItem[position];
                if (it != null) {
                    Log.d (LOG_TAG, "getView(" + it.filename + ")" );
                    
                    it.imageViewFile = (ImageView) view.findViewById(R.id.fileImage);
                    // TODO SRombauts : tente de réserver un espace carré pour l'image
                    //it.imageViewFile.getLayoutParams().width = it.imageViewFile.getHeight();
                    
                    // Lance ici le téléchargement du fichier en tache de fond (SSI il s'agit bien d'une image !)
                    if (null == it.imageBitmap) {
                        AsynchTaskDownloadImage ImageDownloader = new AsynchTaskDownloadImage(it);
                        ImageDownloader.execute(getString(R.string.sjlb_fichiers_attaches) + it.filename);
                    }
                    
                    // Affiche le nom du fichier
                    it.textViewFile  = (TextView) view.findViewById(R.id.filename);
                    it.textViewFile.setText (it.filename);
                }
                // Mémorise dans la vue les infos sous-jacentes
                view.setTag (it);

            }
            return view;
        }
    }    
        
    // Objet représentant une image et le nom du fichier associé
    final static class FileListItem implements CallbackImageDownload {
        public ImageView    imageViewFile;
        public TextView     textViewFile;
        public Bitmap       imageBitmap;
        public String       filename;
        
        public void onImageDownloaded(Bitmap aBitmap) {
            if (null != aBitmap) {
                Log.d (LOG_TAG, "onImageDownloaded(" + aBitmap + ")" );
                textViewFile.setVisibility(TextView.GONE);
                imageViewFile.setImageBitmap(aBitmap);
                imageViewFile.setAdjustViewBounds(true);
                imageViewFile.setHorizontalScrollBarEnabled(true);
                //imageViewFile.getSuggestedMinimumHeight(); 
                /* TODO SRombauts : tests en cours
                imageViewFile.invalidate ();
                imageViewFile.requestLayout();
                imageViewFile.getParent().requestLayout();
                imageViewFile.getParent().recomputeViewAttributes (imageViewFile);
                mMsgListView.invalidate();
                mMsgListView.requestLayout();
                */
            } else {
                Log.e (LOG_TAG, "onImageDownloaded(" + aBitmap + ")" );
            }
        }
        
    }
}
