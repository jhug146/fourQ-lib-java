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
            return new BigInteger(1, reverseByteArray(digest.digest(bytes), false));
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
            return new BigInteger(1, reverseByteArray(digest.digest(reverseByteArray(bytes, false)), false));
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

    public static byte[] reverseByteArray(byte[] src, boolean keepPadding) {
        if (src == null) {
            throw new IllegalArgumentException("Source array is null");
        }
        if (src.length == 0) {
            return new byte[0];
        }

        int n = src.length;
        byte[] rev = new byte[n];
        for (int i = 0; i < n; i++) {
            rev[i] = src[n - 1 - i];
        }

        if (rev[0] == 0) {
            rev = Arrays.copyOfRange(rev, 1, rev.length);
        }

        int leadingZeros = leadingZeroes(src);
        if (keepPadding && leadingZeros > 0) {
            byte[] padded = new byte[rev.length];
            System.arraycopy(rev, 0, padded, leadingZeros, rev.length - leadingZeros);
            rev = padded;
        }

        return rev;
    }

    private static int leadingZeroes(byte[] a) {
        int i = 0;
        while (i < a.length && a[i] == 0) {
            i++;
        }
        return i;
    }
}
