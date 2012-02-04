package fr.srombauts.sjlb;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Définitions pour le Content Provider et l'application SJLB
 */
public final class SJLB {
    // Interdiction de l'instanciation
    private SJLB() {}

    public static final String DATABASE_NAME        = "SJLB.db";
    public static final int    DATABASE_VERSION     = 2;
    
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
         * La clef activant le rafraichissement automatique cyclique en tache de fond (si non, rafraichissement uniquement au lancement de l'appli)
         */
        public static final String AUTO_UPDATE  = "PREF_AUTO_UPDATE";

        /**
         * La clef spécifiant la fréquence (la période) des rafraichissement automatique le cas échéant
         */
        public static final String UPDATE_FREQ  = "PREF_UPDATE_FREQ";
        
        /**
         * La clef spécifiant la notification sonore
         */
        public static final String NOTIFICATION_SOUND	= "PREF_NOTIFICATION_SOUND";
        
        /**
         * La clef spécifiant la notification par vibreur
         */
        public static final String NOTIFICATION_VIBRATE	= "PREF_NOTIFICATION_VIBRATE";
        
        /**
         * La clef spécifiant la notification lumineuse
         */
        public static final String NOTIFICATION_LIGHT	= "PREF_NOTIFICATION_LIGHT";
        
    }
    
    
    /**
     * Table des messages privés
     */
    public static final class PM implements BaseColumns {
        // Interdiction de l'instanciation
        private PM() {}

        public static final String  TABLE_NAME   = "private_messages";
        
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

        /**
         * The default sort order for this table
         */
        public static final String  DEFAULT_SORT_ORDER  = "modified DESC";

        /**
         * L'id du PM
         * <P>Type: INTEGER</P>
         */
        public static final String  ID                  = "_id";
        
        /**
         * Date en secondes depuis le 1er janvier 1970
         * <P>Type: INTEGER (System.curentTimeMillis()/1000)</P>
         */
        public static final String  DATE                = "date";

        /**
         * L'id de l'auteur/expéditeur du PM
         * <P>Type: INTEGER</P>
         */
        public static final String  AUTHOR_ID           = "author_id";

        /**
         * L'auteur/expéditeur du PM
         * <P>Type: TEXT</P>
         */
        public static final String  AUTHOR              = "author";

        /**
         * Le texte du PM
         * <P>Type: INTEGER</P>
         */
        public static final String  TEXT                = "text";
    }
    

    /**
     * Table des messages du forum
     */
    public static final class Msg implements BaseColumns {
        // Interdiction de l'instanciation
        private Msg() {}

        public static final String  TABLE_NAME   = "forum_messages";

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
        
        /**
         * The default sort order for this table
         */
        public static final String  DEFAULT_SORT_ORDER  = "modified DESC";

        /**
         * L'id du PM
         * <P>Type: INTEGER</P>
         */
        public static final String  ID                  = "_id";
        
        /**
         * Date en secondes depuis le 1er janvier 1970
         * <P>Type: INTEGER (System.curentTimeMillis()/1000)</P>
         */
        public static final String  DATE                = "date";

        /**
         * L'auteur/expéditeur du PM
         * <P>Type: TEXT</P>
         */
        public static final String  AUTHOR              = "author";

        /**
         * Le texte du PM
         * <P>Type: INTEGER</P>
         */
        public static final String  TEXT                = "text";
    }

    /**
     * Table des utilisateurs du site
     */
    public static final class User implements BaseColumns {
        // Interdiction de l'instanciation
        private User() {}

        public static final String  TABLE_NAME   = "users";

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
        
        /**
         * The default sort order for this table
         */
        public static final String  DEFAULT_SORT_ORDER  = "modified DESC";

        /**
         * L'id de l'utilisateur
         * <P>Type: INTEGER</P>
         */
        public static final String  ID                  = "_id";
        
        /**
         * Le pseudonyme/login de l'utilisateur
         * <P>Type: TEXT</P>
         */
        public static final String  PSEUDO              = "pseudo";
    }    
}

