package exceptions;

public class InvalidArgumentException extends ValidationException {
    public InvalidArgumentException(String message) {
        super(message);
    }
}