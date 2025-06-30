import constants.Params;
import operations.FP;
import operations.FP2;
import types.*;

import java.math.BigInteger;


public class ECCUtil {
    private static final int W_FIXEDBASE = 5;
    private static final int V_FIXEDBASE = 5;
    private static final int D_FIXEDBASE = 54;
    private static final int E_FIXEDBASE = 10;

    private static final F2Element F2_ONE = new F2Element(BigInteger.ONE, BigInteger.ONE);

    // Set generator
    // Output: P = (x,y)
    // TODO VeRy unsure about this and the helper
    public static void eccSet(AffinePoint<F2Element> P) {
        P.x = convertToF2Element(Params.GENERATOR_X);    // X1
        P.y = convertToF2Element(Params.GENERATOR_Y);    // Y1
    }

    // Helper method to convert BigInteger to F2Element
    private static F2Element convertToF2Element(BigInteger generator) {
        // Split the 256-bit generator into two 127-bit parts for GF(p²)
        BigInteger realPart = generator.and(Params.MASK_127);                           // Lower 127 bits
        BigInteger imagPart = generator.shiftRight(127).and(Params.MASK_127);        // Upper 127 bits

        return new F2Element(realPart, imagPart);
    }

    static FieldPoint<F2Element> eccMulFixed(BigInteger val) {
        BigInteger temp = FP.moduloOrder(val);
        temp = FP.conversionToOdd(temp);
        int[] digits = mLSBSetRecode(temp);  // TODO: No idea how this works
        int digit = digits[W_FIXEDBASE * D_FIXEDBASE - 1];
        int startI = (W_FIXEDBASE - 1) * D_FIXEDBASE - 1;
        for (int i = startI; i >= 2 * D_FIXEDBASE - 1; i -= D_FIXEDBASE) {
            digit = 2 * digit + digits[i];
        }

        // TODO: Both instances of TABLE in this function might need updating
        AffinePoint<F2Element> affPoint = Table.tableLookupFixedBase(digit, digits[D_FIXEDBASE - 1]);
        ExtendedPoint<F2Element> exPoint = r5ToR1(affPoint);

        for (int j = 0; j < V_FIXEDBASE - 1; j++) {
            digit = digits[W_FIXEDBASE * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1];
            int iStart = (W_FIXEDBASE - 1) * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            int iMin = 2 * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            for (int i = iStart; i >= iMin; i -= D_FIXEDBASE) {
                digit = 2 * digit + digits[i];
            }
            // Extract point in (x+y,y-x,2dt) representation
            int signDigit = D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            affPoint = Table.tableLookupFixedBase(digit, digits[signDigit]);
            exPoint = eccMixedAdd(affPoint, exPoint);
        }

        for (int i = E_FIXEDBASE - 2; i >= 0; i--) {
            exPoint = eccDouble(exPoint);
            for (int j = 0; j < V_FIXEDBASE; j++) {
                digit = digits[W_FIXEDBASE * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE];
                int kStart = (W_FIXEDBASE - 1) * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                int kMin = 2 * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                for (int k = kStart; k >= kMin; k -= D_FIXEDBASE) {
                    digit = 2 * digit + digits[k];
                }
                int signDigit = D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                affPoint = Table.tableLookupFixedBase(digit, signDigit);
                exPoint = eccMixedAdd(affPoint, exPoint);
            }
        }
        return eccNorm(exPoint);
    }


    static int[] mLSBSetRecode(BigInteger scalar) {
        return null;
    }

    private static ExtendedPoint<F2Element> r5ToR1(AffinePoint<F2Element> p) {
        F2Element x = FP2.div1271(FP2.fp2sub1271(p.x, p.y));
        F2Element y = FP2.div1271(FP2.fp2add1271(p.x, p.y));
        return new ExtendedPoint<F2Element>(x, y, F2_ONE, x, y);
    }

    private static PreComputedExtendedPoint<F2Element> r1ToR2(ExtendedPoint<F2Element> point) {
        return null;
    }

    private static PreComputedExtendedPoint<F2Element> r1ToR3(ExtendedPoint<F2Element> point) {
        return new PreComputedExtendedPoint<>(
                FP2.fp2add1271(point.x, point.y),
                FP2.fp2sub1271(point.y, point.x),
                FP2.fp2mul1271(point.ta, point.tb),
                point.z
        );
    }

