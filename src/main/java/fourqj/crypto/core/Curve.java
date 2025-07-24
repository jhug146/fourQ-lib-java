package fourqj.crypto.core;

import java.math.BigInteger;

import fourqj.types.data.F2Element;
import fourqj.types.point.ExtendedPoint;
import fourqj.types.point.FieldPoint;
import fourqj.types.point.PreComputedExtendedPoint;

import static fourqj.constants.Params.T_VARBASE;
import static fourqj.constants.Params.W_VARBASE;

/**
 * Advanced curve operations and scalar decomposition for FourQ.
 * <p>
 * This class implements sophisticated algorithms for efficient scalar
 * multiplication including:\n * - 4-dimensional GLV scalar decomposition
 * - Fixed-window recoding for variable-base multiplication
 * - mLSB-set recoding for fixed-base multiplication
 * - Cofactor clearing operations
 * <p>
 * The GLV decomposition breaks down large scalars into smaller components
 * that can be processed in parallel, significantly accelerating elliptic
 * curve operations.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0.0
 */
public class Curve {
    /**
     * Recodes a scalar using the fixed-window method for variable-base scalar multiplication.
     * <p>
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
     * <p>
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

    /**
     * Co-factor clearing operation for elliptic curve points.
     *
     * @param p the input point P = (X₁,Y₁,Z₁,Ta,Tb) in extended twisted Edwards coordinates,
     *          where T₁ = Ta×Tb corresponds to (X₁:Y₁:Z₁:T₁)
     */
    static ExtendedPoint cofactorClearing(ExtendedPoint p) {
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
