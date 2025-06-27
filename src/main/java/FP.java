import types.Pair;

import java.math.BigInteger;

public class FP {
    private static final int NWORDS_ORDER = 8;
    static BigInteger moduloOrder(BigInteger key) {
        BigInteger temp = montgomeryMultiplyModOrder(key, Constants.MONTGOMERY_R_PRIME);
        return montgomeryMultiplyModOrder(temp, BigInteger.ONE);
    }

    static BigInteger montgomeryMultiplyModOrder(BigInteger a, BigInteger b) {
        BigInteger p = multiply(a, b);
        BigInteger q = multiply(p, Constants.MONTGOMERY_R_PRIME);
        BigInteger returnEnd = multiply(q, Constants.CURVE_ORDER);

        Pair<BigInteger, Integer> result = mpAdd(p, returnEnd);
        returnEnd = result.first;
        int cout = result.second;

        BigInteger returnVal = returnEnd.shiftRight(NWORDS_ORDER);
        Pair<BigInteger, Integer> result2 = mpSubtract(returnVal, Constants.CURVE_ORDER);
        returnVal = returnVal.add(result2.first);
        Integer bout = result2.second;
        int mask = cout - bout;

        returnEnd = returnEnd.add(Constants.CURVE_ORDER.and(BigInteger.valueOf(mask)));
        return returnVal.add(returnEnd);
    }

    static BigInteger subtractModOrder(BigInteger a, BigInteger b) {
        Pair<BigInteger, Integer> returnVal = mpSubtract(a, b);
        
    }

    static BigInteger conversionToOdd(BigInteger key) {}

    static BigInteger multiply(BigInteger a, BigInteger b) {}

    static Pair<BigInteger, Integer> mpAdd(BigInteger a, BigInteger b) {}

    static Pair<BigInteger, Integer> mpSubtract(BigInteger a, BigInteger b) {}
}
