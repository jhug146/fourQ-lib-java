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
    public static byte[] computeHash(@NotNull BigInteger input, boolean reverse) throws EncryptionException {
        return computeHash(ArrayUtils.bigIntegerToByte(input, Key.KEY_SIZE,false), reverse);
    }

    public static byte[] computeHash(byte[] bytes, boolean reverse) throws EncryptionException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ENCRYPTION_STANDARD);
            if (reverse) {
                return ArrayUtils.reverseByteArray(digest.digest(bytes), false);
            }
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(String.format(
                    "No such encryption algorithm: %s\n",
                    ENCRYPTION_STANDARD
            ));
        }
    }

    public static byte[] computeHashReversed(byte[] bytes, int target) throws EncryptionException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ENCRYPTION_STANDARD);
            byte [] padded = ArrayUtils.concat(new byte[target - ArrayUtils.reverseByteArray(bytes, false).length], ArrayUtils.reverseByteArray(bytes, false));
            return ArrayUtils.reverseByteArray(digest.digest(padded), false);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(String.format(
                    "No such encryption algorithm: %s\n",
                    ENCRYPTION_STANDARD
            ));
        }
    }
}