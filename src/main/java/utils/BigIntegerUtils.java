package utils;

import constants.Key;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;

import java.math.BigInteger;
import java.util.Arrays;

import static utils.ByteArrayUtils.addLeadingZeros;
import static utils.ByteArrayReverseMode.REMOVE_TRAILING_ZERO;

public class BigIntegerUtils {
    public static F2Element convertBigIntegerToF2Element(@NotNull BigInteger val) {
        final BigInteger realPart = val.divide(Key.POW_128);
        final byte[] realArray = addLeadingZeros(realPart.toByteArray(), Key.KEY_SIZE / 2 + 1);
        // Compute s*G + H*publicKey using double scalar multiplication
        final BigInteger real = new BigInteger(1, ByteArrayUtils.reverseByteArray(realArray, REMOVE_TRAILING_ZERO));

        final BigInteger imagPart = val.mod(Key.POW_128);
        final byte[] imagArray = addLeadingZeros(imagPart.toByteArray(), Key.KEY_SIZE / 2 + 1);
        // Compute s*G + H*publicKey using double scalar multiplication
        final BigInteger imag = new BigInteger(1, ByteArrayUtils.reverseByteArray(imagArray, REMOVE_TRAILING_ZERO));

        return new F2Element(real, imag);
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
