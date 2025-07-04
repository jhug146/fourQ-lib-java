package crypto;

import constants.Params;
import exceptions.EncryptionException;
import operations.FP;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;
import types.point.AffinePoint;
import types.point.ExtendedPoint;
import types.point.FieldPoint;
import types.point.PreComputedExtendedPoint;

import java.math.BigInteger;

import static constants.Params.*;
import static constants.Params.T_VARBASE;
import static operations.FP2.*;


public class ECCUtil {
    private static final int W_FIXEDBASE = 5;
    private static final int V_FIXEDBASE = 5;
    private static final int D_FIXEDBASE = 54;
    private static final int E_FIXEDBASE = 10;
    private static final int L_FIXEDBASE = D_FIXEDBASE * W_FIXEDBASE;

    private static final F2Element F2_ONE = new F2Element(BigInteger.ONE, BigInteger.ONE);

    // Set generator
    // Output: P = (x,y)
    public static void eccSet(AffinePoint P) {
        P.setX(Params.GENERATOR_x);    // X1
        P.setY(Params.GENERATOR_y);    // Y1
        P.setT(null);                  // TODO may be wrong
    }

    @NotNull
    public static FieldPoint eccMulFixed(BigInteger val) throws EncryptionException {
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
        Table.tableLookup(
                (V_FIXEDBASE - 1) * (1 << (W_FIXEDBASE - 1)),
                digit,
                digits[D_FIXEDBASE - 1],
                affPoint
        );
        ExtendedPoint exPoint = r5ToR1(affPoint);

        for (int j = 0; j < V_FIXEDBASE - 1; j++) {
            digit = digits[W_FIXEDBASE * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1];
            final int iStart = (W_FIXEDBASE - 1) * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            final int iMin = 2 * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            for (int i = iStart; i >= iMin; i -= D_FIXEDBASE) {
                digit = 2 * digit + digits[i];
            }
            // Extract point in (x+y,y-x,2dt) representation
            final int signDigit = D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            final int tableStart = (V_FIXEDBASE - j - 2) * (1 << (W_FIXEDBASE - 1));
            affPoint = Table
                    .tableLookup(tableStart, digit, digits[signDigit], affPoint)
                    .toAffinePoint();
            exPoint = eccMixedAdd(affPoint, exPoint);
        }

        for (int i = E_FIXEDBASE - 2; i >= 0; i--) {
            exPoint = eccDouble(exPoint);
            for (int j = 0; j < V_FIXEDBASE; j++) {
                digit = digits[W_FIXEDBASE * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE];
                final int kStart = (W_FIXEDBASE - 1) * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                final int kMin = 2 * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                for (int k = kStart; k >= kMin; k -= D_FIXEDBASE) {
                    digit = 2 * digit + digits[k];
                }
                final int signDigit = D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                final int tableStart = (V_FIXEDBASE - j - 1) * (1 << (W_FIXEDBASE - 1));
                affPoint = Table
                        .tableLookup(tableStart, digit, signDigit, affPoint)
                        .toAffinePoint();
                exPoint = eccMixedAdd(affPoint, exPoint);
            }
        }
        return eccNorm(exPoint);
    }

    public static FieldPoint eccMul(
            FieldPoint p,
            BigInteger k,
            boolean clearCofactor
    ) throws EncryptionException {
        PreComputedExtendedPoint s;
        int[] signMasks = new int[T_VARBASE + 1];

        ExtendedPoint r = pointSetup(p);

        if (!ECCUtil.eccPointValidate(r)) {
            throw new EncryptionException("Point validation failed within eccMul");
        }
        if (clearCofactor) {
            r = ECCUtil.cofactorClearing(r);
        }

        BigInteger kOdd = FP.moduloOrder(k);
        kOdd = FP.conversionToOdd(kOdd);
        PreComputedExtendedPoint[] table = ECCUtil.eccPrecomp(r);
        int[] digits = fixedWindowRecode(kOdd, signMasks);

        s = Table.tableLookup(table, digits[T_VARBASE], signMasks[T_VARBASE]);
        r = ECCUtil.r2ToR4(s, r);

        for (int i = (T_VARBASE -1); i >= 0; i--) {
            r = eccDouble(r);
            s = Table.tableLookup(table, digits[i], signMasks[i]);
            r = eccDouble(r);
            r = eccDouble(r);
            r = eccDouble(r);
            r = eccAdd(s, r);
        }

        return eccNorm(r);
    }


