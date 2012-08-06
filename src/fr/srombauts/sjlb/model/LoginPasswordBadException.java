package fr.srombauts.sjlb.model;


/**
 * Exception envoyée is login et mot de passe ne sont pas cohérents : ie, explicitement refusés par le serveur.
 * @author Seb
 */
@SuppressWarnings("serial")
public class LoginPasswordBadException extends Exception {
}