package fr.srombauts.sjlb;


/**
 * Exception envoyée is login et mot de passe ne sont pas cohérents : ie, explicitement refusés par le serveur.
 * @author Seb
 */
@SuppressWarnings("serial")
class LoginPasswordBadException extends Exception {
}