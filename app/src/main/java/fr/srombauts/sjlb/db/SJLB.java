package fr.srombauts.sjlb.db;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Définitions pour le Content Provider et l'application SJLB
 */
public final class SJLB {
    // Interdiction de l'instanciation
    private SJLB() {}

    public static final String DATABASE_NAME        = "SJLB.db";
    public static final int    DATABASE_VERSION     = 9;
    
    
    /**
     * Clef des préférences
     */
    public static final class PREFS {
        // Interdiction de l'instanciation
        private PREFS() {}

        /**
         * La clef stockant le nom d'utilisateur (login)
         */
        public static final String LOGIN        = "PREF_LOGIN";

        /**
         * La clef stockant le mot de passe utilisateur (caché, mais non hashé donc en clair)
         */
        public static final String PASSWORD     = "PREF_PASSWORD";
        
        
        /**
         * La clef stockant le sens du mouvement nécessaire pour changer d'écran
         * 
         *  true : mouvements à la mode iPhone, qui semble "tirer" les écrans dans un sens ou dans l'autre  
         *  false : 1) mouvement de gauche à droite pour remonter des messages vers l'écran d'accueil, puis quitter,
         *          2) mouvement de droite à gauche pour redescendre vers le sujet qui a été vu en dernier 
         */
        public static final String SWITCH_SCREEN_DIRECTION  = "PREF_SWITCH_SCREEN_DIRECTION";
        
        
        /**
         * La clef activant le rafraîchissement automatique cyclique en tache de fond (si non, rafraîchissement uniquement au lancement de l'appli)
         */
        public static final String AUTO_UPDATE  = "PREF_AUTO_UPDATE";

        /**
         * La clef spécifiant la fréquence (la période) des rafraîchissement automatique le cas échéant
         */
        public static final String UPDATE_FREQ  = "PREF_UPDATE_FREQ";
        
        /**
         * La clef spécifiant la notification sonore
         */
        public static final String NOTIFICATION_SOUND    = "PREF_NOTIFICATION_SOUND";
        
        /**
         * La clef spécifiant la notification par vibreur
         */
        public static final String NOTIFICATION_VIBRATE    = "PREF_NOTIFICATION_VIBRATE";
        
        /**
         * La clef spécifiant la notification lumineuse
         */
        public static final String NOTIFICATION_LIGHT    = "PREF_NOTIFICATION_LIGHT";
        
    }
    
    
    /**
     * Table des messages privés
     */
    public static final class PM implements BaseColumns {
        // Interdiction de l'instanciation
        private PM() {}

        /**
         * Date en secondes depuis le 1er janvier 1970
         * <P>Type: INTEGER</P>
         */
        public static final String  DATE                = "date";

        /**
         * L'id de l'auteur/expéditeur du PM
         * <P>Type: INTEGER</P>
         */
        public static final String  AUTHOR_ID           = "author_id";

        /**
         * L'id du destinataire du PM
         * <P>Type: INTEGER</P>
         */
        public static final String  DEST_ID             = "dest_id";
        
        /**
         * Le texte du PM
         * <P>Type: TEXT</P>
         */
        public static final String  TEXT                = "text";

        /**
         * The default sort order for this table
         */
        public static final String  DEFAULT_SORT_ORDER  = _ID + " ASC";



        public static final String  TABLE_NAME   = "private_messages";

        public static final String  TABLE_CREATE = "create table " + TABLE_NAME + " ("
                                                    + _ID       + " integer primary key, "
                                                    + DATE      + " integer, "
                                                    + AUTHOR_ID + " integer, "
                                                    + DEST_ID   + " integer, "
                                                    + TEXT      + " text);";

        public static final String  TABLE_DROP   = "DROP TABLE IF EXISTS " + TABLE_NAME;
            
            
        /**
         * L'autorité identifiant de manière unique le content provider
         */        
        public static final String  AUTHORITY            = "fr.srombauts.sjlb.pm";

        /**
         * The content:// style URL for this table
         */
        public static final Uri     CONTENT_URI         = Uri.parse("content://" + AUTHORITY + "/pm");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String  CONTENT_TYPE        = "vnd.android.cursor.dir/sjlb.pm";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String  CONTENT_ITEM_TYPE   = "vnd.android.cursor.item/sjlb.pm";

        /**
         * Le matcher d'URI pour tous les PM
         */
        public static final String  MATCHER_ALL         = "pm";

        /**
         * Le matcher d'URI pour un PM
         */
        public static final String  MATCHER_ONE         = "pm/#";

        /**
         * Le matcher d'URI pour un live folder de PM
         */
        public static final String  MATCHER_LIVE_FOLDER = "live_folders/pm";
    }
    
    /**
     * Table des sujets du forum
     */
    public static final class Subj implements BaseColumns {
        // Interdiction de l'instanciation
        private Subj() {}

        /**
         * L'id de la catégorie de rattachement du sujet
         * <P>Type: INTEGER</P>
         */
        public static final String  CAT_ID              = "category_id";
        
