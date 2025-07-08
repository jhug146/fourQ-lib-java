package constants;

import java.util.Arrays;

public class ArrayUtils {

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
}