    private static ExtendedPoint<F2Element> eccMixedAdd(AffinePoint<F2Element> q, ExtendedPoint<F2Element> p) {
        F2Element ta = FP2.fp2mul1271(p.ta, p.tb);
        F2Element t1 = FP2.fp2add1271(p.z, p.z);
        ta = FP2.fp2mul1271(ta, q.t);
        F2Element pz = FP2.fp2add1271(p.x, p.y);
        F2Element tb = FP2.fp2sub1271(p.y, p.x);
        F2Element t2 = FP2.fp2sub1271(t1, ta);
        t1 = FP2.fp2add1271(t1, ta);
        ta = FP2.fp2mul1271(q.x, pz);
        F2Element x = FP2.fp2mul1271(q.y, tb);
        tb = FP2.fp2sub1271(ta, x);
        ta = FP2.fp2add1271(ta, x);
        return new ExtendedPoint<>(
                FP2.fp2mul1271(tb, t2),
                FP2.fp2mul1271(ta, t1),
                FP2.fp2mul1271(t1, t2),
                ta,
                tb
        );
    }

    private static ExtendedPoint<F2Element> eccDouble(ExtendedPoint<F2Element> p) {
        F2Element t1 = FP2.fp2sqr1271(p.x);
        F2Element t2 = FP2.fp2sqr1271(p.y);
        F2Element t3 = FP2.fp2add1271(p.x, p.y);
        F2Element tb = FP2.fp2add1271(t1, t2);
        t1 = FP2.fp2sub1271(t2, t1);
        F2Element ta = FP2.fp2sqr1271(t3);
        t2 = FP2.fp2sqr1271(p.z);
        ta = FP2.fp2sub1271(ta, tb);
        t2 = FP2.fp2addsub1271(t2, t1);
        final F2Element y = FP2.fp2mul1271(t1, tb);
        final F2Element x = FP2.fp2mul1271(t2, ta);
        final F2Element z = FP2.fp2mul1271(t1, t2);
        return new ExtendedPoint<>(x, y, z, ta, tb);
    }

    private static FieldPoint<F2Element> eccNorm(ExtendedPoint<F2Element> p) {
        final F2Element zInv = FP2.fp2inv1271(p.z);
        final F2Element x = FP2.fp2mul1271(p.x, zInv);
        final F2Element y = FP2.fp2mul1271(p.y, zInv);
        return new FieldPoint<>(x, y);
    }

    static FieldPoint<F2Element> eccMulDouble(BigInteger k, FieldPoint<F2Element> q, BigInteger l) {
        FieldPoint<F2Element> a = eccMul(q, l);
        ExtendedPoint<F2Element> t = pointSetup(a);
        final PreComputedExtendedPoint<F2Element> s = r1ToR2(t);
        a = eccMulFixed(k);
        t = pointSetup(a);
        t = eccAdd(s, t);
        return eccNorm(t);
    }

    private static ExtendedPoint<F2Element> eccAddCore(PreComputedExtendedPoint<F2Element> p, PreComputedExtendedPoint<F2Element> q) {
        F2Element z = FP2.fp2mul1271(p.t, q.t);
        F2Element t1 = FP2.fp2mul1271(p.z, q.z);
        F2Element x = FP2.fp2mul1271(p.xy, q.xy);
        F2Element y = FP2.fp2mul1271(p.yx, q.yx);
        F2Element t2 = FP2.fp2sub1271(t1, z);
        t1 = FP2.fp2add1271(t1, z);
        F2Element tb = FP2.fp2sub1271(x, y);
        F2Element ta = FP2.fp2add1271(x, y);
        return new ExtendedPoint<>(
                FP2.fp2mul1271(tb, t2),
                FP2.fp2mul1271(ta, t1),
                FP2.fp2mul1271(t1, t2),
                ta,
                tb
        );
    }

    private static ExtendedPoint<F2Element> eccAdd(PreComputedExtendedPoint<F2Element> q, ExtendedPoint<F2Element> p) {
        return eccAddCore(q, r1ToR3(p));
    }

    private static FieldPoint<F2Element> eccMul(FieldPoint<F2Element> p, BigInteger k) {
        return null;
    }

    private static ExtendedPoint<F2Element> pointSetup(FieldPoint<F2Element> point) {
        return new ExtendedPoint<>(
                point.x,
                point.y,
                new F2Element(BigInteger.ONE, BigInteger.ZERO),
                point.x,
                point.y
        );
    }
}
