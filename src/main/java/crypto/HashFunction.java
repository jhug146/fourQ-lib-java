package crypto;

import exceptions.EncryptionException;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class HashFunction {
    private static final String ENCRYPTION_STANDARD = "SHA-512";
    public static BigInteger computeHash(BigInteger input) throws EncryptionException {
        return computeHash(input.toByteArray());
    }

    public static BigInteger computeHash(byte[] bytes) throws EncryptionException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ENCRYPTION_STANDARD);
            return new BigInteger(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(String.format(
                    "No such encryption algorithm: %s\n",
                    ENCRYPTION_STANDARD
            ));
        }
    }
}