        /**
         * L'id du groupe d'utilisateur ayant les droits sur ce sujet
         * <P>Type: INTEGER</P>
         */
        public static final String  GROUP_ID            = "group_id";
        
        /**
         * La date de dernier message du sujet
         * <P>Type: DATE</P>
         */
        public static final String  LAST_DATE           = "last_date";
        
        /**
         * Le texte du PM
         * <P>Type: INTEGER</P>
         */
        public static final String  TEXT                = "text";
        
        /**
         * Nombre de messages non lus dans le sujet
         * <P>Type: INTEGER</P>
         */
        public static final String  NB_UNREAD           = "nb_unread";

        /**
         * The default sort order for this table
         */
        public static final String  DEFAULT_SORT_ORDER  = LAST_DATE + " DESC";


        public static final String  TABLE_NAME   = "forum_subjects";

        public static final String  TABLE_CREATE = "create table " + TABLE_NAME + " ("
                                                    + _ID      + " integer primary key, "
                                                    + CAT_ID   + " integer, "
                                                    + GROUP_ID + " integer, "
                                                    + LAST_DATE+ " integer, "
                                                    + TEXT     + " text, "
                                                    + NB_UNREAD+ " integer);";

        public static final String  TABLE_DROP   = "DROP TABLE IF EXISTS " + SJLB.Subj.TABLE_NAME;


        /**
         * L'autorité identifiant de manière unique le content provider
         */        
        public static final String  AUTHORITY            = "fr.srombauts.sjlb.subj";
        /**
         * The content:// style URL for this table
         */
        public static final Uri     CONTENT_URI         = Uri.parse("content://" + AUTHORITY + "/subj");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String  CONTENT_TYPE        = "vnd.android.cursor.dir/sjlb.subj";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String  CONTENT_ITEM_TYPE   = "vnd.android.cursor.item/sjlb.subj";

        /**
         * Le matcher d'URI pour tous les Subj
         */
        public static final String  MATCHER_ALL         = "subj";

        /**
         * Le matcher d'URI pour un Subj
         */
        public static final String  MATCHER_ONE         = "subj/#";

        /**
         * Le matcher d'URI pour un live folder de Subj
         */
        public static final String  MATCHER_LIVE_FOLDER = "live_folders/subj";
    }


    /**
     * Table des messages du forum
     */
    public static final class Msg implements BaseColumns {
        // Interdiction de l'instanciation
        private Msg() {}

        /**
         * Date en secondes depuis le 1er janvier 1970
         * <P>Type: INTEGER</P>
         */
        public static final String  DATE                = "date";
        
        /**
         * Date d'édition du msg, en secondes depuis le 1er janvier 1970
         * <P>Type: INTEGER</P>
         */
        public static final String  DATE_EDIT           = "date_edit";
        
        /**
         * L'id de l'auteur/expéditeur du Msg
         * <P>Type: INTEGER</P>
         */
        public static final String  AUTHOR_ID           = "author_id";

        /**
         * L'id de le sujet auquel s'applique le Msg
         * <P>Type: INTEGER</P>
         */
        public static final String  SUBJECT_ID          = "subject_id";

        /**
         * Flag (transitoire) indiquant que le message n'a pas encore été lu
         * <P>Type: INTEGER</P>
         */
        public static final String  UNREAD              = "unread";
        public static final int     UNREAD_FALSE        = 0;
        public static final int     UNREAD_TRUE         = 1;
        public static final int     UNREAD_LOCALY       = 2;

        /**
         * Le texte du Msg
         * <P>Type: TEXT</P>
         */
        public static final String  TEXT                = "text";
        
        /**
         * The default sort order for this table
         */
        public static final String  DEFAULT_SORT_ORDER  = DATE + " ASC";
        public static final String  REVERSE_SORT_ORDER  = DATE + " DESC";
        

        public static final String  TABLE_NAME   = "forum_messages";

        public static final String TABLE_CREATE = "create table " + SJLB.Msg.TABLE_NAME + " ("
                                                    + _ID       + " integer primary key, "
                                                    + DATE      + " integer, "
                                                    + DATE_EDIT + " integer, "
                                                    + AUTHOR_ID + " integer, "
                                                    + SUBJECT_ID+ " integer, "
                                                    + UNREAD    + " integer, "
                                                    + TEXT      + " text);";

        public static final String TABLE_DROP   = "DROP TABLE IF EXISTS " + SJLB.Msg.TABLE_NAME;        


        /**
         * L'autorité identifiant de manière unique le content provider
         */        
        public static final String  AUTHORITY            = "fr.srombauts.sjlb.msg";
        /**
         * The content:// style URL for this table
         */
        public static final Uri     CONTENT_URI         = Uri.parse("content://" + AUTHORITY + "/msg");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String  CONTENT_TYPE        = "vnd.android.cursor.dir/sjlb.msg";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String  CONTENT_ITEM_TYPE   = "vnd.android.cursor.item/sjlb.msg";

        /**
         * Le matcher d'URI pour tous les Msg
         */
        public static final String  MATCHER_ALL         = "msg";

