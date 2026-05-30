package mr.popo.localaiagent.common.exception;

/**
 * Action interdite (403). Pour les cas où l'utilisateur est authentifié mais
 * n'a pas le droit sur la ressource.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
