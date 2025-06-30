package operations;

import types.AddResult;
import types.Pair;
import constants.Params;

import java.math.BigInteger;

public class FP {

    private interface Macros {
        static AddResult ADDC(int carryIn, int a, int b) {
            // Use long to capture overflow
            long temp = (a & 0xFFFFFFFFL) + (b & 0xFFFFFFFFL) + (carryIn & 0xFFFFFFFFL);
            int sum = (int) temp;                               // Low 32 bits
            int carry = (int) (temp >>> Params.RADIX);          // High 32 bits (carry)
            return new AddResult(sum, carry);
        }
    }

    public static BigInteger moduloOrder(BigInteger key) {
        return montgomeryMultiplyModOrder
                (montgomeryMultiplyModOrder(key, Params.MONTGOMERY_R_PRIME), BigInteger.ONE);
    }

    /*
    static BigInteger montgomeryMultiplyModOrder(BigInteger a, BigInteger b) {
        BigInteger product = multiply(a, b), quotient = multiply(product, constants.FourQConstants.MONTGOMERY_R_PRIME);
        BigInteger returnEnd = multiply(quotient, constants.FourQConstants.CURVE_ORDER);

        Pair<BigInteger, Integer> result = mpAdd(product, returnEnd);
        returnEnd = result.first;
        int carryOut = result.second;

        BigInteger returnVal = returnEnd.shiftRight(NWORDS_ORDER);

        Pair<BigInteger, Integer> result2 = mpSubtract(returnVal, constants.FourQConstants.CURVE_ORDER);

        returnVal = returnVal.add(result2.first);
        Integer bout = result2.second;
        int mask = carryOut - bout;

        returnEnd = returnEnd.add(constants.FourQConstants.CURVE_ORDER.and(BigInteger.valueOf(mask)));
        return returnVal.add(returnEnd);
    }
    */ // <-Previous implementation.
    public static BigInteger montgomeryMultiplyModOrder(BigInteger a, BigInteger b) {
        BigInteger product = a.multiply(b);

        // Compute Montgomery quotient
        BigInteger quotient = product.multiply(Params.MONTGOMERY_R_PRIME);
        int wordBits = Params.NWORDS_ORDER * 32; //TODO Change based on system
        BigInteger rMask = BigInteger.ONE.shiftLeft(wordBits).subtract(BigInteger.ONE);
        quotient = quotient.and(rMask);  // quotient mod R

        // Montgomery reduction: (product + quotient * modulus) / R
        BigInteger numerator = product.add(quotient.multiply(Params.CURVE_ORDER));
        BigInteger result = numerator.shiftRight(wordBits);

        // Final conditional subtraction
        return result.compareTo(Params.CURVE_ORDER) >= 0
                ? result.subtract(Params.CURVE_ORDER)
                : result;
    }

    // Subtraction modulo the curve order, c = a+b mod order
    public static BigInteger subtractModOrder(BigInteger a, BigInteger b) {
        return a.subtract(b).mod(Params.CURVE_ORDER);
    }

    // Addition modulo the curve order, c = a+b mod order
    public static BigInteger addModOrder(BigInteger a, BigInteger b) {
        return a.add(b).mod(Params.CURVE_ORDER);
    }

    // Convert scalar to odd if even using the prime subgroup order r
    public static BigInteger conversionToOdd(BigInteger scalar) {
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
        return scalar.add(Params.CURVE_ORDER);
    }

    /**
     * The following assumes that BigInteger performance limitations are negligible.
     *
     * @param a first argument in multiply
     * @param b second argument in multiply
     * @return a * b
     */
    public static BigInteger multiply(BigInteger a, BigInteger b) {
        return a.multiply(b);
    }

