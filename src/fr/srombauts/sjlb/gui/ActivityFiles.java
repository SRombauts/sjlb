package fr.srombauts.sjlb.gui;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import fr.srombauts.sjlb.ApplicationSJLB;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.service.AsynchTaskDownloadImage;
import fr.srombauts.sjlb.service.CallbackImageDownload;


/**
 * Activité présentant la liste des sujets de la catégorie sélectionnée
 * @author 22/08/2010 SRombauts
 */
public class ActivityFiles extends ActivityTouchListener implements OnItemClickListener, CallbackImageDownload {
    private static final String LOG_TAG = "ActivityFiles";
    
    public static final String  START_INTENT_EXTRA_MSG_ID           = "MessageId";
    public static final String  START_INTENT_EXTRA_MSG_TEXT         = "MessageText";
    public static final String  START_INTENT_EXTRA_SUBJ_LABEL       = "SubjectLabel";
    
    private static final String URI_REPERTOIRE_FICHIERS_ATTACHES    = "http://www.sjlb.fr/FichiersAttaches/";

    private FileListItemAdapter mAdapter                = null;
    private FileListItem[]      mFileListItem;

    private long                mSelectedMessageId      = 0;
    private String              mSelectedMessageText    = "";
    private String              mSelectedSubjectLabel   = "";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d (LOG_TAG, "onCreate...");
        
        // Layout de l'activité
        setContentView(R.layout.file_list);
        
        // Récupère l'éventuel paramètre de lancement (id de du sujet du forum sélectionnée)
        Intent startIntent = getIntent();
        if (null != startIntent.getExtras())
        {
            mSelectedMessageId     = startIntent.getExtras().getLong  (START_INTENT_EXTRA_MSG_ID);
            mSelectedMessageText   = startIntent.getExtras().getString(START_INTENT_EXTRA_MSG_TEXT);
            mSelectedSubjectLabel  = startIntent.getExtras().getString(START_INTENT_EXTRA_SUBJ_LABEL);
            Log.i(LOG_TAG, "SelectedMessage (" + mSelectedMessageId + ")");
        }
        
        // Map la description du sujet pour la renseigner dans le titre
        setTitle(mSelectedSubjectLabel);        

        // Récupère un curseur sur les données (les fichiers attachés) en filtrant sur l'id du message sélectionné
        final String[] columns = {SJLB.File.FILENAME};
        Cursor cursor = managedQuery(SJLB.File.CONTENT_URI,
                                     columns, // ne récupère que le filename
                                     SJLB.File.MSG_ID + "=" + mSelectedMessageId,
                                     null, null);

        // Constitue le tableau de fichiers ;
        mFileListItem = new FileListItem[cursor.getCount()+1];
        
        // commence par un item vide réservé à l'affichage du texte du message (à la position 0)
        FileListItem msgTextItem = new FileListItem();
        mFileListItem[0] = msgTextItem;
        
