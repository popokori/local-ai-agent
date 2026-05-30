package mr.popo.localaiagent.common.exception;

/**
 * Ressource introuvable (404). Levée aussi quand la ressource existe mais
 * n'appartient pas à l'utilisateur courant — évite de leak l'existence.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException(entity + " not found: " + id);
    }
}
