package field.operations;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

import static constants.Params.MASK_127;
import static constants.Params.PRIME_1271;

class Mersenne {
    // Mersenne prime reduction for p = 2^127-1
    @NotNull
    static BigInteger mersenneReduce127(@NotNull BigInteger x) {
        // For 2^127-1, we use the property that 2^127 ≡ 1 (mod 2^127-1)
        // So any bits beyond position 126 can be added back to the lower bits

        if (x.bitLength() <= 127) {
            return x.equals(PRIME_1271) ? BigInteger.ZERO : x;
        }

        // Keep reducing until we have at most 127 bits
        while (x.bitLength() > 127) {
            // Split x into high and low parts
            BigInteger xLow = x.and(MASK_127);     // x & (2^127 - 1) - gets lower 127 bits
            BigInteger xHigh = x.shiftRight(127);  // x >> 127 - gets upper bits

            // Since 2^127 ≡ 1 (mod 2^127 - 1), we can add: x ≡ xHigh + xLow
            x = xHigh.add(xLow);
        }

        // Handle the case where x equals 2^127 - 1 (which should become 0)
        if (x.equals(PRIME_1271)) {
            return BigInteger.ZERO;
        }

        return x;
    }

    // More optimized version
    @NotNull
    static BigInteger mersenneReduce127Fast(@NotNull BigInteger x) {
        // Quick path for numbers that already fit
        if (x.bitLength() <= 127) {
            return x.equals(PRIME_1271) ? BigInteger.ZERO : x;
        }

        BigInteger result = BigInteger.ZERO;

        // Process the number in 127-bit chunks
        while (!x.equals(BigInteger.ZERO)) {
            // Extract lower 127 bits and add to result
            result = result.add(x.and(MASK_127));

            // Shift to get next chunk
            x = x.shiftRight(127);

            // If result overflows 127 bits, we need to reduce it too
            if (result.bitLength() > 127) {
                BigInteger carry = result.shiftRight(127);
                result = result.and(MASK_127).add(carry);
            }
        }

        // Final reduction if still needed
        while (result.bitLength() > 127) {
            BigInteger carry = result.shiftRight(127);
            result = result.and(MASK_127).add(carry);
        }

        // Handle the edge case where result equals the prime
        return result.equals(PRIME_1271) ? BigInteger.ZERO : result;
    }
}
