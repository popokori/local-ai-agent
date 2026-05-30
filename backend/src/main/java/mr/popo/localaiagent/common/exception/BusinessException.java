package mr.popo.localaiagent.common.exception;

/**
 * Erreur métier (400). Le message est exposé à l'utilisateur.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
