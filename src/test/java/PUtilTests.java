
import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import field.operations.FP.PUtil;
import constants.Params;


public class PUtilTests {

    private static final BigInteger PRIME = Params.PRIME_1271;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger ZERO = BigInteger.ZERO;

    @Test
    void testMod1271() {
        BigInteger a = PRIME.multiply(BigInteger.TEN).add(BigInteger.valueOf(123));
        assertEquals(BigInteger.valueOf(123), PUtil.fpMod1271(a));
    }

    @Test
    void testFpAdd1271_NoReduction() {
        BigInteger a = BigInteger.valueOf(123);
        BigInteger b = BigInteger.valueOf(456);
        BigInteger expected = a.add(b);
        assertEquals(expected, PUtil.fpAdd1271(a, b));
    }

    @Test
    void testFpAdd1271_WithReduction() {
        BigInteger a = PRIME.subtract(BigInteger.ONE);
        BigInteger b = BigInteger.ONE;
        assertEquals(ZERO, PUtil.fpAdd1271(a, b));
    }

    @Test
    void testFpSub1271_PositiveResult() {
        BigInteger a = BigInteger.valueOf(1000);
        BigInteger b = BigInteger.valueOf(200);
        assertEquals(BigInteger.valueOf(800), PUtil.fpSub1271(a, b));
    }

    @Test
    void testFpSub1271_NegativeWrap() {
        BigInteger a = BigInteger.valueOf(100);
        BigInteger b = BigInteger.valueOf(200);
        assertEquals(PRIME.subtract(BigInteger.valueOf(100)), PUtil.fpSub1271(a, b));
    }

    @Test
    void testFpNeg1271() {
        assertEquals(ZERO, PUtil.fpNeg1271(ZERO));
        assertEquals(ONE, PUtil.fpNeg1271(PRIME.subtract(ONE)));
    }

    @Test
    void testFpMul1271() {
        BigInteger a = BigInteger.valueOf(5);
        BigInteger b = BigInteger.valueOf(7);
        BigInteger expected = a.multiply(b).mod(PRIME);
        assertEquals(expected, PUtil.fpMul1271(a, b));
    }

    @Test
    void testFpSqr1271() {
        BigInteger a = BigInteger.valueOf(12);
        BigInteger expected = a.multiply(a).mod(PRIME);
        assertEquals(expected, PUtil.fpSqr1271(a));
    }

    @Test
    void testFpInv1271() {
        BigInteger a = BigInteger.valueOf(12345);
        BigInteger inv = PUtil.fpInv1271(a);
        BigInteger check = a.multiply(inv).mod(PRIME);
        assertEquals(ONE, check);
    }

    @Test
    void testFpDiv1271_Even() {
        BigInteger a = BigInteger.valueOf(100);
        BigInteger expected = a.shiftRight(1).mod(PRIME);
        assertEquals(expected, PUtil.fpDiv1271(a));
    }

    @Test
    void testFpDiv1271_Odd() {
        BigInteger a = BigInteger.valueOf(101);
        BigInteger expected = a.add(PRIME).shiftRight(1).mod(PRIME);
        assertEquals(expected, PUtil.fpDiv1271(a));
    }

    @Test
    void testModPow1271() {
        BigInteger base = BigInteger.valueOf(12345);
        BigInteger exp = BigInteger.valueOf(6789);
        BigInteger expected = base.modPow(exp, PRIME);
        assertEquals(expected, PUtil.fpModPow1271(base, exp));
    }

    @Test
    void testFpExp1251() {
        BigInteger base = BigInteger.valueOf(2);
        BigInteger result = PUtil.fpExp1251(base);
        // Should return base^(2^125 - 1) mod PRIME
        BigInteger expected = base.modPow(BigInteger.ONE.shiftLeft(125).subtract(ONE), PRIME);
        assertEquals(expected, result);
    }
}