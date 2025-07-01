import constants.Params;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;

import operations.FP.putil;


public class PUtilTests {

    private static final BigInteger PRIME = Params.PRIME_1271;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger ZERO = BigInteger.ZERO;

    @Test
    void testMod1271() {
        BigInteger a = PRIME.multiply(BigInteger.TEN).add(BigInteger.valueOf(123));
        assertEquals(BigInteger.valueOf(123), putil.mod1271(a));
    }

    @Test
    void testFpAdd1271_NoReduction() {
        BigInteger a = BigInteger.valueOf(123);
        BigInteger b = BigInteger.valueOf(456);
        BigInteger expected = a.add(b);
        assertEquals(expected, putil.fpadd1271(a, b));
    }

    @Test
    void testFpAdd1271_WithReduction() {
        BigInteger a = PRIME.subtract(BigInteger.ONE);
        BigInteger b = BigInteger.ONE;
        assertEquals(ZERO, putil.fpadd1271(a, b));
    }

    @Test
    void testFpSub1271_PositiveResult() {
        BigInteger a = BigInteger.valueOf(1000);
        BigInteger b = BigInteger.valueOf(200);
        assertEquals(BigInteger.valueOf(800), putil.fpsub1271(a, b));
    }

    @Test
    void testFpSub1271_NegativeWrap() {
        BigInteger a = BigInteger.valueOf(100);
        BigInteger b = BigInteger.valueOf(200);
        assertEquals(PRIME.subtract(BigInteger.valueOf(100)), putil.fpsub1271(a, b));
    }

    @Test
    void testFpNeg1271() {
        assertEquals(ZERO, putil.fpneg1271(ZERO));
        assertEquals(ONE, putil.fpneg1271(PRIME.subtract(ONE)));
    }

    @Test
    void testFpMul1271() {
        BigInteger a = BigInteger.valueOf(5);
        BigInteger b = BigInteger.valueOf(7);
        BigInteger expected = a.multiply(b).mod(PRIME);
        assertEquals(expected, putil.fpmul1271(a, b));
    }

    @Test
    void testFpSqr1271() {
        BigInteger a = BigInteger.valueOf(12);
        BigInteger expected = a.multiply(a).mod(PRIME);
        assertEquals(expected, putil.fpsqr1271(a));
    }

    @Test
    void testFpZero1271() {
        assertEquals(ZERO, putil.fpzero1271(BigInteger.TEN));
    }

    @Test
    void testFpCopy1271() {
        BigInteger a = new BigInteger("123456789");
        assertEquals(a, putil.fpcopy1271(a));
    }

    @Test
    void testFpInv1271() {
        BigInteger a = BigInteger.valueOf(12345);
        BigInteger inv = putil.fpinv1271(a);
        BigInteger check = a.multiply(inv).mod(PRIME);
        assertEquals(ONE, check);
    }

    @Test
    void testFpDiv1271_Even() {
        BigInteger a = BigInteger.valueOf(100);
        BigInteger expected = a.shiftRight(1).mod(PRIME);
        assertEquals(expected, putil.fpdiv1271(a));
    }

    @Test
    void testFpDiv1271_Odd() {
        BigInteger a = BigInteger.valueOf(101);
        BigInteger expected = a.add(PRIME).shiftRight(1).mod(PRIME);
        assertEquals(expected, putil.fpdiv1271(a));
    }

    @Test
    void testModPow1271() {
        BigInteger base = BigInteger.valueOf(12345);
        BigInteger exp = BigInteger.valueOf(6789);
        BigInteger expected = base.modPow(exp, PRIME);
        assertEquals(expected, putil.modPow1271(base, exp));
    }

    @Test
    void testFpExp1251() {
        BigInteger base = BigInteger.valueOf(2);
        BigInteger result = putil.fpexp1251(base);
        // Should return base^(2^125 - 1) mod PRIME
        BigInteger expected = base.modPow(BigInteger.ONE.shiftLeft(125).subtract(ONE), PRIME);
        assertEquals(expected, result);
    }
}