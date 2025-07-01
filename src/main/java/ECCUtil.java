import constants.Params;
import operations.FP;
import operations.FP2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import types.*;

import java.math.BigInteger;

import static operations.FP2.*;


public class ECCUtil {
    private static final int W_FIXEDBASE = 5;
    private static final int V_FIXEDBASE = 5;
    private static final int D_FIXEDBASE = 54;
    private static final int E_FIXEDBASE = 10;
    private static final int L_FIXEDBASE = D_FIXEDBASE * W_FIXEDBASE;

    private static final F2Element F2_ONE = new F2Element(BigInteger.ONE, BigInteger.ONE);

    // Supporting data structure for recode result
    private static class RecodeResult {
        final int[] digits;
        final int[] signMasks;

        RecodeResult(int[] digits, int[] signMasks) {
            this.digits = digits;
            this.signMasks = signMasks;
        }
    }

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
        F2Element x = FP2.fp2div1271(fp2sub1271(p.x, p.y));
        F2Element y = FP2.fp2div1271(FP2.fp2add1271(p.x, p.y));
        return new ExtendedPoint<F2Element>(x, y, F2_ONE, x, y);
    }

    private static PreComputedExtendedPoint<F2Element> r1ToR2(ExtendedPoint<F2Element> point) {
        F2Element t = fp2sub1271(FP2.fp2add1271(point.ta, point.ta), point.tb);
        return new PreComputedExtendedPoint<>(
                FP2.fp2add1271(point.y, point.x),
                fp2sub1271(point.y, point.x),
                FP2.fp2add1271(point.z, point.z),
                fp2mul1271(t, convertToF2Element(Params.PARAMETER_D))
        );
    }

    private static PreComputedExtendedPoint<F2Element> r1ToR3(ExtendedPoint<F2Element> point) {
        return new PreComputedExtendedPoint<>(
                FP2.fp2add1271(point.x, point.y),
                fp2sub1271(point.y, point.x),
                fp2mul1271(point.ta, point.tb),
                point.z
        );
    }

    private static ExtendedPoint<F2Element> r2ToR4(PreComputedExtendedPoint<F2Element> p, ExtendedPoint<F2Element> q) {
        return new ExtendedPoint<>(
                FP2.fp2sub1271(p.xy, p.yx),
                FP2.fp2add1271(p.xy, p.yx),
                FP2.fp2copy1271(p.z),
                q.ta,
                q.tb
        );
    }

    private static ExtendedPoint<F2Element> eccMixedAdd(
            AffinePoint<F2Element> q,
            ExtendedPoint<F2Element> p
    ) {
        F2Element ta = fp2mul1271(p.ta, p.tb);          // Ta = T1
        F2Element t1 = FP2.fp2add1271(p.z, p.z);            // t1 = 2Z1
        ta = fp2mul1271(ta, q.t);                       // Ta = 2dT1*t2
        F2Element pz = FP2.fp2add1271(p.x, p.y);            // Z = (X1+Y1)
        F2Element tb = fp2sub1271(p.y, p.x);            // Tb = (Y1-X1)
        F2Element t2 = fp2sub1271(t1, ta);              // t2 = theta
        t1 = FP2.fp2add1271(t1, ta);                        // t1 = alpha
        ta = fp2mul1271(q.x, pz);                       // Ta = (X1+Y1)(x2+y2)
        F2Element x = fp2mul1271(q.y, tb);              // X = (Y1-X1)(y2-x2)
        tb = fp2sub1271(ta, x);                         // Tbfinal = beta
        ta = FP2.fp2add1271(ta, x);                         // Tafinal = omega
        return new ExtendedPoint<>(
                fp2mul1271(tb, t2),                     //Xfinal = beta*theta
                fp2mul1271(ta, t1),                     // Yfinal = alpha*omega
                fp2mul1271(t1, t2),                     // Zfinal = theta*alpha
                ta,
                tb
        );
    }

    // Point doubling 2P
    // Input: P = (X1:Y1:Z1) in twisted Edwards coordinates
    // Output: 2P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal,
    //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    private static ExtendedPoint<F2Element> eccDouble(ExtendedPoint<F2Element> p) {
        F2Element t1 = fp2sqr1271(p.x);                 // t1 = X1^2
        F2Element t2 = fp2sqr1271(p.y);                 // t2 = Y1^2
        F2Element t3 = FP2.fp2add1271(p.x, p.y);            // t3 = X1+Y1
        F2Element tb = FP2.fp2add1271(t1, t2);              // Tbfinal = X1^2+Y1^2
        t1 = fp2sub1271(t2, t1);                        // t1 = Y1^2-X1^2
        F2Element ta = fp2sqr1271(t3);                  // Ta = (X1+Y1)^2
        t2 = fp2sqr1271(p.z);                           // t2 = Z1^2
        ta = fp2sub1271(ta, tb);                        // Tafinal = 2X1*Y1 = (X1+Y1)^2-(X1^2+Y1^2)
        t2 = FP2.fp2addsub1271(t2, t1);                     // t2 = 2Z1^2-(Y1^2-X1^2)
        final F2Element y = fp2mul1271(t1, tb);         // Yfinal = (X1^2+Y1^2)(Y1^2-X1^2)
        final F2Element x = fp2mul1271(t2, ta);         // Xfinal = 2X1*Y1*[2Z1^2-(Y1^2-X1^2)]
        final F2Element z = fp2mul1271(t1, t2);         // Zfinal = (Y1^2-X1^2)[2Z1^2-(Y1^2-X1^2)]
        return new ExtendedPoint<>(x, y, z, ta, tb);
    }

    private static FieldPoint<F2Element> eccNorm(ExtendedPoint<F2Element> p) {
        final F2Element zInv = FP2.fp2inv1271(p.z);
        final F2Element x = fp2mul1271(p.x, zInv);
        final F2Element y = fp2mul1271(p.y, zInv);
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
        F2Element z = fp2mul1271(p.t, q.t);
        F2Element t1 = fp2mul1271(p.z, q.z);
        F2Element x = fp2mul1271(p.xy, q.xy);
        F2Element y = fp2mul1271(p.yx, q.yx);
        F2Element t2 = fp2sub1271(t1, z);
        t1 = FP2.fp2add1271(t1, z);
        F2Element tb = fp2sub1271(x, y);
        F2Element ta = FP2.fp2add1271(x, y);
        return new ExtendedPoint<>(
                fp2mul1271(tb, t2),
                fp2mul1271(ta, t1),
                fp2mul1271(t1, t2),
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

    /**
     * Point validation: check if point lies on the curve
     * @param p = (x,y) in affine coordinates, where x, y in [0, 2^127-1]
     * @return true if point lies on curve E: -x^2+y^2-1-dx^2*y^2 = 0, false otherwise
     *
     * @implNote this function does not run in constant time (input point P is assumed to be public)
     */
    public static boolean eccPointValidate(@NotNull AffinePoint<F2Element> p) {
        F2Element t1 = fp2sqr1271(p.y);                                 // y^2
        F2Element t2 = fp2sqr1271(p.x);                                 // x^2
        F2Element t3 = fp2sub1271(t1, t2);                              // y^2 - x^2 = -x^2 + y^2

        t1 = fp2mul1271(t1, t2);                                        // x^2*y^2
        t2 = fp2mul1271(convertToF2Element(Params.PARAMETER_D), t1);    // dx^2*y^2

        // Create F2Element representing 1 + 0i
        F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);
        t2 = fp2add1271(t2, one);                                       // 1 + dx^2*y^2
        t1 = fp2sub1271(t3, t2);                                        // -x^2 + y^2 - 1 - dx^2*y^2

        // Reduce modulo (2^127-1)
        t1 = new F2Element(
                FP.putil.mod1271(t1.real),
                FP.putil.mod1271(t1.im)
        );

        // Check if the result is zero (both real and imaginary parts must be zero) to be on the curve.
        return t1.real.equals(BigInteger.ZERO) && t1.im.equals(BigInteger.ZERO);
    }

    /**
     * Variable-base scalar multiplication Q = k*P using a 4-dimensional decomposition
     *
     * @param P point P = (x,y) in affine coordinates
     * @param K scalar "k" in [0, 2^256-1]
     * @param Q output point Q = k*P in affine coordinates (modified in place)
     * @param clearCofactor whether cofactor clearing is required
     * @return true if successful, false if point validation fails
     */
    @Contract(value = "null, _, _, _ -> fail; _, null, _, _ -> fail; _, _, null, _ -> fail", mutates = "param3")
    public static boolean eccMul(
            FieldPoint<F2Element> P,
            BigInteger K,
            AffinePoint<F2Element> Q,
            boolean clearCofactor
    ) {
        // Convert to representation (X, Y, 1, Ta, Tb)
        ExtendedPoint<F2Element> R = pointSetup(P);

        // Scalar decomposition into 4 scalars using endomorphisms
        BigInteger[] scalars = decompose(K);

        // Check if the point lies on the curve
        if (!eccPointValidate(R)) {
            return false;
        }

        // Optional cofactor clearing
        if (clearCofactor) { R = cofactorClearing(R); }

        // Scalar recoding for efficient computation
        RecodeResult recodeResult = recode(scalars);
        int[] digits = recodeResult.digits;
        int[] signMasks = recodeResult.signMasks;

        // Precomputation - create table of 8 precomputed points
        PreComputedExtendedPoint<F2Element>[] table = eccPrecomp(R);

        // Extract initial point in (X+Y,Y-X,2Z,2dT) representation
        PreComputedExtendedPoint<F2Element> S = tableLookup1x8(table, digits[64], signMasks[64]);

        // Convert to representation (2X,2Y,2Z) for doubling operations
        R = r2ToR4(S, R);

        // Main computation loop: double-and-add with precomputed table
        for (int i = 63; i >= 0; i--) {
            // Extract point S in (X+Y,Y-X,2Z,2dT) representation
            S = tableLookup1x8(table, digits[i], signMasks[i]);

            // Double: R = 2*R using (X,Y,Z,Ta,Tb) <- 2*(X,Y,Z)
            R = eccDouble(R);

            // Add: R = R+S using (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (X+Y,Y-X,2Z,2dT)
            R = eccAdd(S, R);
        }

        // Convert to affine coordinates (x,y) and store in output parameter Q
        FieldPoint<F2Element> result = eccNorm(R);
        Q.x = result.x;
        Q.y = result.y;

        return true;
    }

    /**
     * Generation of the precomputation table used by the variable-base scalar multiplication ecc_mul().
     * @param p = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates.
     * @return table T containing NPOINTS_VARBASE points: P, 3P, 5P, ... , (2*NPOINTS_VARBASE-1)P. NPOINTS_VARBASE is fixed to 8 (see FourQ.h).
     *         Precomputed points use the representation (X+Y,Y-X,2Z,2dT) corresponding to (X:Y:Z:T) in extended twisted Edwards coordinates.
     */
    private static PreComputedExtendedPoint<F2Element>[] eccPrecomp(@NotNull ExtendedPoint<F2Element> p) {
        // Initialize the output table
        @SuppressWarnings("unchecked")
        PreComputedExtendedPoint<F2Element>[] t
                = new PreComputedExtendedPoint[Params.NPOINTS_VARBASE.intValueExact()];

        PreComputedExtendedPoint<F2Element> p2;
        ExtendedPoint<F2Element> q;

        // Generating P2 = 2(X1,Y1,Z1,T1a,T1b) and T[0] = P
        q = eccCopy(p);                    // Copy P to Q
        t[0] = r1ToR2(p);                  // T[0] = P in (X+Y,Y-X,2Z,2dT) format
        q = eccDouble(q);                  // Q = 2P
        p2 = r1ToR3(q);                    // P2 = 2P in R3 format

        // Generate odd multiples: 3P, 5P, 7P, ..., (2*NPOINTS_VARBASE-1)P
        for (int i = 1; i < Params.NPOINTS_VARBASE.intValueExact(); i++) {
            // T[i] = 2P + T[i-1] = (2*i+1)P
            q = eccAddCore(p2, t[i-1]);    // Add 2P to previous odd multiple
            t[i] = r1ToR2(q);              // Convert result to R2 format
        }
    }

}
