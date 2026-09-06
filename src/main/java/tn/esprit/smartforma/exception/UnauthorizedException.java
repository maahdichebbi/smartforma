package tn.esprit.smartforma.exception;

/**
 * Thrown when authentication fails (unknown email, bad password, disabled account).
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
