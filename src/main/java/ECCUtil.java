import constants.Params;
import operations.FP;
import operations.FP2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import types.*;

import java.math.BigInteger;


public class ECCUtil {
    private static final int W_FIXEDBASE = 5;
    private static final int V_FIXEDBASE = 5;
    private static final int D_FIXEDBASE = 54;
    private static final int E_FIXEDBASE = 10;
    private static final int L_FIXEDBASE = D_FIXEDBASE * W_FIXEDBASE;

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


    public static int[] mLSBSetRecode(BigInteger scalar) {
        return null;
    }

    private static ExtendedPoint<F2Element> r5ToR1(AffinePoint<F2Element> p) {
        F2Element x = FP2.fp2div1271(FP2.fp2sub1271(p.x, p.y));
        F2Element y = FP2.fp2div1271(FP2.fp2add1271(p.x, p.y));
        return new ExtendedPoint<F2Element>(x, y, F2_ONE, x, y);
    }

    private static PreComputedExtendedPoint<F2Element> r1ToR2(ExtendedPoint<F2Element> point) {
        F2Element t = FP2.fp2sub1271(FP2.fp2add1271(point.ta, point.ta), point.tb);
        return new PreComputedExtendedPoint<>(
                FP2.fp2add1271(point.y, point.x),
                FP2.fp2sub1271(point.y, point.x),
                FP2.fp2add1271(point.z, point.z),
                FP2.fp2mul1271(t, convertToF2Element(Params.PARAMETER_D))
        );
    }

    private static PreComputedExtendedPoint<F2Element> r1ToR3(ExtendedPoint<F2Element> point) {
        return new PreComputedExtendedPoint<>(
                FP2.fp2add1271(point.x, point.y),
                FP2.fp2sub1271(point.y, point.x),
                FP2.fp2mul1271(point.ta, point.tb),
                point.z
        );
    }

    private static ExtendedPoint<F2Element> eccMixedAdd(
            AffinePoint<F2Element> q,
            ExtendedPoint<F2Element> p
    ) {
        F2Element ta = FP2.fp2mul1271(p.ta, p.tb);          // Ta = T1
        F2Element t1 = FP2.fp2add1271(p.z, p.z);            // t1 = 2Z1
        ta = FP2.fp2mul1271(ta, q.t);                       // Ta = 2dT1*t2
        F2Element pz = FP2.fp2add1271(p.x, p.y);            // Z = (X1+Y1)
        F2Element tb = FP2.fp2sub1271(p.y, p.x);            // Tb = (Y1-X1)
        F2Element t2 = FP2.fp2sub1271(t1, ta);              // t2 = theta
        t1 = FP2.fp2add1271(t1, ta);                        // t1 = alpha
        ta = FP2.fp2mul1271(q.x, pz);                       // Ta = (X1+Y1)(x2+y2)
        F2Element x = FP2.fp2mul1271(q.y, tb);              // X = (Y1-X1)(y2-x2)
        tb = FP2.fp2sub1271(ta, x);                         // Tbfinal = beta
        ta = FP2.fp2add1271(ta, x);                         // Tafinal = omega
        return new ExtendedPoint<>(
                FP2.fp2mul1271(tb, t2),                     //Xfinal = beta*theta
                FP2.fp2mul1271(ta, t1),                     // Yfinal = alpha*omega
                FP2.fp2mul1271(t1, t2),                     // Zfinal = theta*alpha
                ta,
                tb
        );
    }

