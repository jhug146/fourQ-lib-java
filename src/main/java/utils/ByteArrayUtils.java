package utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ByteArrayUtils {
    public static String byteArrayToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] reverseByteArray(
            byte @NotNull [] src,
            ByteArrayReverseMode mode
    ) {
        if (src.length == 0) return new byte[0];
        else if (src.length == 1) return new byte[] { src[0] };

        byte[] rev = new byte[src.length];
        for (int i = 0; i < src.length; i++) rev[i] = src[src.length - 1 - i];

        switch (mode) {
            case REMOVE_LEADING_ZERO -> {
                if (rev[0] == 0) rev = Arrays.copyOfRange(rev, 1, rev.length);
            }
            case KEEP_LEADING_ZERO -> {;} // Do nothing.
            case KEEP_LEADING_PADDING -> {
                final int leadingZeros = leadingZeroes(src);
                if (leadingZeros > 0) {
                    byte[] padded = new byte[rev.length];
                    System.arraycopy(rev, 0, padded, leadingZeros, rev.length - leadingZeros);
                    rev = padded;
                }
            }
        }

        return rev;
    }

    private static int leadingZeroes(byte[] a) {
        int i = 0;
        while (i < a.length && a[i] == 0) i++;
        return i;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        if (a == null || a.length == 0) return b == null ? new byte[0] : b.clone();
        else if (b == null || b.length == 0) return a.clone();

        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

}
