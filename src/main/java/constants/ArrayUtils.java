package constants;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Arrays;

public class ArrayUtils {
    public static String byteArrayToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    public static byte[] reverseByteArray(byte @NotNull [] src) {
        if (src.length == 0) return new byte[0];
        if (src.length == 1) return new byte[] {src[0]};

        int n = src.length;
        byte[] rev = new byte[n];

        for (int i = 0; i < n; i++) {
            rev[i] = src[n - 1 - i];
        }

        if (rev[0] == 0) {
            rev = Arrays.copyOfRange(rev, 1, rev.length);
        }

        return rev;
    }

    public static byte[] reverseByteArray(byte[] src, boolean keepPadding) {
        byte[] rev = reverseByteArray(src);
        final int leadingZeros = leadingZeroes(src);

        if (keepPadding && leadingZeros > 0) {
            byte[] padded = new byte[rev.length];
            System.arraycopy(rev, 0, padded, leadingZeros, rev.length - leadingZeros);
            rev = padded;
        }

        return rev;
    }

    public static byte[] reverseByteArrayKeepLeadingZero(byte[] src) {
        if (src.length == 0) return new byte[0];
        if (src.length == 1) return new byte[] {src[0]};

        int n = src.length;
        byte[] rev = new byte[n];

        for (int i = 0; i < n; i++) {
            rev[i] = src[n - 1 - i];
        }
        return rev;
    }

    public static BigInteger reverseBigInteger(BigInteger val) {
        return new BigInteger(1, reverseByteArray(val.toByteArray(), true));
    }

    private static int leadingZeroes(byte[] a) {
        int i = 0;
        while (i < a.length && a[i] == 0) {
            i++;
        }
        return i;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        if (a == null || a.length == 0) {
            return b == null ? new byte[0] : b.clone();
        }
        if (b == null || b.length == 0) {
            return a.clone();
        }

        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static byte[] bigIntegerToByte(
            BigInteger publicKey,
            int keySize,
            boolean removePadZeros
    ) {
        byte[] raw = publicKey.toByteArray();
        if (removePadZeros) {
            if (raw[0] == 0) {
                raw = Arrays.copyOfRange(raw, 1, raw.length);
            }
            return raw;
        }
        if (raw.length == keySize) {
            return raw;
        }

        if (raw.length < keySize) {
            byte[] padded = new byte[keySize];
            System.arraycopy(raw, 0, padded, keySize - raw.length, raw.length);
            return padded;
        }

        return Arrays.copyOfRange(raw, raw.length - keySize, raw.length);
    }

    public static byte[] safeToByteArray(BigInteger a, int target) {
        byte[] raw = a.toByteArray();
        if(raw.length == target) return raw;
        byte[] paddedRaw = new byte[target];
        System.arraycopy(raw, 0, paddedRaw, target-raw.length, raw.length);
        return paddedRaw;
    }
}
