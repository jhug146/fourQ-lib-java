import operations.FP2;
import types.data.F2Element;
import constants.Params;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FP2Tests {

    private static final BigInteger PRIME = Params.PRIME_1271;

    private F2Element sampleElement(BigInteger a, BigInteger b) {
        return new F2Element(a.mod(PRIME), b.mod(PRIME));
    }

    @Test
    void testFP2Copy1271() {
        F2Element a = sampleElement(BigInteger.valueOf(3), BigInteger.valueOf(5));
        assertSame(a, FP2.fp2Copy1271(a));  // Should return same reference
    }

//    @Test
//    void testFP2Zero1271() {
//        F2Element a = sampleElement(BigInteger.valueOf(123), BigInteger.valueOf(456));
//        F2Element result = FP2.fp2Zero1271(a);
//        assertEquals(ZERO, result.real);
//        assertEquals(ZERO, result.im);
//    }

    @Test
    void testFP2Neg1271() {
        F2Element a = sampleElement(BigInteger.valueOf(3), BigInteger.valueOf(5));
        F2Element result = FP2.fp2Neg1271(a);

        BigInteger expectedReal = PRIME.subtract(a.real).mod(PRIME);
        BigInteger expectedIm = PRIME.subtract(a.im).mod(PRIME);

        assertEquals(expectedReal, result.real);
        assertEquals(expectedIm, result.im);
    }

    @Test
    void testFP2Add1271() {
        F2Element a = sampleElement(BigInteger.valueOf(3), BigInteger.valueOf(4));
        F2Element b = sampleElement(BigInteger.valueOf(5), BigInteger.valueOf(6));
        F2Element result = FP2.fp2Add1271(a, b);

        assertEquals(a.real.add(b.real).mod(PRIME), result.real);
        assertEquals(a.im.add(b.im).mod(PRIME), result.im);
    }

    @Test
    void testFP2Sub1271() {
        F2Element a = sampleElement(BigInteger.valueOf(10), BigInteger.valueOf(15));
        F2Element b = sampleElement(BigInteger.valueOf(4), BigInteger.valueOf(9));
        F2Element result = FP2.fp2Sub1271(a, b);

        assertEquals(a.real.subtract(b.real).mod(PRIME), result.real);
        assertEquals(a.im.subtract(b.im).mod(PRIME), result.im);
    }

    @Test
    void testFP2AddSub1271() {
        F2Element a = sampleElement(BigInteger.valueOf(7), BigInteger.valueOf(9));
        F2Element b = sampleElement(BigInteger.valueOf(3), BigInteger.valueOf(5));

        F2Element doubled = FP2.fp2Add1271(a, a);
        F2Element expected = FP2.fp2Sub1271(doubled, b);
        F2Element result = FP2.fp2AddSub1271(a, b);

        assertEquals(expected, result);
    }

    @Test
    void testFP2Sqr1271() {
        F2Element a = sampleElement(BigInteger.valueOf(2), BigInteger.valueOf(3));
        F2Element result = FP2.fp2Sqr1271(a);

        // c.real = (a + b)(a - b)
        BigInteger expectedReal = (a.real.add(a.im)).multiply(a.real.subtract(a.im)).mod(PRIME);
        // c.im = 2 * a * b
        BigInteger expectedIm = a.real.multiply(a.im).multiply(BigInteger.valueOf(2)).mod(PRIME);

        assertEquals(expectedReal, result.real);
        assertEquals(expectedIm, result.im);
    }

    @Test
    void testFP2Mul1271() {
        F2Element a = sampleElement(BigInteger.valueOf(2), BigInteger.valueOf(3));
        F2Element b = sampleElement(BigInteger.valueOf(4), BigInteger.valueOf(5));
        F2Element result = FP2.fp2Mul1271(a, b);

        // c.real = a0*b0 - a1*b1
        BigInteger t1 = a.real.multiply(b.real).mod(PRIME);
        BigInteger t2 = a.im.multiply(b.im).mod(PRIME);
        BigInteger expectedReal = t1.subtract(t2).mod(PRIME);

        // c.im = (a0+a1)*(b0+b1) - a0*b0 - a1*b1
        BigInteger t3 = (a.real.add(a.im)).multiply(b.real.add(b.im)).mod(PRIME);
        BigInteger expectedIm = t3.subtract(t1).subtract(t2).mod(PRIME);

        assertEquals(expectedReal, result.real);
        assertEquals(expectedIm, result.im);
    }

    @Test
    void testFP2Div1271() {
        F2Element a = sampleElement(BigInteger.valueOf(6), BigInteger.valueOf(8));
        F2Element result = FP2.fp2Div1271(a);

        assertEquals(a.im.shiftRight(1).mod(PRIME), result.real);
        assertEquals(a.real.shiftRight(1).mod(PRIME), result.im);
    }

    @Test
    void testFP2Inv1271() {
        F2Element a = sampleElement(BigInteger.valueOf(5), BigInteger.valueOf(7));
        F2Element inv = FP2.fp2Inv1271(new F2Element(a.real, a.im));

        // Check that a * inv == 1 in GF(p^2)
        F2Element prod = FP2.fp2Mul1271(a, inv);
        assertEquals(BigInteger.ONE, prod.real);
        assertEquals(BigInteger.ZERO, prod.im);
    }
}
