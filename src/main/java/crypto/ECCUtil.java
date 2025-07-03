package crypto;

import constants.Params;
import exceptions.EncryptionException;
import exceptions.TableLookupException;
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

    // Set generator
    // Output: P = (x,y)
    // TODO VeRy unsure about this and the helper
    public static void eccSet(AffinePoint P) {
        P.x = convertToF2Element(Params.GENERATOR_X);    // X1
        P.y = convertToF2Element(Params.GENERATOR_Y);    // Y1
    }

    // Helper method to convert BigInteger to F2Element
    public static F2Element convertToF2Element(BigInteger generator) {
        // Split the 256-bit generator into two 127-bit parts for GF(p²)
        BigInteger realPart = generator.and(Params.MASK_127);                           // Lower 127 bits
        BigInteger imagPart = generator.shiftRight(127).and(Params.MASK_127);        // Upper 127 bits

        return new F2Element(realPart, imagPart);
    }

    @NotNull
    public static FieldPoint eccMulFixed(BigInteger val) {
        BigInteger temp = FP.moduloOrder(val);
        temp = FP.conversionToOdd(temp);
        int[] digits = mLSBSetRecode(temp, new int[270]);  // TODO: No idea how this works
        int digit = digits[W_FIXEDBASE * D_FIXEDBASE - 1];
        int startI = (W_FIXEDBASE - 1) * D_FIXEDBASE - 1;
        for (int i = startI; i >= 2 * D_FIXEDBASE - 1; i -= D_FIXEDBASE) {
            digit = 2 * digit + digits[i];
        }

        // TODO: Both instances of TABLE in this function might need updating
        AffinePoint affPoint = new AffinePoint();
        try {
            Table.tableLookup(
                    (V_FIXEDBASE - 1) * (1 << (W_FIXEDBASE - 1)),
                    digit,
                    digits[D_FIXEDBASE - 1],
                    affPoint
            );
        } catch (TableLookupException e) {
            throw EncryptionException("Error with table lookup");
        }
        ExtendedPoint exPoint = r5ToR1(affPoint);

        for (int j = 0; j < V_FIXEDBASE - 1; j++) {
            digit = digits[W_FIXEDBASE * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1];
            int iStart = (W_FIXEDBASE - 1) * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            int iMin = 2 * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            for (int i = iStart; i >= iMin; i -= D_FIXEDBASE) {
                digit = 2 * digit + digits[i];
            }
            // Extract point in (x+y,y-x,2dt) representation
            int signDigit = D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            affPoint = Table.tableLookup(, V_FIXEDBASE, digit, digits[signDigit]);
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
                affPoint = Table.tableLookup(, V_FIXEDBASE, digit, signDigit);
                exPoint = eccMixedAdd(affPoint, exPoint);
            }
        }
        return eccNorm(exPoint);
    }

    private static ExtendedPoint r5ToR1(AffinePoint p) {
        F2Element x = fp2Div1271(fp2Sub1271(p.x, p.y));
        F2Element y = fp2Div1271(fp2Add1271(p.x, p.y));
        return new ExtendedPoint(x, y, F2_ONE, x, y);
    }

    private static PreComputedExtendedPoint r1ToR2(ExtendedPoint point) {
        F2Element t = fp2Sub1271(fp2Add1271(point.ta, point.ta), point.tb);
        return new PreComputedExtendedPoint(
                fp2Add1271(point.y, point.x),
                fp2Sub1271(point.y, point.x),
                fp2Add1271(point.z, point.z),
                fp2Mul1271(t, convertToF2Element(Params.PARAMETER_D))
        );
    }

    private static PreComputedExtendedPoint r1ToR3(ExtendedPoint point) {
        return new PreComputedExtendedPoint(
                fp2Add1271(point.x, point.y),
                fp2Sub1271(point.y, point.x),
                fp2Mul1271(point.ta, point.tb),
                point.z
        );
    }

    @NotNull
    private static ExtendedPoint r2ToR4(@NotNull PreComputedExtendedPoint p, @NotNull ExtendedPoint q) {
        return new ExtendedPoint(
                FP2.fp2Sub1271(p.xy, p.yx),
                FP2.fp2Add1271(p.xy, p.yx),
                FP2.fp2Copy1271(p.z),
                q.ta,
                q.tb
        );
    }

    private static ExtendedPoint eccMixedAdd(
            AffinePoint q,
            ExtendedPoint p
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
    private static ExtendedPoint eccDouble(ExtendedPoint p) {
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
        return new ExtendedPoint(x, y, z, ta, tb);
    }

    private static FieldPoint eccNorm(ExtendedPoint p) {
        final F2Element zInv = fp2Inv1271(p.z);
        final F2Element x = fp2Mul1271(p.x, zInv);
        final F2Element y = fp2Mul1271(p.y, zInv);
        return new FieldPoint(x, y);
    }

    public static FieldPoint eccMulDouble(
            BigInteger k,
            FieldPoint q, BigInteger l
    ) throws EncryptionException {
        // Step 1: Compute l*Q
        FieldPoint lQ = eccMul(q, l);

        // Step 2-3: Convert l*Q to precomputed format
        ExtendedPoint extLQ = pointSetup(lQ);
        PreComputedExtendedPoint preCompLQ = r1ToR2(extLQ);

        // Step 4: Compute k*G (generator multiplication)
        FieldPoint kG = eccMulFixed(k);

        // Step 5-6: Add k*G + l*Q
        ExtendedPoint extKG = pointSetup(kG);
        ExtendedPoint result = eccAdd(preCompLQ, extKG);

        // Step 7: Normalize to affine coordinates
        return eccNorm(result);
    }

    private static ExtendedPoint eccAddCore(
            PreComputedExtendedPoint p,
            PreComputedExtendedPoint q
    ) {
        F2Element z = fp2Mul1271(p.t, q.t);
        F2Element t1 = fp2Mul1271(p.z, q.z);
        F2Element x = fp2Mul1271(p.xy, q.xy);
        F2Element y = fp2Mul1271(p.yx, q.yx);
        F2Element t2 = fp2Sub1271(t1, z);
        t1 = fp2Add1271(t1, z);
        F2Element tb = fp2Sub1271(x, y);
        F2Element ta = fp2Add1271(x, y);
        return new ExtendedPoint(
                fp2Mul1271(tb, t2),
                fp2Mul1271(ta, t1),
                fp2Mul1271(t1, t2),
                ta,
                tb
        );
    }

    private static ExtendedPoint eccAdd(
            PreComputedExtendedPoint q,
            ExtendedPoint p
    ) {
        return eccAddCore(q, r1ToR3(p));
    }

    @NotNull
    private static FieldPoint eccMul(
            FieldPoint p,
            BigInteger k
    ) throws EncryptionException {
        throw new EncryptionException("");
    }

    public static ExtendedPoint pointSetup(FieldPoint point) {
        return new ExtendedPoint(
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
    public static boolean eccPointValidate(@NotNull ExtendedPoint p) {
        F2Element t1 = fp2Sqr1271(p.y);                                 // y^2
        F2Element t2 = fp2Sqr1271(p.x);                                 // x^2
        F2Element t3 = fp2Sub1271(t1, t2);                              // y^2 - x^2 = -x^2 + y^2

        t1 = fp2Mul1271(t1, t2);                                        // x^2*y^2
        t2 = fp2Mul1271(convertToF2Element(Params.PARAMETER_D), t1);    // dx^2*y^2

        // Create F2Element representing 1 + 0i
        F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);
        t2 = fp2Add1271(t2, one);                                       // 1 + dx^2*y^2
        t1 = fp2Sub1271(t3, t2);                                        // -x^2 + y^2 - 1 - dx^2*y^2

        // Reduce modulo (2^127-1)
        t1 = new F2Element(
                FP.PUtil.mod1271(t1.real),
                FP.PUtil.mod1271(t1.im)
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
            FieldPoint P,
            BigInteger K,
            AffinePoint Q,
            boolean clearCofactor // Equivalent to the C Flag
    ) throws EncryptionException {
        // Convert to representation (X, Y, 1, Ta, Tb)
        ExtendedPoint R = pointSetup(P);

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
        PreComputedExtendedPoint[] table = eccPrecomp(R);

        // Extract initial point in (X+Y,Y-X,2Z,2dT) representation
        try {
            PreComputedExtendedPoint S = Table3.tableLookup1x8(table, digits[64], signMasks[64]);
            // Convert to representation (2X,2Y,2Z) for doubling operations
            R = r2ToR4(S, R);

            // Main computation loop: double-and-add with precomputed table
            for (int i = 63; i >= 0; i--) {
                // Extract point S in (X+Y,Y-X,2Z,2dT) representation
                S = Table3.tableLookup1x8(table, digits[i], signMasks[i]);

                // Double: R = 2*R using (X,Y,Z,Ta,Tb) <- 2*(X,Y,Z)
                R = eccDouble(R);

                // Add: R = R+S using (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (X+Y,Y-X,2Z,2dT)
                R = eccAdd(S, R);
            }
        } catch (exceptions.TableLookupException e) {
            throw new EncryptionException("Table lookup exception thrown.\n");
        }

        // Convert to affine coordinates (x,y) and store in output parameter Q
        FieldPoint result = eccNorm(R);
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
    private static PreComputedExtendedPoint[] eccPrecomp(@NotNull ExtendedPoint p) {
        // Initialize the output table
        @SuppressWarnings("unchecked")
        PreComputedExtendedPoint[] t
                = new PreComputedExtendedPoint[Params.NPOINTS_VARBASE.intValueExact()];

        PreComputedExtendedPoint p2;
        ExtendedPoint q;

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

        return null;
    }

    /**
     * Copy extended projective point source to destination
     * Equivalent to C macro: ecccopy(source, dest)
     * @param source the point to copy from Q = (X:Y:Z:Ta:Tb)
     * @return dest the point to copy to P = (X:Y:Z:Ta:Tb)
     */
    public static ExtendedPoint eccCopy(
            ExtendedPoint source
    ) {
        return new ExtendedPoint<>(
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
    private static ExtendedPoint cofactorClearing(ExtendedPoint p) {
        PreComputedExtendedPoint q = r1ToR2(p);  // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,2Z,2dT)

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
