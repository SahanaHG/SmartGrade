/**
 * Custom checked exception used for every "bad input" situation
 * (empty name, duplicate ID, marks out of range, ...).
 * The message is always written so it can be shown directly to the user.
 */
public class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);
    }
}
