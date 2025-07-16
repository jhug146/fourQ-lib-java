package utils;

import constants.Key;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Function;

import static utils.ByteArrayReverseMode.KEEP_LEADING_PADDING;
import static utils.ByteArrayReverseMode.REMOVE_TRAILING_ZERO;
import static utils.ByteArrayUtils.copyByteArrayToByteArray;

public class BigIntegerUtils {
    @NotNull
    public static F2Element convertBigIntegerToF2Element(@NotNull BigInteger val) {
       final BigInteger[] divModRes = val.divideAndRemainder(Key.POW_128);
       final int targetLength = Key.KEY_SIZE / 2 + 1;

        final BigInteger real = new BigInteger(1, ByteArrayUtils.reverseByteArray(
            addLeadingZeros(divModRes[0].toByteArray(), targetLength),
            REMOVE_TRAILING_ZERO));

        final BigInteger imag = new BigInteger(1, ByteArrayUtils.reverseByteArray(
            addLeadingZeros(divModRes[1].toByteArray(), targetLength),
            REMOVE_TRAILING_ZERO));

        return new F2Element(real, imag);
    }

    public static byte[] bigIntegerToByte(BigInteger publicKey, int keySize, boolean removePadZeros) {
        byte[] raw = publicKey.toByteArray();
        if (removePadZeros) return (raw[0] == 0) ? Arrays.copyOfRange(raw, 1, raw.length) : raw;
        if (raw.length == keySize) return raw;
        if (raw.length < keySize) return addLeadingZeros(raw, keySize);
        return Arrays.copyOfRange(raw, raw.length - keySize, raw.length);
    }

    public static byte[] addLeadingZeros(byte[] array, int targetLength) {
        if (array.length == targetLength) return array;
        byte[] padded = new byte[targetLength];
        copyByteArrayToByteArray(array, 0, padded, targetLength - array.length, array.length);
        return padded;
    }

    // Takes an initial BigInteger and a series of function operations that are applied sequentially.
    // Note: This function leverages the immutability of the parameter.
    @SafeVarargs
    public static BigInteger buildBigInteger(BigInteger initial, Function<BigInteger, BigInteger>... operations) {
        for (Function<BigInteger, BigInteger> operation : operations) initial = operation.apply(initial);
        return initial;
    }

    public static void copyBigIntegerToByteArray(BigInteger value, int size, byte[] destination, int offset) {
        copyByteArrayToByteArray(bigIntegerToByte(value, size, false), 0, destination, offset, size);
    }
}
