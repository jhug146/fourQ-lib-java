package crypto.core;

import constants.Params;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;
import types.point.ExtendedPoint;
import types.point.FieldPoint;
import types.point.PreComputedExtendedPoint;

import java.math.BigInteger;

import static constants.Params.T_VARBASE;
import static constants.Params.W_VARBASE;

/**
 * Advanced curve operations and scalar decomposition for FourQ.
 * 
 * This class implements sophisticated algorithms for efficient scalar
 * multiplication including:\n * - 4-dimensional GLV scalar decomposition
 * - Fixed-window recoding for variable-base multiplication
 * - mLSB-set recoding for fixed-base multiplication
 * - Cofactor clearing operations
 * 
 * The GLV decomposition breaks down large scalars into smaller components
 * that can be processed in parallel, significantly accelerating elliptic
 * curve operations.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class Curve {
    /**
     * Recodes a scalar using the fixed-window method for variable-base scalar multiplication.
     * 
     * This method transforms a scalar into a sequence of signed digits that allows
     * for efficient computation using precomputed odd multiples. The technique
     * reduces the number of point additions required during scalar multiplication.
     * 
     * @param scalar the scalar to recode
     * @param signMasks output array for sign information
     * @return array of recoded digits
     */
    static int[] fixedWindowRecode(BigInteger scalar, int[] signMasks) {
        int[] digits = new int[T_VARBASE + 1];
        BigInteger val1 = BigInteger.ONE.shiftLeft(W_VARBASE.intValue()).subtract(BigInteger.ONE);
        BigInteger val2 = BigInteger.ONE.shiftLeft(W_VARBASE.intValue() - 1);

        BigInteger currentScalar = scalar;
        int windowSize = W_VARBASE.intValueExact() - 1;

        for (int i = 0; i < T_VARBASE; i++) {
            BigInteger temp = currentScalar.and(val1).subtract(val2);
            computeDigit(i, digits, signMasks, temp);
            currentScalar = currentScalar.subtract(temp).shiftRight(windowSize);
        }

        // Final digit computation
        computeDigit(T_VARBASE, digits, signMasks, currentScalar);
        return digits;
    }

    static void computeDigit(int pos, int[] digits, int[] signMasks, BigInteger temp) {
        boolean isNegative = temp.signum() < 0;
        signMasks[pos] = isNegative ? 0x00000000 : 0xFFFFFFFF;
        int tempInt = temp.intValue();
        int negTempInt = -tempInt;
        int tempXorNeg = tempInt ^ negTempInt;
        digits[pos] = ((signMasks[pos] & tempXorNeg) ^ negTempInt) >>> 1;
    }

    /**
     * Converts an affine point to extended projective coordinates.
     * 
     * Extended coordinates (X:Y:Z:T) where T = X*Y/Z provide faster
     * addition formulas for twisted Edwards curves. This method initializes
     * Z = 1 and sets up the auxiliary coordinates.
     * 
     * @param point the affine point (x,y) to convert
     * @return the point in extended projective coordinates
     */
    public static ExtendedPoint pointSetup(FieldPoint point) {
        return new ExtendedPoint(
                point.getX(),
                point.getY(),
                new F2Element(BigInteger.ONE, BigInteger.ZERO),
                point.getX(),
                point.getY()
        );
    }

    public static int @NotNull [] mLSBSetRecode(
            BigInteger inputScalar,
            int @NotNull [] digits
    ) {
        final int d = Params.D_FIXEDBASE;                              // ceil(bitlength(order)/(w*v))*v

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
        for (int i = d; i < Params.L_FIXEDBASE; i++) {
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
     * Decomposes a scalar using 4-dimensional GLV for parallel computation.
     * 
     * The Gallant-Lambert-Vanstone (GLV) method decomposes a large scalar k
     * into four smaller scalars k1, k2, k3, k4 such that:
     * k*P = k1*P + k2*φ(P) + k3*ψ(P) + k4*φ(ψ(P))
     * 
     * This allows parallel computation of four smaller scalar multiplications,
     * significantly reducing the total computation time.
     * 
     * @param k the scalar to decompose (range [0, 2^256-1])
     * @return array of 4 sub-scalars for efficient multiplication
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
        PreComputedExtendedPoint q = Conversion.r1ToR2(p);  // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,2Z,2dT)
        p = ECC.eccDouble(p);                                   // P = 2*P using representations (X,Y,Z,Ta,Tb) <- 2*(X,Y,Z)
        p = ECC.eccAdd(q, p);                                   // P = P+Q using representations (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (X+Y,Y-X,2Z,2dT)
        p = ECC.eccDouble(p);
        p = ECC.eccDouble(p);
        p = ECC.eccDouble(p);
        p = ECC.eccDouble(p);
        p = ECC.eccAdd(q, p);
        p = ECC.eccDouble(p);
        p = ECC.eccDouble(p);
        return ECC.eccDouble(p);
    }
}