    public static Pair<BigInteger, Integer> mpAdd(BigInteger a, BigInteger b) {
        // Add the two numbers
        BigInteger sum = a.add(b);

        // Calculate the maximum value for NWORDS_ORDER words
        // Assuming 32-bit words: max = 2^(NWORDS_ORDER * 32) - 1
        int bitsPerWord = 32;  // TODO or 64 if using 64-bit words
        int totalBits = Params.NWORDS_ORDER * bitsPerWord;
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

    public static Pair<BigInteger, Integer> mpSubtract(BigInteger a, BigInteger b) {
        // For fixed-width arithmetic, handle negative results
        if (a.compareTo(b) >= 0) {
            // No borrow
            return new Pair<>(a.subtract(b), 0);
        }

        // Borrow occurred
        // Simulate fixed-width wraparound: a - b + 2^n
        int totalBits = Params.NWORDS_ORDER * 32; //TODO make '32' change based on system
        BigInteger modulus = BigInteger.ONE.shiftLeft(totalBits), wrappedResult = a.subtract(b).add(modulus);
        return new Pair<>(wrappedResult, 1);
    }

    // namespace for prime-based utility functions.
    public interface putil {
        // Modular correction, output = a mod (2^127-1)
        static BigInteger mod1271(BigInteger a) {
            return a.mod(Params.PRIME_1271);
        }

        // Field multiplication using schoolbook method, c = a*b mod p
        static BigInteger fpmul1271(BigInteger a, BigInteger b) {
            return Mersenne.mersenneReduce127Fast(multiply(a, b));
        }

        // Field squaring using schoolbook method, output = a^2 mod p
        static BigInteger fpsqr1271(BigInteger a) {
            return putil.fpmul1271(a, a);
        }

        // Zeroing a field element, a = 0
        //  NB: There is no mutable BigInteger interface, which renders this functionality
        //          heavily redundant.
        static BigInteger fpzero1271(BigInteger a) {
            return BigInteger.ZERO;
        }

        // Copy of a field element, out = a
        static BigInteger fpcopy1271(BigInteger a) {
            return a.subtract(BigInteger.ZERO);
        }

        // Field negation, a = -a mod (2^127-1)
        static BigInteger fpneg1271(BigInteger a) {
            // Ensure input is in valid range first
            a = a.mod(Params.PRIME_1271);

            if (a.equals(BigInteger.ZERO)) {
                return BigInteger.ZERO;
            }
            return Params.PRIME_1271.subtract(a);
        }

        // Field inversion, af = a^-1 = a^(p-2) mod p
        static BigInteger fpinv1271(BigInteger a) {
            BigInteger outputBuilder = fpexp1251(a);
            outputBuilder = fpsqr1271(outputBuilder);
            outputBuilder = fpsqr1271(outputBuilder);
            outputBuilder = fpmul1271(a, outputBuilder);
            return outputBuilder;
        }

        static BigInteger fpexp1251(BigInteger a) {
            // TODO ASSM: The "1251" in the name might refer to a specific windowing or addition chain strategy,
            //  not the literal exponent 1251
            BigInteger exponent = BigInteger.ONE.shiftLeft(125).subtract(BigInteger.ONE);
            return modPow1271(a, exponent);
        }

        // Optimized modular exponentiation for 2^127-1
        static BigInteger modPow1271(BigInteger base, BigInteger exponent) {
            // Use Java's built-in with Mersenne optimization
            BigInteger result = base.modPow(exponent, Params.PRIME_1271);
            return Mersenne.mersenneReduce127(result);
        }

        // Field addition, c = a+b mod (2^127-1)
        static BigInteger fpadd1271(BigInteger a, BigInteger b) {
            BigInteger sum = a.add(b);

            // Quick path: if sum < 2^127, no reduction needed
            if (sum.bitLength() <= 127) {
                return sum.equals(Params.PRIME_1271) ? BigInteger.ZERO : sum;
            }

            // Handle the single overflow case (sum has 128 bits)
            if (sum.bitLength() == 128) {
                // Extract bit 127 and add it back to lower 127 bits
                BigInteger lower127 = sum.and(Params.MASK_127);  // sum & (2^127-1)
                BigInteger overflow = sum.shiftRight(127); // sum >> 127 (will be 1)

                BigInteger result = lower127.add(overflow);
                return result.equals(Params.PRIME_1271) ? BigInteger.ZERO : result;
            }

            // Fallback for unexpected cases (shouldn't happen with valid inputs)
            return Mersenne.mersenneReduce127(sum);
        }

        // Field subtraction, c = a-b mod (2^127-1)
        static BigInteger fpsub1271(BigInteger a, BigInteger b) {
            BigInteger diff = a.subtract(b);

            // If result is negative, add the prime to make it positive
            if (diff.signum() < 0) {
                return diff.add(Params.PRIME_1271);
            }

            return diff;
        }
    }
}
