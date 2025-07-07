package crypto;

import constants.Key;
import exceptions.EncryptionException;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class HashFunction {
    private static final String ENCRYPTION_STANDARD = "SHA-512";
    public static BigInteger computeHash(@NotNull BigInteger input) throws EncryptionException {
        return computeHash(CryptoUtil.bigIntegerToByte(input, Key.KEY_SIZE,false));
    }

    public static BigInteger computeHash(byte[] bytes) throws EncryptionException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ENCRYPTION_STANDARD);
            return new BigInteger(1, reverseByteArray(digest.digest(bytes)));
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(String.format(
                    "No such encryption algorithm: %s\n",
                    ENCRYPTION_STANDARD
            ));
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] reverseByteArray(byte[] _array) {
        byte[] array = new byte[_array.length];
        for (int i = 0; i < _array.length; i++) {
            array[i] = _array[_array.length - i - 1];
        }
        if (array[0] == 0) {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        return array;
    }
}