    public static int[] fixedWindowRecode(BigInteger scalar, int[] signMasks) {
        int[] digits = new int[T_VARBASE + 1];
        BigInteger val1 = BigInteger.ONE.shiftLeft(W_VARBASE.intValue()).subtract(BigInteger.ONE);
        BigInteger val2 = BigInteger.ONE.shiftLeft(W_VARBASE.intValue() - 1);

        BigInteger currentScalar = scalar;
        int windowSize = W_VARBASE.intValue() - 1;

        for (int i = 0; i < T_VARBASE; i++) {
            BigInteger temp = currentScalar.and(val1).subtract(val2);
            computeDigit(i, digits, signMasks, temp);
            currentScalar = currentScalar.subtract(temp).shiftRight(windowSize);
        }

        // Final digit computation
        computeDigit(T_VARBASE, digits, signMasks, currentScalar);
        return digits;
    }

    private static void computeDigit(int pos, int[] digits, int[] signMasks, BigInteger scalar) {
        boolean isNegative = scalar.signum() < 0;
        signMasks[pos] = isNegative ? 0x00000000 : 0xFFFFFFFF;

        BigInteger tempXorNeg = scalar.xor(scalar.negate());
        BigInteger signMaskBig = BigInteger.valueOf(signMasks[pos] & 0xFFFFFFFFL);
        BigInteger digitCalc = signMaskBig
                .and(tempXorNeg)
                .xor(scalar.negate())
                .shiftRight(1);
        digits[pos] = digitCalc.intValue();
    }

    private static ExtendedPoint r5ToR1(AffinePoint p) {
        F2Element x = fp2Div1271(fp2Sub1271(p.getX(), p.getY()));
        F2Element y = fp2Div1271(fp2Add1271(p.getX(), p.getY()));
        return new ExtendedPoint(x, y, F2_ONE, x, y);
    }

    private static PreComputedExtendedPoint r1ToR2(ExtendedPoint point) {
        F2Element t = fp2Sub1271(fp2Add1271(point.getTa(), point.getTb()), point.getTb());
        return new PreComputedExtendedPoint(
                fp2Add1271(point.getY(), point.getX()),
                fp2Sub1271(point.getY(), point.getX()),
                fp2Add1271(point.getZ(), point.getZ()),
                fp2Mul1271(t, Params.PARAMETER_d)
        );
    }

    private static PreComputedExtendedPoint r1ToR3(ExtendedPoint point) {
        return new PreComputedExtendedPoint(
                fp2Add1271(point.getX(), point.getY()),
                fp2Sub1271(point.getY(), point.getX()),
                fp2Mul1271(point.getTa(), point.getTb()),
                point.getZ()
        );
    }

    @NotNull
    public static ExtendedPoint r2ToR4(@NotNull PreComputedExtendedPoint p, @NotNull ExtendedPoint q) {
        return new ExtendedPoint(
                fp2Sub1271(p.xy, p.yx),
                fp2Add1271(p.xy, p.yx),
                fp2Copy1271(p.z),
                q.getTa(),
                q.getTb()
        );
    }

