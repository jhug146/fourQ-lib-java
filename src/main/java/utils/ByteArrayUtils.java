package utils;

import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.Optional;

import static utils.ByteArrayReverseMode.*;

public class ByteArrayUtils {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static byte[] reverseByteArray(
            byte @NotNull [] src,
            @NotNull Optional<ByteArrayReverseMode> mode
    ) {
        if (src.length == 0) return new byte[0];
        else if (src.length == 1) return new byte[] { src[0] };

        byte[] rev = new byte[src.length];
        for (int i = 0; i < src.length; i++) rev[i] = src[src.length - 1 - i];

        if (mode.isEmpty() || mode.get() == KEEP_LEADING_ZERO) return rev;
        
        return switch (mode.get()) {
            case ByteArrayReverseMode m when m == REMOVE_LEADING_ZERO && rev[0] == 0 ->
                Arrays.copyOfRange(rev, 1, rev.length);
            case ByteArrayReverseMode m when m == REMOVE_TRAILING_ZERO && rev[rev.length - 1] == 0 ->
                Arrays.copyOfRange(rev, 0, rev.length - 1);
            case ByteArrayReverseMode m when m == KEEP_LEADING_PADDING && leadingZeroes(src) > 0 -> {
                final int leadingZeros = leadingZeroes(src);
                byte[] padded = new byte[rev.length];
                copyByteArrayToByteArray(rev, 0, padded, leadingZeros, rev.length - leadingZeros);
                yield padded;
            }
            default -> rev;
        };
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
        copyByteArrayToByteArray(a, 0, out, 0, a.length);
        copyByteArrayToByteArray(b, 0, out, a.length, b.length);
        return out;
    }

    // Wraps System.arraycopy() calls.
    public static void copyByteArrayToByteArray(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }
}
