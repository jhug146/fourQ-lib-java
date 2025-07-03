package exceptions;

public class InvalidArgumentException extends EncryptionException {
    public InvalidArgumentException(String message) {
        super(message);
    }
}