    private static ExtendedPoint eccMixedAdd(
            AffinePoint q,
            ExtendedPoint p
    ) {
        F2Element ta = fp2Mul1271(p.getTa(), p.getTb());          // Ta = T1
        F2Element t1 = fp2Add1271(p.getZ(), p.getZ());            // t1 = 2Z1
        ta = fp2Mul1271(ta, q.getT());                       // Ta = 2dT1*t2
        F2Element pz = fp2Add1271(p.getX(), p.getY());            // Z = (X1+Y1)
        F2Element tb = fp2Sub1271(p.getY(), p.getX());            // Tb = (Y1-X1)
        F2Element t2 = fp2Sub1271(t1, ta);              // t2 = theta
        t1 = fp2Add1271(t1, ta);                        // t1 = alpha
        ta = fp2Mul1271(q.getX(), pz);                       // Ta = (X1+Y1)(x2+y2)
        F2Element x = fp2Mul1271(q.getY(), tb);              // X = (Y1-X1)(y2-x2)
        tb = fp2Sub1271(ta, x);                         // Tbfinal = beta
        ta = fp2Add1271(ta, x);                         // Tafinal = omega
        return new ExtendedPoint(
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
    static ExtendedPoint eccDouble(ExtendedPoint p) {
        F2Element t1 = fp2Sqr1271(p.getX());                 // t1 = X1^2
        F2Element t2 = fp2Sqr1271(p.getY());                 // t2 = Y1^2
        F2Element t3 = fp2Add1271(p.getX(), p.getY());            // t3 = X1+Y1
        F2Element tb = fp2Add1271(t1, t2);              // Tbfinal = X1^2+Y1^2
        t1 = fp2Sub1271(t2, t1);                        // t1 = Y1^2-X1^2
        F2Element ta = fp2Sqr1271(t3);                  // Ta = (X1+Y1)^2
        t2 = fp2Sqr1271(p.getZ());                           // t2 = Z1^2
        ta = fp2Sub1271(ta, tb);                        // Tafinal = 2X1*Y1 = (X1+Y1)^2-(X1^2+Y1^2)
        t2 = fp2AddSub1271(t2, t1);                     // t2 = 2Z1^2-(Y1^2-X1^2)
        final F2Element y = fp2Mul1271(t1, tb);         // Yfinal = (X1^2+Y1^2)(Y1^2-X1^2)
        final F2Element x = fp2Mul1271(t2, ta);         // Xfinal = 2X1*Y1*[2Z1^2-(Y1^2-X1^2)]
        final F2Element z = fp2Mul1271(t1, t2);         // Zfinal = (Y1^2-X1^2)[2Z1^2-(Y1^2-X1^2)]
        return new ExtendedPoint(x, y, z, ta, tb);
    }

    static FieldPoint eccNorm(ExtendedPoint p) {
        final F2Element zInv = fp2Inv1271(p.getZ());
        final F2Element x = fp2Mul1271(p.getX(), zInv);
        final F2Element y = fp2Mul1271(p.getY(), zInv);
        return new FieldPoint(x, y);
    }

    @NotNull
    public static FieldPoint eccMulDouble(
            BigInteger k,
            FieldPoint q,
            BigInteger l
    ) throws EncryptionException {
        // Step 1: Compute l*Q
        FieldPoint lQ = eccMul(q, l, false);

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

    static ExtendedPoint eccAdd(
            PreComputedExtendedPoint q,
            ExtendedPoint p
    ) {
        return eccAddCore(q, r1ToR3(p));
    }

    public static ExtendedPoint pointSetup(FieldPoint point) {
        return new ExtendedPoint(
                point.getX(),
                point.getY(),
                new F2Element(BigInteger.ONE, BigInteger.ZERO),
                point.getX(),
                point.getY()
        );
    }

    /**
     * Computes the modified LSB-set representation of a scalar
     * @param inputScalar scalar in [0, order-1], where the order of FourQ's subgroup is 246 bits
     * @param digits output array where:
     *               - First d values (indices 0 to d-1) store signs: -1 (negative), 0 (positive)
     *               - Remaining values (indices d to l-1) store recoded values (excluding sign)
     */
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
        F2Element t1 = fp2Sqr1271(p.getY());                                 // y^2
        F2Element t2 = fp2Sqr1271(p.getX());                                 // x^2
        F2Element t3 = fp2Sub1271(t1, t2);                              // y^2 - x^2 = -x^2 + y^2

        t1 = fp2Mul1271(t1, t2);                                        // x^2*y^2
        t2 = fp2Mul1271(Params.PARAMETER_d, t1);    // dx^2*y^2

        // Create F2Element representing 1 + 0i
        F2Element one = new F2Element(
                BigInteger.ONE,
                BigInteger.ZERO
        );

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
     * Generation of the precomputation table used by the variable-base scalar multiplication eccMul().
     * @param p = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates.
     * @return table T containing NPOINTS_VARBASE points: P, 3P, 5P, ... , (2*NPOINTS_VARBASE-1)P. NPOINTS_VARBASE is fixed to 8 (see FourQ.h).
     *         Precomputed points use the representation (X+Y,Y-X,2Z,2dT) corresponding to (X:Y:Z:T) in extended twisted Edwards coordinates.
     */
    @NotNull
    public static PreComputedExtendedPoint[] eccPrecomp(@NotNull ExtendedPoint p) {
        // Initialize the output table
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
        return new ExtendedPoint(
                fp2Copy1271(source.getX()),
                fp2Copy1271(source.getY()),
                fp2Copy1271(source.getZ()),
                fp2Copy1271(source.getTa()),
                fp2Copy1271(source.getTb())
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
     * Co-factor clearing operation for elliptic curve points.
     *
     * @param p the input point P = (X₁,Y₁,Z₁,Ta,Tb) in extended twisted Edwards coordinates,
     *          where T₁ = Ta×Tb corresponds to (X₁:Y₁:Z₁:T₁)
     */
    public static ExtendedPoint cofactorClearing(ExtendedPoint p) {
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
        return eccDouble(p);
    }
}