        int nbFiles = cursor.getCount();
        //Log.v(LOG_TAG, "mSelectedMessageId=" + mSelectedMessageId + " cursor.getCount()=" + cursor.getCount());
        for (int position = 1; position < (nbFiles+1); position++) {
            // TODO SRombauts : filtrer sur les extensions d'image reconnues uniquement !
            if (cursor.moveToPosition(position-1)) {
                // Récupère le nom du fichier
                FileListItem fileItem = new FileListItem();
                fileItem.fileName = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.File.FILENAME));
                // Récupère l'image éventuellement précédemment téléchargée durant la vie de l'application (avant changement d'orientation par exemple) 
                fileItem.fileBitmap = ((ApplicationSJLB)getApplication ()).getFichiersAttaches(fileItem.fileName);
                if (null == fileItem.fileBitmap) {
                    // Sinon, lance ici le téléchargement du fichier en tache de fond
                    fileItem.fileDownloader = new AsynchTaskDownloadImage(this);
                    Log.i(LOG_TAG, "ImageDownloader.execute(" + fileItem.fileName + ", " + position + ")");
                    fileItem.fileDownloader.execute(URI_REPERTOIRE_FICHIERS_ATTACHES + fileItem.fileName,
                                                    Long.toString(position));
                }
                // insert l'item dans la liste des fichiers
                mFileListItem[position] = fileItem;
            }
        }

        // Créer l'adapteur entre la liste de fichiers et le layout et les informations sur le mapping des colonnes
        mAdapter = new FileListItemAdapter( this,
                                            R.layout.file_item,
                                            mFileListItem);

        ListView    fileListView = (ListView)findViewById(R.id.file_listview);
        fileListView.setAdapter (mAdapter);

        // Enregistre les listener d'IHM que la classe implémente
        fileListView.setOnTouchListener(this);

        // Enregistre le menu contextuel de la liste
        fileListView.setOnItemClickListener(this);
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
    
    protected void onDestroy() {
        super.onDestroy();
        Log.i (LOG_TAG, "onDestroy... : cancel downloads");
        // Interrompt tout chargement éventuellement en cours
        int nbFiles = mFileListItem.length;
        for (int position = 0; position < nbFiles; position++) {
            if (null != mFileListItem[position].fileDownloader) {
                mFileListItem[position].fileDownloader.cancel();
            }
        }
    }
    
    /**
     * @brief Click sur une image affichée : la récupère dans la galerie du téléphone (si pas déjà présente) et l'affiche dans ce contexte
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mFileListItem[position].fileBitmap) {
            final String[]  projection  = { MediaStore.Images.Media._ID };
            final String    selection   = MediaStore.Images.Media.TITLE + "=?";
            final String[]  args        = { mFileListItem[position].fileName };
            final String    imageUrl;
            Cursor cursor = MediaStore.Images.Media.query(getContentResolver(),
                                                          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                          projection, selection, args, null);
            if (null != cursor) {
                //Log.v(LOG_TAG, "projection[0]=" + projection[0] + " selection=" + selection + " args[0]=" + args[0] + ": count=" + cursor.getCount());
                if (0 < cursor.getCount()) {
                    cursor.moveToFirst();
                    long imageId = cursor.getLong(0);                
                    imageUrl = MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/"+ imageId;
                    Log.d(LOG_TAG, "Photo déjà dans la galerie (" + imageUrl + ")");
                    Toast.makeText(this, "Photo déjà dans la galerie", Toast.LENGTH_SHORT).show();
                } else {
                    // Si pas déjà sauvegardée, l'insert ici :
                    imageUrl = saveImage(mFileListItem[position].fileBitmap, mFileListItem[position].fileName);
                }
                cursor.close();
                if (null != imageUrl) {
                    Log.i(LOG_TAG, "startActivity(ACTION_VIEW, " + imageUrl + ")");
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(imageUrl), "image/*");
                    startActivity(intent);                
                } else {
                    Log.e(LOG_TAG, "onItemClick(" + mFileListItem[position].fileBitmap + ") : no image !");
                }
            }
        }        
    }
    
    // Enregistre dans l'espace dédiés aux médias partagés (sd card ou zone de flash réservé)
    private String saveImage(Bitmap aBitmap, String aFilename) {
        String  imageUrl = null;
        try {
            String path = Environment.getExternalStorageDirectory().toString() + "/sjlb/";
            new File(path).mkdir();
            File file = new File(path, aFilename);
            Log.d(LOG_TAG, "file=" + file);
            FileOutputStream out = new FileOutputStream(file);
            aBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            imageUrl = MediaStore.Images.Media.insertImage(getContentResolver(),
                                                           file.getAbsolutePath(),
                                                           file.getName(),
                                                           file.getName());
            // Indique au scanner de média la présence d'un nouveau fichier, pour le rendre immédiatement disponible dans la galerie de photo
            MyMediaScannerConnectionClient  mediaClient = new MyMediaScannerConnectionClient();
            MediaScannerConnection          mediaScanner= new MediaScannerConnection(this, mediaClient);
            mediaClient.startScanner(mediaScanner, file.getAbsolutePath());

            Log.i(LOG_TAG, "saveImage(" + file + ")");
            Toast.makeText(getApplicationContext(), "Photo ajoutée à la galerie SJLB", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return imageUrl;
    }
    
    // Indique au scanner de média la présence d'un nouveau fichier, pour le rendre immédiatement disponible dans la galerie de photo
    class MyMediaScannerConnectionClient implements MediaScannerConnectionClient {

        private MediaScannerConnection  mScanner;
        private String                  mPath;

        public void startScanner(MediaScannerConnection aScanner, String aPath) {
            Log.d(LOG_TAG, "startScanner(" + aPath + ")");
            mScanner = aScanner;
            mPath    = aPath;
            mScanner.connect();
        }
        
        @Override
        public void onMediaScannerConnected() {
            Log.v(LOG_TAG, "onMediaScannerConnected(" + mPath + ")");
            mScanner.scanFile(mPath, null);

        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            Log.i(LOG_TAG, "Media Scan completed on file: path=" + path + " uri=" + uri);
            mScanner.disconnect();
            mScanner = null;
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
            // Met le bitmap dans la liste,
            fileItem.fileBitmap = aBitmap;
            // et le mémorise en plus au niveau de l'application pour pouvoir le restaurer rapidement (changement d'orientation, ou partage entre activités)
            ((ApplicationSJLB)getApplication ()).setFichiersAttaches(fileItem.fileName, aBitmap);
            Log.i (LOG_TAG, "onImageDownloaded(" + fileItem.fileName + ", " + aPosition + ")=" + fileItem.fileBitmap);
            mAdapter.notifyDataSetChanged();
        } else {
            Log.e (LOG_TAG, "onImageDownloaded(" + fileItem.fileName + ", " + aPosition + ")= not an image !?");
        }
    }
    
    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activité pour retour à la liste des messages");
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
            TextView    fileMsgTextView;
            ImageView   fileImageView;
            TextView    fileTextView;
           
            // Construit ou récupère la vue appropriée
            if (convertView == null) {
                Log.v (LOG_TAG, "inflate");
                view = mInflater.inflate(R.layout.file_item, parent, false);
            } else {
                view = convertView;
            }

            // Récupère l'item correspondant à la position
            FileListItem fileItem = getItem(position);
                    
            fileMsgTextView = (TextView)  view.findViewById(R.id.file_msg_text);
            fileImageView   = (ImageView) view.findViewById(R.id.file_image);
            fileTextView    = (TextView)  view.findViewById(R.id.file_name);

            if (0 == position) {
                // Cas particulier de l'item en position 0 : utilisé pour afficher le texte du message ;
                fileMsgTextView.setText (mSelectedMessageText);
                fileMsgTextView.setVisibility(View.VISIBLE);
                fileTextView.setVisibility(View.GONE);
                fileImageView.setVisibility(View.GONE);
                Log.v (LOG_TAG, "getView(" + position + ")");
            } else {
                // dans les autres cas, cache le champ texte du message
                fileMsgTextView.setVisibility(View.GONE);
                
                if (null == fileItem.fileBitmap) {
                    // Affiche le nom du fichier tant que l'image n'est pas disponible
                    fileTextView.setText(fileItem.fileName);
                    fileTextView.setVisibility(View.VISIBLE);
                    // et affiche l'icône de fichier par défaut
                    fileImageView.setImageDrawable(getResources().getDrawable(R.drawable.file_icon));
                    fileImageView.setVisibility(View.VISIBLE);
                    Log.v (LOG_TAG, "getView(" + fileItem.fileName + ", " + position + ") : fileBitmap=" + fileItem.fileBitmap);
                } else {
                    // Cache le nom du fichier dès que le fichier est disponible
                    fileTextView.setVisibility(View.GONE);
                    // et affiche l'image téléchargée
                    fileImageView.setVisibility(View.VISIBLE);
                    fileImageView.setImageBitmap(fileItem.fileBitmap);
                    fileImageView.setAdjustViewBounds(true);
                    Log.v (LOG_TAG, "getView(" + fileItem.fileName + ", " + position + ")");
                }
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
