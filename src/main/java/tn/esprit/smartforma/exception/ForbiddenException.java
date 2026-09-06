package tn.esprit.smartforma.exception;

/**
 * Thrown when the authenticated user is not allowed to access a resource.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
