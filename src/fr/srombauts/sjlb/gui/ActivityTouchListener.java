package fr.srombauts.sjlb.gui;

import android.app.Activity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import fr.srombauts.sjlb.model.PrefsInterface;


/**
 * Ajoute à une activité un gestionnaire d'interface tactile.
 * 
 * Appelle une callback spécifique à chaque type de gestion reconnu,
 * en faisant entrer les préférences permettant d'inverser gauche et droite selon l'effet préféré par l'utilisateur. 
 * 
 * @author 27/08/2010 SRombauts
 */
public class ActivityTouchListener extends Activity implements OnTouchListener {
    private static final String LOG_TAG = "ActivityTouchListener";
    
    protected float         mTouchStartPositionX    = 0;
    protected float         mTouchStartPositionY    = 0;
    
    private   float         mSensibility            = (float)0.4;   // Ampleur (seuil) du mouvement nécessaire pour déclencher une action
    
    /**
     * Callback d'évènements générique, mutualisée entre les différentes activités de l'application
     */
    public boolean onTouch(View aView, MotionEvent aMotionEvent) {
              boolean   bActionTraitee  = false;
        final int       touchAction     = aMotionEvent.getAction();
        final float     touchX          = aMotionEvent.getX();
        final float     touchY          = aMotionEvent.getY();
        
        switch (touchAction)
        {
            case MotionEvent.ACTION_DOWN: {
                //Log.d (LOG_TAG, "onTouch (ACTION_DOWN) : touch (" + touchX + ", " + touchY + ")");
                mTouchStartPositionX = touchX;
                mTouchStartPositionY = touchY;
                break;
            }
            case MotionEvent.ACTION_UP: {
                // Calcul l'ampleur du mouvement par rapport aux dimensions de l'écran
                final Display display = getWindowManager().getDefaultDisplay();
                final float proportionalDeltaX = (touchX - mTouchStartPositionX) / (float)display.getWidth();
                final float proportionalDeltaY = (touchY - mTouchStartPositionY) / (float)display.getHeight();

                //Log.d (LOG_TAG, "onTouch (ACTION_UP) : touch (" + touchX + ", " + touchY + ")");
                //Log.d (LOG_TAG, "onTouch: deltas proportionnels : (" + proportionalDeltaX + ", " + proportionalDeltaY + ")");
                
                // Teste si le mouvement correspond à un mouvement horizontal (X) ou vertical (Y)
                if (Math.abs(proportionalDeltaX) > Math.abs(proportionalDeltaY)) {
                    // Teste si le mouvement correspond à un mouvement franc, d'ampleur suffisante en regard de la largeur de l'écran (> 40%)
                    if (Math.abs(proportionalDeltaX) > mSensibility) {
                        // Teste le sens du mouvement horizontal, et l'inverse éventuellement selon les préférences
                        if (   (proportionalDeltaX > 0) ==  (false == PrefsInterface.inverseSwitchScreenDirection(this)) )   {
                            Log.v(LOG_TAG, "onRightGesture(): x=" + (touchX - mTouchStartPositionX) + " y=" + (touchY - mTouchStartPositionY));
                            bActionTraitee = onRightGesture ();
                        }
                        else {
                            Log.v(LOG_TAG, "onLeftGesture(): x=" + (touchX - mTouchStartPositionX) + " y=" + (touchY - mTouchStartPositionY));
                            bActionTraitee = onLeftGesture ();
                        }
                    }
                } else {
                    // Teste si le mouvement correspond à un mouvement franc, d'ampleur suffisante en regard de la hauteur de l'écran (> 40%)
                    if (Math.abs(proportionalDeltaY) > mSensibility) {
                        // Teste le sens du mouvement vertical (sans inversion possible)
                        if (proportionalDeltaY > 0)   {
                            Log.v(LOG_TAG, "onUpGesture(): x=" + (touchX - mTouchStartPositionX) + " y=" + (touchY - mTouchStartPositionY));
                            bActionTraitee = onUpGesture ();
                        }
                        else {
                            Log.v(LOG_TAG, "onDownGesture(): x=" + (touchX - mTouchStartPositionX) + " y=" + (touchY - mTouchStartPositionY));
                            bActionTraitee = onDownGesture ();
                        }
                    }
                }
                break;
            }
            default: {
                //Log.d (LOG_TAG, "onTouch autre (" + touchAction  + ") : touch (" + touchX + ", " + touchY + ")");
            }
        }

        // Si on n'a pas déjà traité l'action, on passe la main à la Vue sous-jacente
        if (false == bActionTraitee) {
            aView.onTouchEvent(aMotionEvent);
        }
        
        // Si on retourne false, on n'est plus notifié des évènements suivants
        return true;
    }
    
    /**
     *  Permet de régler l'ampleur (seuil) du mouvement nécessaire pour déclencher une action    
     */
    protected void setSensibility (float aSensibility) {
        mSensibility = aSensibility;
    }
    
    /**
     *  A surdéfinir pour traiter un mouvement vers la gauche (éventuellement inversé par les préférences)
     *  
     * @return true pour indiquer si une action a été prise, et qu'il ne faut pas donner la main à la vue sous-jacente
     */
    protected boolean onLeftGesture () {
        return false;
    }
    
    /**
     *  A surdéfinir pour traiter un mouvement vers la droite (éventuellement inversé par les préférences)
     *  
     * @return true pour indiquer si une action a été prise, et qu'il ne faut pas donner la main à la vue sous-jacente
     */
    protected boolean onRightGesture () {
        return false;
    }
    
    /**
     *  A surdéfinir pour traiter un mouvement vers le haut
     *  
     * @return true pour indiquer si une action a été prise, et qu'il ne faut pas donner la main à la vue sous-jacente
     */
    protected boolean onUpGesture () {
        return false;
    }
    
    /**
     *  A surdéfinir pour traiter un mouvement vers le base
     *  
     * @return true pour indiquer si une action a été prise, et qu'il ne faut pas donner la main à la vue sous-jacente
     */
    protected boolean onDownGesture () {
        return false;
    }
    
}
