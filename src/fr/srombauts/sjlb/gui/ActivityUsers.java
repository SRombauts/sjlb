package fr.srombauts.sjlb.gui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import fr.srombauts.sjlb.ApplicationSJLB;
import fr.srombauts.sjlb.R;
import fr.srombauts.sjlb.db.SJLB;
import fr.srombauts.sjlb.model.UserContactDescr;


/**
 * Activité présentant la liste des messages privés
 * @author 10/08/2012 SRombauts
 */
public class ActivityUsers extends ActivityTouchListener {
    private static final String LOG_TAG = "ActivityUsers";
    
    private Cursor              mCursor             = null;
    private UserListItemAdapter mAdapter            = null;
    private ListView            mUsersListView      = null;
    
    //private long                mSelectedUserId     = 0;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout de l'activité
        setContentView(R.layout.activity_list);
        setTitle(getString(R.string.user_description));
        
        // Récupère un curseur sur les données (les membres) 
        mCursor = managedQuery( SJLB.User.CONTENT_URI, null,
                                null,
                                null, null);

        // Créer l'adapteur entre le curseur et le layout et les informations sur le mapping des colonnes
        mAdapter = new UserListItemAdapter( this,
                                          R.layout.user_item,
                                          mCursor);
        
        mUsersListView = (ListView)findViewById(R.id.activity_listview);
        mUsersListView.setAdapter (mAdapter);
        
        // Enregistre le menu contextuel de la liste
        registerForContextMenu (mUsersListView);        
        
        // Enregistre les listener d'IHM que la classe implémente        
        mUsersListView.setOnTouchListener(this);
        mUsersListView.getRootView().setOnTouchListener(this);
    }
    
    protected void onResume () {
        super.onResume();
    }

    @Override
    protected boolean onLeftGesture () {
        Log.i (LOG_TAG, "onTouch: va a l'ecran de gauche... quitte l'activite pour retour à la liste des sujets");
        finish ();
        return true;
    }
    
    /**
     * Création du menu général
     *
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user, menu);
        return true;
    }
    
    /**
     * Sur sélection dans le menu général
     *
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case (R.id.menu_new_user): {
                Intent intent = new Intent(this, ActivityNewPrivateMessage.class);
                startActivity(intent);
                break;
            }
            case (R.id.menu_update): {
                // Toast notification de début de rafraîchissement
                Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
                // TODO voir si c'est la meilleurs manière de faire : donnerait plus de contrôle si l'on pouvait faire un accès direct à la AsynchTask...
                // TODO SRombauts : rafraîchir la liste à l'échéance de la tache de rafraîchissement
                //            => "suffirait" que le service lance un Intent sur cette Activity, comme lorsqu'on clique sur une notification 
                IntentReceiverStartService.startService (this, LOG_TAG);
                mCursor.requery();
                break;            }
            case (R.id.menu_delete_all_user): {
                showDialog(DIALOG_ID_PM_DELETE_ALL);
                break;
            }
            case (R.id.menu_prefs): {
                Intent intent = new Intent(this, ActivityPreferences.class);
                startActivity(intent);
                break;
            }
            case (R.id.menu_quit): {
                finish ();
                break;
            }
            default:
                return false;
        }
        return true;
    }

    /**
     * Création du menu contextuel
     *
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_context, menu);
    }

    /**
     * Sur sélection d'un choix du menu contextuel
     *
    public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      switch (item.getItemId()) {
          case R.id.menu_user_answer:
              answerPM(mCursor.getInt(mCursor.getColumnIndexOrThrow(SJLB.User.AUTHOR_ID)));
              return true;
          case R.id.menu_user_delete:
              mSelectedPmId = info.id;
              showDialog(DIALOG_ID_PM_DELETE_ONE);
              return true;
          default:
              return super.onContextItemSelected(item);
      }
    }
    */


    // Adaptateur mappant les données du curseur dans des objets du cache du pool d'objets View utilisés par la ListView
    private final class UserListItemAdapter extends ResourceCursorAdapter {
        public UserListItemAdapter(Context context, int layout, Cursor cursor) {
            super(context, layout, cursor);
        }

        // Met à jour avec un nouveau contenu un objet de cache du pool de view utilisées par la ListView 
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final UserListItemCache  cache = (UserListItemCache) view.getTag();
            
            // Fixe les infos de l'utilisateur
            String  pseudo = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.User.PSEUDO)); 
            cache.pseudoView.setText(pseudo);
            String  name = cursor.getString(cursor.getColumnIndexOrThrow(SJLB.User.NAME));
            cache.nameView.setText(name);
            // TODO SRombauts :
            String  addresse = "";//cursor.getString(cursor.getColumnIndexOrThrow(SJLB.User.TEXT));
            cache.addressView.setText(addresse);

            // Récupère le contact éventuellement associé à l'utilisateur (Uri et photo)
            ApplicationSJLB appSJLB = (ApplicationSJLB)getApplication ();
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(SJLB.User._ID));
            UserContactDescr user = appSJLB.mUserContactList.get(userId);
            // Fixe la barre de QuickContact
            cache.quickContactView.assignContactUri(user.lookupUri);
            
            // Affiche la photo du contact si elle existe (sinon petite icône de robot par défaut)
            if (null != user.photo) {
                cache.quickContactView.setImageBitmap(user.photo);
            } else {
                cache.quickContactView.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        // Création d'une nouvelle View et de son objet de cache (vide) pour le pool
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            
            // Construction d'un nouvel objet de cache
            UserListItemCache cache = new UserListItemCache();
            // et binding sur les View décrites par le Layout
            cache.quickContactView  = (QuickContactBadge)   view.findViewById(R.id.userBadge);
            cache.pseudoView        = (TextView)            view.findViewById(R.id.userPseudo);
            cache.nameView          = (TextView)            view.findViewById(R.id.userName);
            cache.addressView       = (TextView)            view.findViewById(R.id.userAddress);
            // enregistre cet objet de cache
            view.setTag(cache);

            return view;
        }
    }

    // Objet utilisé comme cache des données d'une View, dans un pool d'objets utilisés par la ListView
    final static class UserListItemCache {
        public QuickContactBadge    quickContactView;
        public TextView             pseudoView;
        public TextView             nameView;
        public TextView             addressView;
    }    
}