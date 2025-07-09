package crypto;

import constants.ArrayUtils;
import constants.Key;
import exceptions.EncryptionException;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class HashFunction {
    private static final String ENCRYPTION_STANDARD = "SHA-512";
    public static BigInteger computeHash(@NotNull BigInteger input, boolean reverse) throws EncryptionException {
        return computeHash(CryptoUtil.bigIntegerToByte(input, Key.KEY_SIZE,false), reverse);
    }

    public static BigInteger computeHash(byte[] bytes, boolean reverse) throws EncryptionException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ENCRYPTION_STANDARD);
            if (reverse) {
                return new BigInteger(1, ArrayUtils.reverseByteArray(digest.digest(bytes), false));
            } else {
                return new BigInteger(1, digest.digest(bytes));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(String.format(
                    "No such encryption algorithm: %s\n",
                    ENCRYPTION_STANDARD
            ));
        }
    }

    public static BigInteger computeHashReversed(byte[] bytes) throws EncryptionException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ENCRYPTION_STANDARD);
            return new BigInteger(1, ArrayUtils.reverseByteArray(digest.digest(ArrayUtils.reverseByteArray(bytes, false)), false));
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(String.format(
                    "No such encryption algorithm: %s\n",
                    ENCRYPTION_STANDARD
            ));
        }
    }
}