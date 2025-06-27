import types.AddResult;
import types.Pair;

import java.math.BigInteger;

public class FP {

    private static class Macros {
        public static AddResult ADDC(int carryIn, int a, int b) {
            // Use long to capture overflow
            long temp = (a & 0xFFFFFFFFL) + (b & 0xFFFFFFFFL) + (carryIn & 0xFFFFFFFFL);
            int sum = (int) temp;                               // Low 32 bits
            int carry = (int) (temp >>> FourQConstants.RADIX);  // High 32 bits (carry)
            return new AddResult(sum, carry);
        }
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
        return a.subtract(b).mod(FourQConstants.CURVE_ORDER);
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

    /**
     * The following assumes that BigInteger performance limitations are negligible.
     *
     * @param a first argument in multiply
     * @param b second argument in multiply
     * @return a * b
     */
    static BigInteger multiply(BigInteger a, BigInteger b) {
        return a.multiply(b);
    }

    static Pair<BigInteger, Integer> mpAdd(BigInteger a, BigInteger b) {
        // Add the two numbers
        BigInteger sum = a.add(b);

        // Calculate the maximum value for NWORDS_ORDER words
        // Assuming 32-bit words: max = 2^(NWORDS_ORDER * 32) - 1
        int bitsPerWord = 32;  // TODO or 64 if using 64-bit words
        int totalBits = NWORDS_ORDER * bitsPerWord;
        BigInteger maxValue = BigInteger.ONE.shiftLeft(totalBits); //First value that cannot be represented in system

        // Check if overflow occurred
        if (sum.compareTo(maxValue) >= 0) {
            // Overflow, return sum mod 2^totalBits, carry = 1
            BigInteger wrappedSum = sum.remainder(maxValue);
            return new Pair<>(wrappedSum, 1);
        } else {
            // Nooverflow, return sum as-is, carry = 0
            return new Pair<>(sum, 0);
        }
    }

    static Pair<BigInteger, Integer> mpSubtract(BigInteger a, BigInteger b) {
        // For fixed-width arithmetic, handle negative results
        if (a.compareTo(b) >= 0) {
            // No borrow
            return new Pair<>(a.subtract(b), 0);
        }

        // Borrow occurred
        // Simulate fixed-width wraparound: a - b + 2^n
        int totalBits = NWORDS_ORDER * 32; //TODO make '32' change based on system
        BigInteger modulus = BigInteger.ONE.shiftLeft(totalBits), wrappedResult = a.subtract(b).add(modulus);
        return new Pair<>(wrappedResult, 1);
    }
}
