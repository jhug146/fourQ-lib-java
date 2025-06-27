import types.Pair;

import java.math.BigInteger;

public class FP {
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

        int carry = 0; //NB this should be unsigned.
        byte[] order = FourQConstants.CURVE_ORDER.toByteArray();

        byte mask = (byte) ~-(k[0] & 1);

        byte[] kOdd = new byte[k.length];
        for (int i = 0; i < NWORDS_ORDER; i++) {  // If (k is odd) then k_odd = k else k_odd = k + r
            ADDC(carry, order[i] & mask, k[i], carry, kOdd[i]);
        }

        return new BigInteger(kOdd);
    }

    private static void ADDC(int carryIn, int addend1, byte addend2, int carryOut, int sumOut) {
        int tempReg = (addend1) + (int)(carryIn);
        (sumOut) = (addend2) + tempReg;
        (carryOut) = (isDigitLessthanCt(tempReg, (carryIn)) | isDigitLessthanCt((sumOut), tempReg));
    }

    private static int isDigitLessthanCt(int x, int y) { // Is x < y?
        return ((x ^ ((x ^ y) | ((x - y) ^ y))) >> (FourQConstants.RADIX-1));
    }

    static BigInteger multiply(BigInteger a, BigInteger b) {

    }

    static Pair<BigInteger, Integer> mpAdd(BigInteger a, BigInteger b) {

    }

    static Pair<BigInteger, Integer> mpSubtract(BigInteger a, BigInteger b) {

    }
}
