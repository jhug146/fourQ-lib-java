import types.Pair;

import java.math.BigInteger;

public class FP {

    private static class macros {

    }

    private static final int NWORDS_ORDER = 8;
    static BigInteger moduloOrder(BigInteger key) {
        BigInteger temp = montgomeryMultiplyModOrder(key, FourQConstants.MONTGOMERY_R_PRIME);
        return montgomeryMultiplyModOrder(temp, FourQConstants.ONE);
    }

    static BigInteger montgomeryMultiplyModOrder(BigInteger a, BigInteger b) {
        BigInteger p = multiply(a, b);
        BigInteger q = multiply(p, FourQConstants.MONTGOMERY_R_PRIME);
        BigInteger returnEnd = multiply(q, FourQConstants.CURVE_ORDER);

        Pair<BigInteger, Integer> result = mpAdd(p, returnEnd);
        returnEnd = result.first;
        int cout = result.second;

        BigInteger returnVal = returnEnd.shiftRight(NWORDS_ORDER);
        Pair<BigInteger, Integer> result2 = mpSubtract(returnVal, FourQConstants.CURVE_ORDER);
        returnVal = returnVal.add(result2.first);
        Integer bout = result2.second;
        int mask = cout - bout;

        returnEnd = returnEnd.add(FourQConstants.CURVE_ORDER.and(BigInteger.valueOf(mask)));
        return returnVal.add(returnEnd);
    }

    static BigInteger subtractModOrder(BigInteger a, BigInteger b) {
        Pair<BigInteger, Integer> returnVal = mpSubtract(a, b);
        
    }

    // Convert scalar to odd if even using the prime subgroup order r
    static BigInteger conversionToOdd(BigInteger scalar) {
        byte[] k = scalar.toByteArray();

        // Check if scalar is even (use last byte for parity)
        /*
        Java uses big-endian for the BigInteger class (msb comes first here)
        Hence, the least significant bit will determine odd/even: i.e. the last digit
         */
        boolean isEven = (k[k.length-1] & 1) == 0;

        if (!isEven) {
            return scalar;  // Already odd
        }

        // Add curve order to make odd
        return scalar.add(FourQConstants.CURVE_ORDER);
    }

    static BigInteger multiply(BigInteger a, BigInteger b) {

    }

    static Pair<BigInteger, Integer> mpAdd(BigInteger a, BigInteger b) {

    }

    static Pair<BigInteger, Integer> mpSubtract(BigInteger a, BigInteger b) {

    }
}
