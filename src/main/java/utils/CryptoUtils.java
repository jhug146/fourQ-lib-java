package utils;

import constants.Params;
import crypto.core.Curve;
import crypto.core.ECC;
import exceptions.EncryptionException;
import exceptions.ValidationException;
import field.operations.FP;
import field.operations.FP2;
import types.point.ExtendedPoint;
import types.data.F2Element;
import types.point.FieldPoint;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Cryptographic utility functions for FourQ operations.
 * <p>
 * This class provides essential cryptographic utilities including:
 * - Secure random number generation
 * - Montgomery form conversions for efficient modular arithmetic
 * - Point encoding/decoding between curve points and byte representations
 * - Field arithmetic optimizations
 * <p>
 * The encoding/decoding functions handle the compression and decompression
 * of elliptic curve points for efficient storage and transmission.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class CryptoUtils {
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates cryptographically secure random bytes.
     * <p>
     * Uses the system's SecureRandom implementation to generate high-quality
     * random bytes suitable for cryptographic operations like key generation.
     * 
     * @param size the number of random bytes to generate
     * @return a BigInteger containing the random bytes
     */
    public static BigInteger randomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return new BigInteger(bytes);
    }

    /**
     * Converts a value to Montgomery form for efficient modular arithmetic.
     * <p>
     * Montgomery form allows faster modular multiplications by avoiding
     * expensive division operations. This is particularly useful for
     * repeated modular operations in signature schemes.
     * 
     * @param key the value to convert to Montgomery form
     * @return the value in Montgomery form
     */
    public static BigInteger toMontgomery(BigInteger key) {
        return FP.montgomeryMultiplyModOrder(key, Params.MONTGOMERY_R_PRIME);
    }

    /**
     * Converts a value from Montgomery form back to normal representation.
     * <p>
     * This operation is the inverse of toMontgomery() and is used to
     * convert results back to standard form after Montgomery arithmetic.
     * 
     * @param key the value in Montgomery form to convert back
     * @return the value in normal form
     */
    public static BigInteger fromMontgomery(BigInteger key) {
        return FP.montgomeryMultiplyModOrder(key, BigInteger.ONE);
    }

    /**
     * Encodes an elliptic curve point into compressed 32-byte representation.
     * <p>
     * The encoding stores the y-coordinate explicitly and encodes the x-coordinate's
     * sign information in a single bit. This compression reduces storage requirements
     * while maintaining all information needed to recover the full point.
     * 
     * @param P the curve point to encode
     * @return the compressed point as a 32-byte BigInteger
     */
    public static BigInteger encode(FieldPoint P) {
        byte temp1 = (byte) (P.getX().im.testBit(126) ? 0x80 : 0x00);
        byte temp2 = (byte) (P.getX().real.testBit(126) ? 0x80 : 0x00);

        byte[] realPart = P.getY().real.toByteArray();
        byte[] imPart = P.getY().im.toByteArray();
        byte[] result = new byte[32];

        // Copy real bytes in reverse order to positions 0-15
        int realLen = Math.min(realPart.length, 16);
        for (int i = 0; i < realLen; i++) {
            result[i] = realPart[realLen - 1 - i];
        }

        // Copy im bytes in reverse order to positions 16-31
        int imLen = Math.min(imPart.length, 16);
        for (int i = 0; i < imLen; i++) {
            result[16 + i] = imPart[imLen - 1 - i];
        }

        if (P.getX().isZero()) {
            result[31] |= temp1;
        } else {
            result[31] |= temp2;
        }

        return new BigInteger(1, result);
    }

    /**
     * Decodes a compressed point representation back to a full curve point.
     * <p>
     * This method reverses the encoding process by:
     * 1. Extracting the y-coordinate from the encoded data
     * 2. Computing the x-coordinate using the curve equation
     * 3. Determining the correct sign using the encoded bit
     * 4. Validating the resulting point lies on the curve
     * 
     * @param encoded the compressed 32-byte point representation
     * @return the decoded curve point
     * @throws EncryptionException if decoding fails or point is invalid
     */
    public static FieldPoint decode(BigInteger encoded) throws EncryptionException {
        F2Element y = BigIntegerUtils.convertBigIntegerToF2Element(encoded);
        int signBit = encoded.testBit(7) ? 1 : 0;
        y.im = y.im.clearBit(127);

        F2Element u = FP2.fp2Sqr1271(y);
        F2Element v = FP2.fp2Mul1271(u, Params.PARAMETER_d);
        u = FP2.fp2Sub1271(u, F2Element.ONE);
        v = FP2.fp2Add1271(v, F2Element.ONE);

        BigInteger t0 = FP.PUtil.fpSqr1271(v.real);
        BigInteger t1 = FP.PUtil.fpSqr1271(v.im);         // t1 = v1^2
        t0 = FP.PUtil.fpAdd1271(t0, t1);                  // t0 = t0+t1
        t1 = FP.PUtil.fpMul1271(u.real, v.real);          // t1 = u0*v0
        BigInteger t2 = FP.PUtil.fpMul1271(u.im, v.im);   // t2 = u1*v1
        t1 = FP.PUtil.fpAdd1271(t1, t2);                  // t1 = t1+t2
        t2 = FP.PUtil.fpMul1271(u.im, v.real);            // t2 = u1*v0
        BigInteger t3 = FP.PUtil.fpMul1271(u.real, v.im); // t3 = u0*v1
        t2 = FP.PUtil.fpSub1271(t2, t3);                  // t2 = t2-t3
        t3 = FP.PUtil.fpSqr1271(t1);                      // t3 = t1^2
        BigInteger t4 = FP.PUtil.fpSqr1271(t2);           // t4 = t2^2
        t3 = FP.PUtil.fpAdd1271(t3, t4);                  // t3 = t3+t4
        for (int i = 0; i < 125; i++) {                       // t3 = t3^(2^125)
            t3 = FP.PUtil.fpSqr1271(t3);
        }

        BigInteger t = FP.PUtil.fpAdd1271(t1, t3);      // t = t1+t3
        if (t.equals(BigInteger.ZERO)) {
            t = FP.PUtil.fpSub1271(t1, t3);             // t = t1-t3
        }
        t = FP.PUtil.fpAdd1271(t, t);                   // t = 2*t
        t3 = FP.PUtil.fpSqr1271(t0);                    // t3 = t0^2
        t3 = FP.PUtil.fpMul1271(t3, t0);                // t3 = t3*t0
        t3 = FP.PUtil.fpMul1271(t, t3);                 // t3 = t3*t
        BigInteger r = FP.PUtil.fpExp1251(t3);          // r = t3^(2^125-1)
        t3 = FP.PUtil.fpMul1271(t0, r);                 // t3 = t0*r
        BigInteger x0 = FP.PUtil.fpMul1271(t, t3);      // x0 = t*t3
        t1 = FP.PUtil.fpSqr1271(x0);
        t1 = FP.PUtil.fpMul1271(t0, t1);                // t1 = t0*x0^2
        x0 = FP.PUtil.fpDiv1271(x0);                    // x0 = x0/2
        BigInteger x1 = FP.PUtil.fpMul1271(t2, t3);     // x1 = t3*t2

        if (!t.equals(t1)) {        // If t != t1 then swap x0 and x1
            t0 = x0;
            x0 = x1;
            x1 = t0;
        }
        F2Element x = new F2Element(x0, x1);


        int signDec;
        if (x.isZero()) {
            // Entire x coordinate is zero, extract sign from imaginary part
            signDec = x.im.shiftRight(126).intValue() & 0x3;  // Extract top 2 bits for 127-bit field
        } else {
            // x coordinate is non-zero, extract sign from real part
            signDec = x.real.shiftRight(126).intValue() & 0x3;  // Extract top 2 bits for 127-bit field
        }

        if (signBit != signDec) {           // If sign of x-coordinate decoded != input sign bit, then negate x-coordinate
            x = FP2.fp2Neg1271(x);
        }

        FieldPoint point = new FieldPoint(x, y);
        ExtendedPoint testPoint = Curve.pointSetup(point);
        if (!ECC.eccPointValidate(testPoint)) {
            testPoint.getX().im = FP.PUtil.fpNeg1271(testPoint.getX().im);
            point.getX().im = testPoint.getX().im;
            if (!ECC.eccPointValidate(testPoint)) {       // Final point validation
                throw new ValidationException("Error validating point in decode.");
            }
        }

        return point;
    }

}