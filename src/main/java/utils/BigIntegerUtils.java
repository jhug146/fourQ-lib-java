package utils;

import constants.Key;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;

import java.math.BigInteger;
import java.util.Arrays;

import static utils.ByteArrayReverseMode.KEEP_LEADING_PADDING;

public class BigIntegerUtils {
    public static F2Element convertBigIntegerToF2Element(@NotNull BigInteger val) {
        BigInteger realPart = reverseBigInteger(val.divide(Key.POW_128), KEEP_LEADING_PADDING);
        BigInteger imagPart = reverseBigInteger(val.mod(Key.POW_128), KEEP_LEADING_PADDING);
        return new F2Element(realPart, imagPart);
    }

    public static BigInteger reverseBigInteger(BigInteger val, ByteArrayReverseMode handleZero) {
        return new BigInteger(
                1,
                ByteArrayUtils.reverseByteArray(
                        val.toByteArray(),
                        handleZero
                )
        );
    }

    public static byte[] bigIntegerToByte(
            BigInteger publicKey,
            int keySize,
            boolean removePadZeros
    ) {
        byte[] raw = publicKey.toByteArray();
        if (removePadZeros) {
            if (raw[0] == 0) raw = Arrays.copyOfRange(raw, 1, raw.length);
            return raw;
        }
        if (raw.length == keySize) return raw;

        if (raw.length < keySize) {
            byte[] padded = new byte[keySize];
            System.arraycopy(raw, 0, padded, keySize - raw.length, raw.length);
            return padded;
        }

        return Arrays.copyOfRange(raw, raw.length - keySize, raw.length);
    }
}
