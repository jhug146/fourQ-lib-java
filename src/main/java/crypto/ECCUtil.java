package crypto;

import constants.Params;
import constants.PregeneratedTables;
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
        private record RecodeResult(int[] digits, int[] signMasks) {

        /**
         * Get the effective signed digit at position i
         *
         * @param i position in the recoded representation
         * @return signed digit value
         */
            public int getSignedDigit(int i) {
                if (i < 0 || i >= digits.length) {
                    throw new IndexOutOfBoundsException("Index: " + i);
                }

                // If sign_mask[i] == 0xFFFFFFFF, digit is positive
                // If sign_mask[i] == 0x00000000, digit is negative
                return (signMasks[i] == -1) ? digits[i] : -digits[i];
            }
        }


    /**
     * Convert a precomputed 256-bit BigInteger to F2Element
     * The BigInteger represents a uint64_t[4] array: [0][1][2][3]
     * where [0,1] = real part and [2,3] = imaginary part
     *
     * @param precomputed 256-bit BigInteger from concatenated uint64_t[4] array
     * @return F2Element with proper real and imaginary parts
     */
    public static F2Element convertToF2Element(BigInteger precomputed) {
        // Mask for extracting 64-bit values
        BigInteger mask64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);

        // Extract the 4 original uint64_t values from the 256-bit BigInteger
        // BigInteger format: [0][1][2][3] (big-endian concatenation)
        BigInteger val3 = precomputed.and(mask64);                    // [3] - lowest 64 bits
        BigInteger val2 = precomputed.shiftRight(64).and(mask64);     // [2]
        BigInteger val1 = precomputed.shiftRight(128).and(mask64);    // [1]
        BigInteger val0 = precomputed.shiftRight(192).and(mask64);    // [0] - highest 64 bits

        // Combine pairs to form real and imaginary parts
        // Real part: combine [0] and [1] (little-endian within field element)
        BigInteger realPart = val0.add(val1.shiftLeft(64));

        // Imaginary part: combine [2] and [3] (little-endian within field element)
        BigInteger imagPart = val2.add(val3.shiftLeft(64));

        return new F2Element(realPart, imagPart);
    }

    // Set generator
    // Output: P = (x,y)
    public static void eccSet(AffinePoint<F2Element> P) {
        // Create generator coordinates with correct F2Element structure
        P.x = createGeneratorX();
        P.y = createGeneratorY();
    }

    /**
     * CORRECTED: Create generator X coordinate as proper F2Element
     */
    private static F2Element createGeneratorX() {
        // GENERATOR_x[4] = {0x286592AD7B3833AA, 0x1A3472237C2FB305, 0x96869FB360AC77F6, 0x1E1F553F2878AA9C}
        // [0,1] = real part, [2,3] = imaginary part

        // Real part: combine [0] and [1]
        BigInteger realPart = combineUint64Pair(
                new BigInteger("286592AD7B3833AA", 16),
                new BigInteger("1A3472237C2FB305", 16)
        );

        // Imaginary part: combine [2] and [3]
        BigInteger imagPart = combineUint64Pair(
                new BigInteger("96869FB360AC77F6", 16),
                new BigInteger("1E1F553F2878AA9C", 16)
        );

        return new F2Element(realPart, imagPart);
    }

    /**
     * CORRECTED: Create generator Y coordinate as proper F2Element
     */
    private static F2Element createGeneratorY() {
        // GENERATOR_y[4] = {0xB924A2462BCBB287, 0x0E3FEE9BA120785A, 0x49A7C344844C8B5C, 0x6E1C4AF8630E0242}
        // [0,1] = real part, [2,3] = imaginary part

        // Real part: combine [0] and [1]
        BigInteger realPart = combineUint64Pair(
                new BigInteger("B924A2462BCBB287", 16),
                new BigInteger("0E3FEE9BA120785A", 16)
        );

        // Imaginary part: combine [2] and [3]
        BigInteger imagPart = combineUint64Pair(
                new BigInteger("49A7C344844C8B5C", 16),
                new BigInteger("6E1C4AF8630E0242", 16)
        );

        return new F2Element(realPart, imagPart);
    }
    /**
     * Combine two uint64_t values into a single field element
     * For FourQ: each field element is in F_{2^127-1}, stored in 2 uint64_t values
     */
    private static BigInteger combineUint64Pair(BigInteger low, BigInteger high) {
        // Check if this is little-endian or big-endian combination
        // For FourQ, typically: result = low + (high << 64)
        return low.add(high.shiftLeft(64));
    }

    /*
    public static FieldPoint<F2Element> eccMulFixed(BigInteger val) {
            int w = W_FIXEDBASE, v = V_FIXEDBASE, d = D_FIXEDBASE, e = E_FIXEDBASE;

        BigInteger temp = FP.moduloOrder(val);
        temp = FP.conversionToOdd(temp);
        int[] digits = mLSBSetRecode(temp, new int[270]);  // TODO: No idea how this works
        int digit = digits[W_FIXEDBASE * D_FIXEDBASE - 1];
        int startI = (W_FIXEDBASE - 1) * D_FIXEDBASE - 1;
        for (int i = startI; i >= 2 * D_FIXEDBASE - 1; i -= D_FIXEDBASE) {
            digit = 2 * digit + digits[i];
        }

        // TODO: Both instances of TABLE in this function might need updating
//        AffinePoint<F2Element> affPoint = Table.tableLookupFixedBase(digit, digits[D_FIXEDBASE - 1]);
        PreComputedExtendedPoint<F2Element> affPoint = Table.FixedBaseTableLookup.performTableLookup(v, w, digit, digits, d);

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
     */ // <- Previous eccMulFixed Implementation

    // Alternative version using the specific methods:
    public static FieldPoint<F2Element> eccMulFixed(BigInteger val) {
        int w = W_FIXEDBASE, v = V_FIXEDBASE, d = D_FIXEDBASE, e = E_FIXEDBASE;

        BigInteger temp = FP.moduloOrder(val);
        temp = FP.conversionToOdd(temp);
        int[] digits = mLSBSetRecode(temp, new int[270]);

        // Extracting initial digit
        int digit = digits[w * d - 1];
        int startI = (w - 1) * d - 1;
        for (int i = startI; i >= 2 * d - 1; i -= d) {
            digit = 2 * digit + digits[i];
        }

        // Initialize with initial table lookup
        PreComputedExtendedPoint<F2Element> S = Table.FixedBaseTableLookup.performTableLookupInitial(
                v, w, digit, digits, d);
        ExtendedPoint<F2Element> R = r5ToR1(S.toAffinePoint());

        // First loop
        for (int j = 0; j < v - 1; j++) {
            digit = digits[w * d - (j + 1) * e - 1];
            int iStart = (w - 1) * d - (j + 1) * e - 1;
            int iMin = 2 * d - (j + 1) * e - 1;
            for (int i = iStart; i >= iMin; i -= d) {
                digit = 2 * digit + digits[i];
            }

            S = Table.FixedBaseTableLookup.performTableLookupFirstLoop(v, w, j, e, d, digit, digits);
            R = eccMixedAdd(S.toAffinePoint(), R);
        }

        // Second nested loop
        for (int ii = e - 2; ii >= 0; ii--) {
            R = eccDouble(R);
            for (int j = 0; j < v; j++) {
                digit = digits[w * d - j * e + ii - e];
                int kStart = (w - 1) * d - j * e + ii - e;
                int kMin = 2 * d - j * e + ii - e;
                for (int k = kStart; k >= kMin; k -= d) {
                    digit = 2 * digit + digits[k];
                }

                S = Table.FixedBaseTableLookup.performTableLookupSecondLoop(v, w, j, e, d, ii, digit, digits);
                R = eccMixedAdd(S.toAffinePoint(), R);
            }
        }

        return eccNorm(R);
    }


    private static ExtendedPoint<F2Element> r5ToR1(AffinePoint<F2Element> p) {
        F2Element x = fp2Div1271(fp2Sub1271(p.x, p.y));
        F2Element y = fp2Div1271(fp2Add1271(p.x, p.y));
        return new ExtendedPoint<F2Element>(x, y, F2_ONE, x, y);
    }

    private static PreComputedExtendedPoint<F2Element> r1ToR2(ExtendedPoint<F2Element> point) {
        F2Element t = fp2Sub1271(fp2Add1271(point.ta, point.ta), point.tb);
        return new PreComputedExtendedPoint<>(
                fp2Add1271(point.y, point.x),
                fp2Sub1271(point.y, point.x),
                fp2Add1271(point.z, point.z),
                fp2Mul1271(t, convertToF2Element(Params.PARAMETER_D))
        );
    }

    private static PreComputedExtendedPoint<F2Element> r1ToR3(ExtendedPoint<F2Element> point) {
        return new PreComputedExtendedPoint<>(
                fp2Add1271(point.x, point.y),
                fp2Sub1271(point.y, point.x),
                fp2Mul1271(point.ta, point.tb),
                point.z
        );
    }

    @NotNull
    private static ExtendedPoint<F2Element> r2ToR4(@NotNull PreComputedExtendedPoint<F2Element> p, @NotNull ExtendedPoint<F2Element> q) {
        return new ExtendedPoint<>(
                FP2.fp2Sub1271(p.xy, p.yx),
                FP2.fp2Add1271(p.xy, p.yx),
                FP2.fp2Copy1271(p.z),
                q.ta,
                q.tb
        );
    }

    private static ExtendedPoint<F2Element> eccMixedAdd(
            AffinePoint<F2Element> q,
            ExtendedPoint<F2Element> p
    ) {
        F2Element ta = fp2Mul1271(p.ta, p.tb);          // Ta = T1
        F2Element t1 = fp2Add1271(p.z, p.z);            // t1 = 2Z1
        ta = fp2Mul1271(ta, q.t);                       // Ta = 2dT1*t2
        F2Element pz = fp2Add1271(p.x, p.y);            // Z = (X1+Y1)
        F2Element tb = fp2Sub1271(p.y, p.x);            // Tb = (Y1-X1)
        F2Element t2 = fp2Sub1271(t1, ta);              // t2 = theta
        t1 = fp2Add1271(t1, ta);                        // t1 = alpha
        ta = fp2Mul1271(q.x, pz);                       // Ta = (X1+Y1)(x2+y2)
        F2Element x = fp2Mul1271(q.y, tb);              // X = (Y1-X1)(y2-x2)
        tb = fp2Sub1271(ta, x);                         // Tbfinal = beta
        ta = fp2Add1271(ta, x);                         // Tafinal = omega
        return new ExtendedPoint<>(
                fp2Mul1271(tb, t2),                     // Xfinal = beta*theta
                fp2Mul1271(ta, t1),                     // Yfinal = alpha*omega
                fp2Mul1271(t1, t2),                     // Zfinal = theta*alpha
                ta,
                tb
        );
    }

    // Point doubling 2P
    // Input: P = (X1:Y1:Z1) in twisted Edwards coordinates
    // Output: 2P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal,
    //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    private static ExtendedPoint<F2Element> eccDouble(ExtendedPoint<F2Element> p) {
        F2Element t1 = fp2Sqr1271(p.x);                 // t1 = X1^2
        F2Element t2 = fp2Sqr1271(p.y);                 // t2 = Y1^2
        F2Element t3 = fp2Add1271(p.x, p.y);            // t3 = X1+Y1
        F2Element tb = fp2Add1271(t1, t2);              // Tbfinal = X1^2+Y1^2
        t1 = fp2Sub1271(t2, t1);                        // t1 = Y1^2-X1^2
        F2Element ta = fp2Sqr1271(t3);                  // Ta = (X1+Y1)^2
        t2 = fp2Sqr1271(p.z);                           // t2 = Z1^2
        ta = fp2Sub1271(ta, tb);                        // Tafinal = 2X1*Y1 = (X1+Y1)^2-(X1^2+Y1^2)
        t2 = fp2AddSub1271(t2, t1);                     // t2 = 2Z1^2-(Y1^2-X1^2)
        final F2Element y = fp2Mul1271(t1, tb);         // Yfinal = (X1^2+Y1^2)(Y1^2-X1^2)
        final F2Element x = fp2Mul1271(t2, ta);         // Xfinal = 2X1*Y1*[2Z1^2-(Y1^2-X1^2)]
        final F2Element z = fp2Mul1271(t1, t2);         // Zfinal = (Y1^2-X1^2)[2Z1^2-(Y1^2-X1^2)]
        return new ExtendedPoint<>(x, y, z, ta, tb);
    }

    private static FieldPoint<F2Element> eccNorm(ExtendedPoint<F2Element> p) {
        final F2Element zInv = fp2Inv1271(p.z);
        final F2Element x = fp2Mul1271(p.x, zInv);
        final F2Element y = fp2Mul1271(p.y, zInv);
        return new FieldPoint<>(x, y);
    }

    /**
     * Double scalar multiplication R = k*G + l*Q, where G is the generator.
     *
     * @param k scalar "k" in [0, 2^256-1]
     * @param q point Q in affine coordinates
     * @param l scalar "l" in [0, 2^256-1]
     * @return R = k*G + l*Q in affine coordinates (x,y), or null if point validation fails
     *
     * @implNote This function is intended for non-constant-time operations such as signature verification.
     */
    public static FieldPoint<F2Element> eccMulDouble(
            BigInteger k,
            FieldPoint<F2Element> q,
            BigInteger l
    ) {
        // Step 1: Compute l*Q
        AffinePoint<F2Element> A = new AffinePoint<>();
        if (!eccMul(q, l, A, false)) {
            return null; // Point validation failed
        }

        // Step 2: Convert l*Q to extended projective coordinates
        ExtendedPoint<F2Element> T = pointSetup(new FieldPoint<>(A.x, A.y));

        // Step 3: Convert to precomputed representation (X+Y,Y-X,2Z,2dT)
        PreComputedExtendedPoint<F2Element> S = r1ToR2(T);

        // Step 4: Compute k*G (generator multiplication)
        FieldPoint<F2Element> kG = eccMulFixed(k);
        if (kG == null) {
            return null;
        }

        // Step 5: Convert k*G to extended projective coordinates
        T = pointSetup(kG);

        // Step 6: Add l*Q + k*G: T = S + T
        T = eccAdd(S, T);

        // Step 7: Convert to affine coordinates (x,y) and return
        return eccNorm(T);
    }

    private static ExtendedPoint<F2Element> eccAddCore(
            PreComputedExtendedPoint<F2Element> p,
            PreComputedExtendedPoint<F2Element> q
    ) {
        F2Element z = fp2Mul1271(p.t, q.t);
        F2Element t1 = fp2Mul1271(p.z, q.z);
        F2Element x = fp2Mul1271(p.xy, q.xy);
        F2Element y = fp2Mul1271(p.yx, q.yx);
        F2Element t2 = fp2Sub1271(t1, z);
        t1 = fp2Add1271(t1, z);
        F2Element tb = fp2Sub1271(x, y);
        F2Element ta = fp2Add1271(x, y);
        return new ExtendedPoint<>(
                fp2Mul1271(tb, t2),
                fp2Mul1271(ta, t1),
                fp2Mul1271(t1, t2),
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

    public static ExtendedPoint<F2Element> pointSetup(FieldPoint<F2Element> point) {
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
    public static boolean eccPointValidate(ExtendedPoint<F2Element> p) {
        if (isPointAtInfinity(p)) {
            return true;
        }

        F2Element x2 = fp2Sqr1271(p.x);      // X^2
        F2Element y2 = fp2Sqr1271(p.y);      // Y^2
        F2Element z2 = fp2Sqr1271(p.z);      // Z^2
        F2Element z4 = fp2Sqr1271(z2);       // Z^4

        F2Element y2z2 = fp2Mul1271(y2, z2); // Y^2*Z^2
        F2Element x2z2 = fp2Mul1271(x2, z2); // X^2*Z^2
        F2Element t3 = fp2Sub1271(y2z2, x2z2); // Y^2*Z^2 - X^2*Z^2 = -X^2*Z^2 + Y^2*Z^2

        F2Element x2y2 = fp2Mul1271(x2, y2); // X^2*Y^2
        F2Element dx2y2 = fp2Mul1271(convertToF2Element(Params.PARAMETER_D), x2y2); // d*X^2*Y^2

        F2Element rhs = fp2Add1271(z4, dx2y2); // Z^4 + d*X^2*Y^2
        F2Element result = fp2Sub1271(t3, rhs); // -X^2*Z^2 + Y^2*Z^2 - Z^4 - d*X^2*Y^2

        // Reduce and check
        result = new F2Element(
                result.real = FP.PUtil.mod1271(result.real),
                result.im = FP.PUtil.mod1271(result.im)
        );

        System.out.println(result);
        return result.real.equals(BigInteger.ZERO) && result.im.equals(BigInteger.ZERO);
    }

    private static boolean isPointAtInfinity(ExtendedPoint<F2Element> p) {
        F2Element zero = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);

        // Method 1: Check if it equals (0:1:1:0:anything) in extended coordinates
        if (fp2IsEqual(p.x, zero) && fp2IsEqual(p.y, one) && fp2IsEqual(p.z, one)) {
            return true;
        }

        // Method 2: More robust - convert to affine and check (0, 1)
        if (fp2IsEqual(p.z, zero)) {
            // Special case: Z = 0 (different infinity representation)
            return true;
        }

        // Convert to affine coordinates and check if (0, 1)
        F2Element zInv = fp2Inv1271(p.z);
        F2Element x_affine = fp2Mul1271(p.x, zInv);
        F2Element y_affine = fp2Mul1271(p.y, zInv);

        return fp2IsEqual(x_affine, zero) && fp2IsEqual(y_affine, one);
    }

    private static boolean fp2IsEqual(F2Element a, F2Element b) {
        return a.real.equals(b.real) && a.im.equals(b.im);
    }

//    /**
//     * Variable-base scalar multiplication Q = k*P using a 4-dimensional decomposition
//     *
//     * @param P point P = (x,y) in affine coordinates
//     * @param K scalar "k" in [0, 2^256-1]
//     * @param res output point Q = k*P in affine coordinates (modified in place)
//     * @param clearCofactor whether cofactor clearing is required
//     * @return true if successful, false if point validation fails
//     */
//    //@Contract(value = "null, _, _, _ -> fail; _, null, _, _ -> fail; _, _, null, _ -> fail", mutates = "param3")
//    public static boolean eccMul(
//            FieldPoint<F2Element> P,
//            BigInteger K,
//            AffinePoint<F2Element> res,
//            boolean clearCofactor // Equivalent to the C Flag
//    ) {
//        // Convert to representation (X, Y, 1, Ta, Tb)
//        ExtendedPoint<F2Element> R = pointSetup(P);
//
//        // Scalar decomposition into 4 scalars using endomorphisms
//        BigInteger[] scalars = decompose(K);
//
//        // Check if the point lies on the curve
//        if (!eccPointValidate(R)) {
//            return false;
//        }
//
//        // Optional cofactor clearing
//        if (clearCofactor) { R = cofactorClearing(R); }
//
//        // Scalar recoding for efficient computation
//        RecodeResult recodeResult = recode(scalars);
//        int[] digits = recodeResult.digits;
//        int[] signMasks = recodeResult.signMasks;
//
//        // Precomputation - create table of 8 precomputed points
//        PreComputedExtendedPoint<F2Element>[] table = eccPrecomp(R);
//
//        // Extract initial point in (X+Y,Y-X,2Z,2dT) representation
//        assert table != null;
//        PreComputedExtendedPoint<F2Element> S = Table.tableLookup1x8(table, digits[64], signMasks[64]);
//
//        // Convert to representation (2X,2Y,2Z) for doubling operations
//        R = r2ToR4(S, R);
//
//        // Main computation loop: double-and-add with precomputed table
//        for (int i = 63; i >= 0; i--) {
//            // Extract point S in (X+Y,Y-X,2Z,2dT) representation
//            S = Table.tableLookup1x8(table, digits[i], signMasks[i]);
//
//            // Double: R = 2*R using (X,Y,Z,Ta,Tb) <- 2*(X,Y,Z)
//            R = eccDouble(R);
//
//            // Add: R = R+S using (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (X+Y,Y-X,2Z,2dT)
//            R = eccAdd(S, R);
//        }
//
//        // Convert to affine coordinates (x,y) and store in output parameter Q
//        FieldPoint<F2Element> result = eccNorm(R);
//        res.x = result.x;
//        res.y = result.y;
//
//        return true;
//    }

    /**
     * Simple scalar multiplication Q = k*P
     * This replaces the complex 4D GLV (above) with the simpler windowed method from C
     *
     * @param P point P = (x,y) in affine coordinates
     * @param k scalar "k" in [0, 2^256-1]
     * @param Q output point Q = k*P in affine coordinates (modified in place)
     * @param clearCofactor whether cofactor clearing is required
     * @return true if successful, false if point validation fails
     */
    public static boolean eccMul(
            FieldPoint<F2Element> P,
            BigInteger k,
            AffinePoint<F2Element> Q,
            boolean clearCofactor
    ) {
        // Convert to representation (X,Y,1,Ta,Tb)
        ExtendedPoint<F2Element> R = pointSetup(P);

        // Check if point lies on the curve
        if (!eccPointValidate(R)) {
            return false;
        }

        // Optional cofactor clearing
        if (clearCofactor) {
            R = cofactorClearing(R);
        }

        // k_odd = k mod (order)
        BigInteger k_odd = FP.moduloOrder(k);

        // Converting scalar to odd using the prime subgroup order
        k_odd = FP.conversionToOdd(k_odd);

        // Precomputation of points T[0],...,T[npoints-1]
        PreComputedExtendedPoint<F2Element>[] table = eccPrecomp(R);

        // Scalar recoding - use fixed window instead of 4D GLV
        FixedWindowRecodeResult recodeResult = fixedWindowRecode(k_odd);
        int[] digits = recodeResult.digits;
        int[] signMasks = recodeResult.signMasks;

        // Initial table lookup and conversion
        PreComputedExtendedPoint<F2Element> S = Table.tableLookup1x8(table, digits[Params.t_VARBASE], signMasks[Params.t_VARBASE]);
        R = r2ToR4(S, R);  // Conversion to representation (2X,2Y,2Z)

        // Main computation loop
        for (int i = Params.t_VARBASE - 1; i >= 0; i--) {
            // Single double
            R = eccDouble(R);

            // Extract point in (X+Y,Y-X,2Z,2dT) representation
            S = Table.tableLookup1x8(table, digits[i], signMasks[i]);

            // Triple double (total 8x multiplication per iteration)
            R = eccDouble(R);
            R = eccDouble(R);
            R = eccDouble(R);

            // Add: P = P+S
            R = eccAdd(S, R);
        }

        // Convert to affine coordinates (x,y)
        FieldPoint<F2Element> result = eccNorm(R);
        Q.x = result.x;
        Q.y = result.y;

        return true;
    }

// ========================================
// SUPPORTING CLASSES AND METHODS
// ========================================

    /**
     * Result structure for fixed window recoding
     */
    private static class FixedWindowRecodeResult {
        final int[] digits;
        final int[] signMasks;

        FixedWindowRecodeResult(int[] digits, int[] signMasks) {
            this.digits = digits;
            this.signMasks = signMasks;
        }
    }

    /**
     * Window recoding for scalar multiplication
     *
     * @param k_odd the scalar converted to odd form
     * @return FixedWindowRecodeResult containing digits and sign masks
     */
    private static FixedWindowRecodeResult fixedWindowRecode(BigInteger k_odd) {
        // Initialize arrays - size should match C constant t_VARBASE+1
        int[] digits = new int[Params.t_VARBASE + 1];
        int[] signMasks = new int[Params.t_VARBASE + 1];

        // Convert scalar to byte array for bit manipulation
        byte[] scalarBytes = k_odd.toByteArray();

        // Window size (typically 4 for 1x8 table = 2^3 = 8 precomputed points)
        int windowSize = 3; // log2(8) = 3

        // Process each window
        for (int i = 0; i <= Params.t_VARBASE; i++) {
            int bitPos = i * windowSize;
            int digit = extractBits(scalarBytes, bitPos, windowSize);

            // Handle signed representation for odd scalars
            if (digit == 0) {
                digits[i] = 0;
                signMasks[i] = 0;
            } else {
                // Convert to signed odd form
                if (digit % 2 == 0) {
                    digit++; // Make odd
                }

                if (digit > (1 << (windowSize - 1))) {
                    digits[i] = (1 << windowSize) - digit;
                    signMasks[i] = 0; // Negative
                } else {
                    digits[i] = digit;
                    signMasks[i] = -1; // Positive (0xFFFFFFFF)
                }
            }
        }

        return new FixedWindowRecodeResult(digits, signMasks);
    }


    /**
     * Extract bits from byte array starting at bitPos
     */
    private static int extractBits(byte[] data, int bitPos, int numBits) {
        int result = 0;

        for (int i = 0; i < numBits; i++) {
            int byteIndex = (bitPos + i) / 8;
            int bitIndex = (bitPos + i) % 8;

            if (byteIndex < data.length) {
                // Extract bit (LSB first)
                int bit = (data[data.length - 1 - byteIndex] >> bitIndex) & 1;
                result |= (bit << i);
            }
        }

        return result;
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

        return t;
    }

    /**
     * Copy extended projective point source to destination
     * Equivalent to C macro: ecccopy(source, dest)
     * @param source the point to copy from Q = (X:Y:Z:Ta:Tb)
     * @return dest the point to copy to P = (X:Y:Z:Ta:Tb)
     */
    public static ExtendedPoint<F2Element> eccCopy(
            ExtendedPoint<F2Element> source
    ) {
        return new ExtendedPoint<F2Element>(
                fp2Copy1271(source.x),
                fp2Copy1271(source.y),
                fp2Copy1271(source.z),
                fp2Copy1271(source.ta),
                fp2Copy1271(source.tb)
        );
    }

    /**
     * Scalar decomposition for variable-base scalar multiplication using 4-dimensional GLV
     * @param k - scalar in the range [0, 2^256-1]
     * @return 4 sub-scalars for efficient scalar multiplication
     */
    public static BigInteger[] decompose(BigInteger k) {
        // Phase 1: Compute initial coefficients using truncated multiplication
        BigInteger a1 = mulTruncate(k, Params.ELL1);
        BigInteger a2 = mulTruncate(k, Params.ELL2);
        BigInteger a3 = mulTruncate(k, Params.ELL3);
        BigInteger a4 = mulTruncate(k, Params.ELL4);

        // Phase 2: Compute first scalar with parity adjustment
        BigInteger temp = k
                .subtract(a1.multiply(Params.B11))
                .subtract(a2.multiply(Params.B21))
                .subtract(a3.multiply(Params.B31))
                .subtract(a4.multiply(Params.B41))
                .add(Params.C1);

        // Phase 3: Parity check and conditional adjustment
        // If temp is even then mask = 0xFF...FF, else mask = 0
        boolean isEven = !temp.testBit(0);
        BigInteger mask = isEven ? Params.MASK_ALL_ONES : BigInteger.ZERO;

        // Phase 4: Compute the 4 decomposed scalars
        BigInteger[] scalars = new BigInteger[4];

        scalars[0] = temp.add(mask.and(Params.B41));

        scalars[1] = a1.multiply(Params.B12)
                .add(a2)
                .subtract(a3.multiply(Params.B32))
                .subtract(a4.multiply(Params.B42))
                .add(Params.C2)
                .add(mask.and(Params.B42));

        scalars[2] = a3.multiply(Params.B33)
                .subtract(a1.multiply(Params.B13))
                .subtract(a2)
                .add(a4.multiply(Params.B43))
                .add(Params.C3)
                .subtract(mask.and(Params.B43));

        scalars[3] = a1.multiply(Params.B14)
                .subtract(a2.multiply(Params.B24))
                .subtract(a3.multiply(Params.B34))
                .add(a4.multiply(Params.B44))
                .add(Params.C4)
                .subtract(mask.and(Params.B44));

        return scalars;
    }

    /**
     * Truncated multiplication: computes floor((k * ell) / 2^256)
     * This extracts the high bits of the multiplication
     */
    private static BigInteger mulTruncate(BigInteger k, BigInteger ell) {
        BigInteger product = k.multiply(ell);
        return product.shiftRight(256);  // Equivalent to dividing by 2^256
    }

    /**
     * Recoding sub-scalars for use in variable-base scalar multiplication
     * Based on Algorithm 1 in "Efficient and Secure Methods for GLV-Based Scalar Multiplication"
     *
     * @param scalars 4 64-bit sub-scalars obtained from decompose()
     * @return RecodeResult containing digits and sign_masks arrays
     */
    private static RecodeResult recode(BigInteger[] scalars) {
        if (scalars == null || scalars.length != 4) {
            throw new IllegalArgumentException("Expected exactly 4 scalars");
        }

        int[] digits = new int[65];
        int[] signMasks = new int[65];

        // Work with mutable copies, given that BigInteger is immutable
        BigInteger[] workingScalars = new BigInteger[4];
        for (int i = 0; i < 4; i++) {
            workingScalars[i] = scalars[i] != null ? scalars[i] : BigInteger.ZERO;
        }

        // Initialize final sign mask
        signMasks[64] = -1;                                                     // 0xFFFFFFFF (all bits set)

        // Process 64 iterations
        for (int i = 0; i < 64; i++) {
            // Extract and process scalar[0]
            workingScalars[0] = workingScalars[0].shiftRight(1);
            int bit0 = workingScalars[0].testBit(0) ? 1 : 0;

            // Create sign mask: if bit0=1 then 0xFFFFFFFF, else 0x00000000
            signMasks[i] = -bit0;

            // Process scalar[1] and build digit
            int bit1 = workingScalars[1].testBit(0) ? 1 : 0;
            int carry1 = (bit0 | bit1) ^ bit0;
            workingScalars[1] = workingScalars[1].shiftRight(1).add(BigInteger.valueOf(carry1));
            digits[i] = bit1;

            // Process scalar[2] and add to digit
            int bit2 = workingScalars[2].testBit(0) ? 1 : 0;
            int carry2 = (bit0 | bit2) ^ bit0;
            workingScalars[2] = workingScalars[2].shiftRight(1).add(BigInteger.valueOf(carry2));
            digits[i] += (bit2 << 1);                                           // bit2 * 2

            // Process scalar[3] and add to digit
            int bit3 = workingScalars[3].testBit(0) ? 1 : 0;
            int carry3 = (bit0 | bit3) ^ bit0;
            workingScalars[3] = workingScalars[3].shiftRight(1).add(BigInteger.valueOf(carry3));
            digits[i] += (bit3 << 2);                                           // bit3 * 4
        }

        // Compute the final digit from remaining scalar bits
        BigInteger finalDigit = workingScalars[1]
                .add(workingScalars[2].shiftLeft(1))                         // scalars[2] * 2
                .add(workingScalars[3].shiftLeft(2));                        // scalars[3] * 4

        digits[64] = finalDigit.intValue();

        return new RecodeResult(digits, signMasks);
    }

    /**
     * Co-factor clearing operation for elliptic curve points.
     * @param p the input point P = (X₁,Y₁,Z₁,Ta,Tb) in extended twisted Edwards coordinates,
     *          where T₁ = Ta×Tb corresponds to (X₁:Y₁:Z₁:T₁)
     */
    private static ExtendedPoint<F2Element> cofactorClearing(ExtendedPoint<F2Element> p) {
        PreComputedExtendedPoint<F2Element> q = r1ToR2(p);  // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,2Z,2dT)

        p = eccDouble(p);                                   // P = 2*P using representations (X,Y,Z,Ta,Tb) <- 2*(X,Y,Z)
        p = eccAdd(q, p);                                   // P = P+Q using representations (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (X+Y,Y-X,2Z,2dT)
        p = eccDouble(p);
        p = eccDouble(p);
        p = eccDouble(p);
        p = eccDouble(p);
        p = eccAdd(q, p);
        p = eccDouble(p);
        p = eccDouble(p);
        p = eccDouble(p);

        return p;
    }
}
