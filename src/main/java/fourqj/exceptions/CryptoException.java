package fourqj.exceptions;

public class CryptoException extends org.bouncycastle.crypto.CryptoException {
    public CryptoException(String message, Exception e) {
        super(message, e);
    }

    public CryptoException(String message) {
        super(message, new EncryptionException(""));
    }
}