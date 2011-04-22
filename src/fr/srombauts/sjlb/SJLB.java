package fr.srombauts.sjlb;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Définitions pour le Content Provider et l'application SJLB
 */
public final class SJLB {
    // Interdiction de l'instanciation
    private SJLB() {}

    public static final String AUTHORITY = "fr.srombauts.sjlb";

    public static final String DATABASE_NAME       = "SJLB.db";
    public static final int    DATABASE_VERSION    = 1;
    
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
        
    }
    
    
    /**
     * Table des messages privés
     */
    public static final class PM implements BaseColumns {
        // Interdiction de l'instanciation
        private PM() {}

        public static final String TABLE_NAME   = "private_messages";
        
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
     * Table des messages du forum
     */
    public static final class Msg implements BaseColumns {
        // Interdiction de l'instanciation
        private Msg() {}

        public static final String TABLE_NAME   = "forum_messages";

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
}