        /**
         * Le matcher d'URI pour un Msg
         */
        public static final String  MATCHER_ONE         = "msg/#";

        /**
         * Le matcher d'URI pour un live folder de Msg
         */
        public static final String  MATCHER_LIVE_FOLDER = "live_folders/msg";
    }

    
    /**
     * Table des fichiers attachés
     */
    public static final class File implements BaseColumns {
        // Interdiction de l'instanciation
        private File() {}

        /**
         * L'id du message auquel est rattaché le fichier attaché
         * <P>Type: INTEGER</P>
         */
        public static final String  MSG_ID              = "message_id";

        /**
         * Le nom du fichier attaché
         * <P>Type: TEXT</P>
         */
        public static final String  FILENAME            = "filename";

        /**
         * The default sort order for this table
         */
        public static final String  DEFAULT_SORT_ORDER  = FILENAME + " ASC";



        public static final String  TABLE_NAME   = "fichiers_attaches";

        public static final String  TABLE_CREATE = "create table " + TABLE_NAME + " ("
                                                    + MSG_ID    + " integer, "
                                                    + FILENAME  + " text);";

        public static final String  TABLE_DROP   = "DROP TABLE IF EXISTS " + TABLE_NAME;
            
            
        /**
         * L'autorité identifiant de manière unique le content provider
         */        
        public static final String  AUTHORITY            = "fr.srombauts.sjlb.file";

        /**
         * The content:// style URL for this table
         */
        public static final Uri     CONTENT_URI         = Uri.parse("content://" + AUTHORITY + "/file");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String  CONTENT_TYPE        = "vnd.android.cursor.dir/sjlb.file";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String  CONTENT_ITEM_TYPE   = "vnd.android.cursor.item/sjlb.file";

        /**
         * Le matcher d'URI pour tous les fichiers attachés
         */
        public static final String  MATCHER_ALL         = "file";

        /**
         * Le matcher d'URI pour un fichier attaché
         */
        public static final String  MATCHER_ONE         = "file/#";

        /**
         * Le matcher d'URI pour un live folder de fichier attaché
         */
        public static final String  MATCHER_LIVE_FOLDER = "live_folders/file";
    }
    
    /**
     * Table des utilisateurs du site
     */
    public static final class User implements BaseColumns {
        // Interdiction de l'instanciation
        private User() {}

        /**
         * Le pseudonyme/login de l'utilisateur
         * <P>Type: TEXT</P>
         */
        public static final String  PSEUDO              = "pseudo";
        
        /**
         * Le nom complet de l'utilisateur
         * <P>Type: TEXT</P>
         */
        public static final String  NAME                  = "name";
        
        /**
         * L'adresse de l'utilisateur
         * <P>Type: TEXT</P>
         */
        public static final String  ADDRESS               = "address";
        
        /**
         * Notes complémentaires de l'adresse de l'utilisateur (étage, métro, digicode...)
         * <P>Type: TEXT</P>
         */
        public static final String  NOTES                 = "notes";
        
        /**
         * Date de maj des infos, en secondes depuis le 1er janvier 1970
         * <P>Type: INTEGER</P>
         */
        public static final String  DATE_MAJ              = "date_maj";
        
        /**
         * Flag indiquant si l'utilisateur est actif sur le site SJLB
         * <P>Type: INTEGER</P>
         */
        public static final String  IS_ACTIVE              = "is_active";


        /**
         * The default sort order for this table
         */
        public static final String  DEFAULT_SORT_ORDER  = _ID + " ASC";

        /**
         * The default sort order for this table
         */
        public static final String  PSEUDO_SORT_ORDER  = PSEUDO + " ASC";


        public static final String  TABLE_NAME   = "users";

        public static final String  TABLE_CREATE = "create table " + TABLE_NAME + " ("
                                                    + _ID       + " integer primary key, "
                                                    + PSEUDO    + " text, "
                                                    + NAME      + " text, "
                                                    + ADDRESS   + " text, "
                                                    + NOTES     + " text, "
                                                    + DATE_MAJ  + " integer,"
                                                    + IS_ACTIVE + " integer);";

        public static final String  TABLE_DROP   = "DROP TABLE IF EXISTS " + TABLE_NAME;


        /**
         * L'autorité identifiant de manière unique le content provider
         */        
        public static final String  AUTHORITY            = "fr.srombauts.sjlb.user";
        
        /**
         * The content:// style URL for this table
         */
        public static final Uri     CONTENT_URI         = Uri.parse("content://" + AUTHORITY + "/user");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String  CONTENT_TYPE        = "vnd.android.cursor.dir/sjlb.user";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String  CONTENT_ITEM_TYPE   = "vnd.android.cursor.item/sjlb.user";

        /**
         * Le matcher d'URI pour tous les User
         */
        public static final String  MATCHER_ALL         = "user";

        /**
         * Le matcher d'URI pour un User
         */
        public static final String  MATCHER_ONE         = "user/#";

        /**
         * Le matcher d'URI pour un live folder de User
         */
        public static final String  MATCHER_LIVE_FOLDER = "live_folders/user";
    }    
}

