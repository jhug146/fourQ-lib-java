import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;

import types.Pair;
import constants.Params;
import operations.FP;


public class FPTests {

    private static final BigInteger CURVE_ORDER = Params.CURVE_ORDER;
    private static final BigInteger ZERO = BigInteger.ZERO;

    @Test
    void testAddModOrderNoOverflow() {
        BigInteger a = BigInteger.valueOf(100);
        BigInteger b = BigInteger.valueOf(200);
        BigInteger expected = a.add(b).mod(CURVE_ORDER);
        assertEquals(expected, FP.addModOrder(a, b));
    }

    @Test
    void testAddModOrderWithOverflow() {
        BigInteger a = CURVE_ORDER.subtract(BigInteger.ONE);
        BigInteger b = BigInteger.TWO;
        BigInteger expected = a.add(b).mod(CURVE_ORDER);
        assertEquals(expected, FP.addModOrder(a, b));
    }

    @Test
    void testSubtractModOrderPositiveResult() {
        BigInteger a = BigInteger.valueOf(300);
        BigInteger b = BigInteger.valueOf(100);
        assertEquals(BigInteger.valueOf(200), FP.subtractModOrder(a, b));
    }

    @Test
    void testSubtractModOrderNegativeWraparound() {
        BigInteger a = BigInteger.valueOf(100);
        BigInteger b = BigInteger.valueOf(200);
        BigInteger expected = a.subtract(b).mod(CURVE_ORDER);
        assertEquals(expected, FP.subtractModOrder(a, b));
    }

    @Test
    void testConversionToOddWithOddInput() {
        BigInteger odd = BigInteger.valueOf(12345); // LSB is 1
        assertEquals(odd, FP.conversionToOdd(odd));
    }

    @Test
    void testConversionToOddWithEvenInput() {
        BigInteger even = BigInteger.valueOf(1000); // LSB is 0
        BigInteger expected = even.add(CURVE_ORDER);
        assertEquals(expected, FP.conversionToOdd(even));
    }

    @Test
    void testMultiply() {
        BigInteger a = BigInteger.valueOf(123);
        BigInteger b = BigInteger.valueOf(456);
        BigInteger expected = a.multiply(b);
        assertEquals(expected, FP.multiply(a, b));
    }

    @Test
    void testMpAddNoOverflow() {
        BigInteger a = BigInteger.valueOf(1000);
        BigInteger b = BigInteger.valueOf(2000);
        Pair<BigInteger, Integer> result = FP.mpAdd(a, b);
        assertEquals(a.add(b), result.first);
        assertEquals(0, result.second);
    }

    @Test
    void testMpAddWithOverflow() {
        BigInteger max = BigInteger.ONE.shiftLeft(Params.NWORDS_ORDER * 32).subtract(BigInteger.ONE);
        BigInteger a = max.subtract(BigInteger.ONE);
        BigInteger b = BigInteger.TEN;
        Pair<BigInteger, Integer> result = FP.mpAdd(a, b);

        assertEquals((a.add(b)).mod(BigInteger.ONE.shiftLeft(Params.NWORDS_ORDER * 32)), result.first);
        assertEquals(1, result.second);
    }

    @Test
    void testMpSubtractNoBorrow() {
        BigInteger a = BigInteger.valueOf(5000);
        BigInteger b = BigInteger.valueOf(3000);
        Pair<BigInteger, Integer> result = FP.mpSubtract(a, b);
        assertEquals(BigInteger.valueOf(2000), result.first);
        assertEquals(0, result.second);
    }

    @Test
    void testMpSubtractWithBorrow() {
        BigInteger a = BigInteger.valueOf(1000);
        BigInteger b = BigInteger.valueOf(3000);
        BigInteger expected = a.subtract(b).add(BigInteger.ONE.shiftLeft(Params.NWORDS_ORDER * 32));
        Pair<BigInteger, Integer> result = FP.mpSubtract(a, b);

        assertEquals(expected, result.first);
        assertEquals(1, result.second);
    }

    @Test
    void testMontgomeryMultiplyModOrderBasicCorrectness() {
        BigInteger a = BigInteger.valueOf(12345);
        BigInteger b = BigInteger.valueOf(67890);
        BigInteger result = FP.montgomeryMultiplyModOrder(a, b);

        // Sanity check: result should be in range [0, CURVE_ORDER)
        assertTrue(result.compareTo(ZERO) >= 0 && result.compareTo(CURVE_ORDER) < 0);
    }
}
