package mlvisualizer;

/** Input error that the GUI can show without exposing a stack trace. */
public class InvalidParameterException extends IllegalArgumentException {
    public InvalidParameterException(String message) {
        super(message);
    }
}
