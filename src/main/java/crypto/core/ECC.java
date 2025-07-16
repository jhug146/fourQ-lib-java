package crypto.core;

import java.math.BigInteger;

import org.jetbrains.annotations.NotNull;

import constants.Params;
import crypto.primitives.Table;
import exceptions.EncryptionException;
import field.operations.FP;
import types.data.F2Element;
import types.point.ExtendedPoint;
import types.point.FieldPoint;
import types.point.PreComputedExtendedPoint;

import static constants.Params.T_VARBASE;
import static field.operations.FP2.*;

/**
 * Core elliptic curve cryptography operations for the FourQ curve.
 * <p>
 * This class implements the fundamental ECC operations including:
 * - Point arithmetic (addition, doubling, multiplication)
 * - Fixed-base and variable-base scalar multiplication
 * - Point validation and normalization
 * - Precomputation table generation
 * <p>
 * FourQ uses extended twisted Edwards coordinates for efficient point arithmetic
 * and employs advanced techniques like 4-dimensional GLV decomposition and
 * precomputed tables for high-performance scalar multiplications.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class ECC {
    /**
     * Returns the generator point of the FourQ elliptic curve.
     * <p>
     * The generator point is a fixed point of prime order that generates
     * the cryptographic subgroup used for all scalar multiplications.
     * 
     * @return the generator point G in affine coordinates (x,y)
     */
    @NotNull
    public static FieldPoint getGeneratorPoint() {
        return new FieldPoint(Params.GENERATOR_X, Params.GENERATOR_Y);
    }

    /**
     * Performs fixed-base scalar multiplication k*G where G is the generator.
     * <p>
     * This method is optimized for multiplication with the fixed generator point
     * using precomputed tables and advanced windowing techniques for maximum
     * performance in key generation and signing operations.
     * 
     * @param val the scalar multiplier k
     * @return the point k*G in affine coordinates
     * @throws EncryptionException if the scalar multiplication fails
     */
    @NotNull
    public static FieldPoint eccMulFixed(@NotNull BigInteger val) throws EncryptionException {
        return ECC.eccMul(ECC.getGeneratorPoint(), val,false);
    }

    /**
     * Performs variable-base scalar multiplication k*P for arbitrary point P.
     * <p>
     * This method implements the sliding window method with precomputed odd multiples
     * for efficient scalar multiplication with arbitrary base points. Optionally
     * performs cofactor clearing for points that may not be in the prime subgroup.
     * 
     * @param p the base point P to multiply
     * @param k the scalar multiplier k
     * @param clearCofactor whether to clear the cofactor (multiply by cofactor)
     * @return the point k*P in affine coordinates
     * @throws EncryptionException if point validation fails or multiplication errors occur
     */
    @NotNull
    public static FieldPoint eccMul(
            @NotNull FieldPoint p,
            @NotNull BigInteger k,
            boolean clearCofactor
    ) throws EncryptionException {
        PreComputedExtendedPoint s;
        int[] signMasks = new int[T_VARBASE + 1];

        ExtendedPoint r = Curve.pointSetup(p);

        if (!eccPointValidate(r)) {
            throw new EncryptionException("Point validation failed within eccMul");
        }
        if (clearCofactor) {
            r = Curve.cofactorClearing(r);
        }

        BigInteger kOdd = FP.moduloOrder(k);
        kOdd = FP.conversionToOdd(kOdd);
        PreComputedExtendedPoint[] table = eccPrecomp(r);
        int[] digits = Curve.fixedWindowRecode(kOdd, signMasks);

        s = Table.tableLookup(table, digits[T_VARBASE], signMasks[T_VARBASE]);
        r = Conversion.r2ToR4(s, r);

        for (int i = T_VARBASE - 1; i >= 0; i--) {
            r = eccDouble(r);
            s = Table.tableLookup(table, digits[i], signMasks[i]);
            r = eccDouble(r);
            r = eccDouble(r);
            r = eccDouble(r);
            r = eccAdd(s, r);
        }

        return eccNorm(r);
    }

    /**
     * Point doubling operation: computes 2P for point P.
     * <p>
     * Uses the fastest known doubling formulas for twisted Edwards curves.
     * Input point P = (X₁:Y₁:Z₁:Ta:Tb) where T₁ = Ta×Tb corresponds to
     * (X₁:Y₁:Z₁:T₁) in extended twisted Edwards coordinates.
     * 
     * @param p the input point P in extended coordinates
     * @return the point 2P in extended coordinates
     */
    @NotNull
    public static ExtendedPoint eccDouble(@NotNull ExtendedPoint p) {
        F2Element t1 = fp2Sqr1271(p.getX());                 // t1 = X1^2
        F2Element t2 = fp2Sqr1271(p.getY());                 // t2 = Y1^2
        F2Element t3 = fp2Add1271(p.getX(), p.getY());       // t3 = X1+Y1
        F2Element tb = fp2Add1271(t1, t2);                   // TB_final = X1^2+Y1^2
        t1 = fp2Sub1271(t2, t1);                             // t1 = Y1^2-X1^2
        F2Element ta = fp2Sqr1271(t3);                       // Ta = (X1+Y1)^2
        t2 = fp2Sqr1271(p.getZ());                           // t2 = Z1^2
        ta = fp2Sub1271(ta, tb);                             // TA_final = 2X1*Y1 = (X1+Y1)^2-(X1^2+Y1^2)
        t2 = fp2AddSub1271(t2, t1);                          // t2 = 2Z1^2-(Y1^2-X1^2)
        final F2Element y = fp2Mul1271(t1, tb);              // Y_final = (X1^2+Y1^2)(Y1^2-X1^2)
        final F2Element x = fp2Mul1271(t2, ta);              // X_final = 2X1*Y1*[2Z1^2-(Y1^2-X1^2)]
        final F2Element z = fp2Mul1271(t1, t2);              // Z_final = (Y1^2-X1^2)[2Z1^2-(Y1^2-X1^2)]
        return new ExtendedPoint(x, y, z, ta, tb);
    }

    /**
     * Normalizes a point from extended projective coordinates to affine coordinates.
     * <p>
     * Converts a point (X:Y:Z:T) in extended twisted Edwards coordinates to
     * affine coordinates (x,y) by computing x = X/Z and y = Y/Z. The resulting
     * coordinates are fully reduced modulo the field prime.
     * 
     * @param p the point in extended projective coordinates
     * @return the same point in affine coordinates (x,y)
     */
    @NotNull
    public static FieldPoint eccNorm(@NotNull ExtendedPoint p) {
        final F2Element zInv = fp2Inv1271(p.getZ());
        final F2Element x = fp2Mul1271(p.getX(), zInv);
        final F2Element y = fp2Mul1271(p.getY(), zInv);

        x.im = FP.PUtil.fpMod1271(x.im);
        x.real = FP.PUtil.fpMod1271(x.real);

        y.im = FP.PUtil.fpMod1271(y.im);
        y.real = FP.PUtil.fpMod1271(y.real);
        return new FieldPoint(x, y);
    }

    /**
     * Computes double scalar multiplication k*G + l*Q efficiently.
     * <p>
     * This method is optimized for signature verification where we need to compute
     * a linear combination of the generator G and another point Q. It's more
     * efficient than computing k*G and l*Q separately and then adding them.
     * 
     * @param k scalar multiplier for the generator point G
     * @param q the second base point Q
     * @param l scalar multiplier for point Q
     * @return the point k*G + l*Q in affine coordinates
     * @throws EncryptionException if any scalar multiplication fails
     */
    @NotNull
    public static FieldPoint eccMulDouble(
            @NotNull BigInteger k,
            @NotNull FieldPoint q,
            @NotNull BigInteger l
    ) throws EncryptionException {
        // Step 1: Compute l*Q
        FieldPoint lQ = eccMul(q, l, false);

        // Step 2-3: Convert l*Q to precomputed format
        ExtendedPoint extLQ = Curve.pointSetup(lQ);
        PreComputedExtendedPoint preCompLQ = Conversion.r1ToR2(extLQ);

        // Step 4: Compute k*G (generator multiplication)
        FieldPoint kG = eccMulFixed(k);

        // Step 5-6: Add k*G + l*Q
        ExtendedPoint extKG = Curve.pointSetup(kG);
        ExtendedPoint result = eccAdd(preCompLQ, extKG);

        // Step 7: Normalize to affine coordinates
        return eccNorm(result);
    }

    @NotNull
    private static ExtendedPoint eccAddCore(
            @NotNull PreComputedExtendedPoint p,
            @NotNull PreComputedExtendedPoint q
    ) {
        F2Element z = fp2Mul1271(p.getT(), q.getT());
        F2Element t1 = fp2Mul1271(p.getZ(), q.getZ());
        F2Element x = fp2Mul1271(p.getX(), q.getX());
        F2Element y = fp2Mul1271(p.getY(), q.getY());
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

    @NotNull
    static ExtendedPoint eccAdd(
            @NotNull PreComputedExtendedPoint q,
            @NotNull ExtendedPoint p
    ) {
        return eccAddCore(q, Conversion.r1ToR3(p));
    }

    /**
     * Point validation: check if point lies on the curve
     * @param p = (x,y) in affine coordinates, where x, y in [0, 2^127-1]
     * @return true if point lies on curve E: -x^2+y^2-1-dx^2*y^2 = 0, false otherwise
     *
     * @implNote this function does not run in constant time (input point P is assumed to be public)
     */
    public static boolean eccPointValidate(@NotNull ExtendedPoint p) {
        F2Element t1 = fp2Sqr1271(p.getY());                            // y^2
        F2Element t2 = fp2Sqr1271(p.getX());                            // x^2
        F2Element t3 = fp2Sub1271(t1, t2);                              // y^2 - x^2 = -x^2 + y^2

        t1 = fp2Mul1271(t1, t2);                                        // x^2*y^2
        t2 = fp2Mul1271(Params.PARAMETER_D, t1);                        // dx^2*y^2

        // Create F2Element representing 1 + 0i
        F2Element one = new F2Element(
                BigInteger.ONE,
                BigInteger.ZERO
        );

        t2 = fp2Add1271(t2, one);                                       // 1 + dx^2*y^2
        t1 = fp2Sub1271(t3, t2);                                        // -x^2 + y^2 - 1 - dx^2*y^2

        // Reduce modulo (2^127-1)
        t1 = new F2Element(
                FP.PUtil.fpMod1271(t1.real),
                FP.PUtil.fpMod1271(t1.im)
        );

        // Check if the result is zero (both real and imaginary parts must be zero) to be on the curve.
        return t1.real.equals(BigInteger.ZERO) && t1.im.equals(BigInteger.ZERO);
    }

    /**
     * Generation of the precomputation table used by the variable-base scalar multiplication eccMul().
     * @param p = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates.
     * @return table T containing N_POINTS_VARBASE points: P, 3P, 5P, ... , (2 * N_POINTS_VARBASE - 1)P. N_POINTS_VARBASE is fixed to 8 (see FourQ.h).
     *         Precomputed points use the representation (X+Y,Y-X,2Z,2dT) corresponding to (X:Y:Z:T) in extended twisted Edwards coordinates.
     */
    @NotNull
    public static PreComputedExtendedPoint[] eccPrecomp(@NotNull ExtendedPoint p) {
        // Initialize the output table
        PreComputedExtendedPoint[] t
                = new PreComputedExtendedPoint[Params.N_POINTS_VARBASE.intValueExact()];

        PreComputedExtendedPoint p2;
        ExtendedPoint q;

        // Generating P2 = 2(X1,Y1,Z1,T1a,T1b) and T[0] = P
        q = p.dup();
        t[0] = Conversion.r1ToR2(p);                    // T[0] = P in (X+Y,Y-X,2Z,2dT) format
        q = eccDouble(q);                               // Q = 2P
        p2 = Conversion.r1ToR3(q);                      // P2 = 2P in R3 format

        // Generate odd multiples: 3P, 5P, 7P, ..., (2 * N_POINTS_VARBASE - 1)P
        for (int i = 1; i < Params.N_POINTS_VARBASE.intValueExact(); i++) {
            // T[i] = 2P + T[i-1] = (2*i+1)P
            q = eccAddCore(p2, t[i-1]);                 // Add 2P to previous odd multiple
            t[i] = Conversion.r1ToR2(q);                // Convert result to R2 format
        }

        return t;
    }
}