    // Point doubling 2P
    // Input: P = (X1:Y1:Z1) in twisted Edwards coordinates
    // Output: 2P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal,
    //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    private static ExtendedPoint<F2Element> eccDouble(ExtendedPoint<F2Element> p) {
        F2Element t1 = FP2.fp2sqr1271(p.x);                 // t1 = X1^2
        F2Element t2 = FP2.fp2sqr1271(p.y);                 // t2 = Y1^2
        F2Element t3 = FP2.fp2add1271(p.x, p.y);            // t3 = X1+Y1
        F2Element tb = FP2.fp2add1271(t1, t2);              // Tbfinal = X1^2+Y1^2
        t1 = FP2.fp2sub1271(t2, t1);                        // t1 = Y1^2-X1^2
        F2Element ta = FP2.fp2sqr1271(t3);                  // Ta = (X1+Y1)^2
        t2 = FP2.fp2sqr1271(p.z);                           // t2 = Z1^2
        ta = FP2.fp2sub1271(ta, tb);                        // Tafinal = 2X1*Y1 = (X1+Y1)^2-(X1^2+Y1^2)
        t2 = FP2.fp2addsub1271(t2, t1);                     // t2 = 2Z1^2-(Y1^2-X1^2)
        final F2Element y = FP2.fp2mul1271(t1, tb);         // Yfinal = (X1^2+Y1^2)(Y1^2-X1^2)
        final F2Element x = FP2.fp2mul1271(t2, ta);         // Xfinal = 2X1*Y1*[2Z1^2-(Y1^2-X1^2)]
        final F2Element z = FP2.fp2mul1271(t1, t2);         // Zfinal = (Y1^2-X1^2)[2Z1^2-(Y1^2-X1^2)]
        return new ExtendedPoint<>(x, y, z, ta, tb);
    }

    private static FieldPoint<F2Element> eccNorm(ExtendedPoint<F2Element> p) {
        final F2Element zInv = FP2.fp2inv1271(p.z);
        final F2Element x = FP2.fp2mul1271(p.x, zInv);
        final F2Element y = FP2.fp2mul1271(p.y, zInv);
        return new FieldPoint<>(x, y);
    }

    public static FieldPoint<F2Element> eccMulDouble(
            BigInteger k,
            FieldPoint<F2Element> q, BigInteger l
    ) {
        // Step 1: Compute l*Q
        FieldPoint<F2Element> lQ = eccMul(q, l);
        if (lQ == null) { return null; }                    // Point validation failed

        // Step 2-3: Convert l*Q to precomputed format
        ExtendedPoint<F2Element> extLQ = pointSetup(lQ);
        PreComputedExtendedPoint<F2Element> precompLQ = r1ToR2(extLQ);

        // Step 4: Compute k*G (generator multiplication)
        FieldPoint<F2Element> kG = eccMulFixed(k);
        if (kG == null) { return null; }

        // Step 5-6: Add k*G + l*Q
        ExtendedPoint<F2Element> extKG = pointSetup(kG);
        ExtendedPoint<F2Element> result = eccAdd(precompLQ, extKG);

        // Step 7: Normalize to affine coordinates
        return eccNorm(result);
    }

    private static ExtendedPoint<F2Element> eccAddCore(
            PreComputedExtendedPoint<F2Element> p,
            PreComputedExtendedPoint<F2Element> q
    ) {
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

    private static ExtendedPoint<F2Element> eccAdd(
            PreComputedExtendedPoint<F2Element> q,
            ExtendedPoint<F2Element> p
    ) {
        return eccAddCore(q, r1ToR3(p));
    }

    private static FieldPoint<F2Element> eccMul(
            FieldPoint<F2Element> p,
            BigInteger k
    ) {
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

    /**
     * Computes the modified LSB-set representation of a scalar
     * @param inputScalar scalar in [0, order-1], where the order of FourQ's subgroup is 246 bits
     * @param digits output array where:
     *               - First d values (indices 0 to d-1) store signs: -1 (negative), 0 (positive)
     *               - Remaining values (indices d to l-1) store recoded values (excluding sign)
     */
    @Contract(value = "_, _ -> _", mutates = "param2")
    public static int @NotNull [] mLSBSetRecode(
            BigInteger inputScalar,
            int @NotNull [] digits
    ) {
        final int d = D_FIXEDBASE;                              // ceil(bitlength(order)/(w*v))*v

        BigInteger scalar = inputScalar;

        // Initialize
        digits[d-1] = 0;

        // Initial shift right by 1
        scalar = scalar.shiftRight(1);

        // Part 1: Extract signs for indices 0 to d-2
        for (int i = 0; i < d-1; i++) {
            // Extract LSB and convert to sign convention
            int lsb = scalar.testBit(0) ? 1 : 0;
            digits[i] = lsb - 1;                                // Convert: 0 -> -1 (negative), 1 -> 0 (positive)

            // Shift right by 1
            scalar = scalar.shiftRight(1);
        }

        // Part 2: Extract digits for indices d to l-1
        for (int i = d; i < L_FIXEDBASE; i++) {
            // Extract LSB as digit value
            digits[i] = scalar.testBit(0) ? 1 : 0;

            // Shift right by 1
            scalar = scalar.shiftRight(1);

            // Conditional addition based on sign
            int signIndex = i % d;                              // Equivalent to i-(i/d)*d
            int comp = (-digits[signIndex]) & digits[i];

            // Add temp to scalar (equivalent to floor(scalar/2) + comp)
            if (comp != 0) {
                scalar = scalar.add(BigInteger.ONE);
            }
        }

        return digits;
    }

    public static boolean eccMul(AffinePoint<F2Element> P, BigInteger K, AffinePoint<F2Element> Q, boolean clearCofactor) {

        return false;
    }

}
