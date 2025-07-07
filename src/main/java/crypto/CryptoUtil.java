package crypto;

import constants.Key;
import constants.Params;
import crypto.core.Curve;
import crypto.core.ECC;
import exceptions.EncryptionException;
import field.operations.FP;
import field.operations.FP2;
import types.point.ExtendedPoint;
import types.data.F2Element;
import types.point.FieldPoint;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoUtil {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static byte[] bigIntegerToByte(
            BigInteger publicKey,
            int keySize,
            boolean removePadZeros
    ) {
        byte[] raw = publicKey.toByteArray();
        if (removePadZeros) { return raw; }
        if (raw.length == keySize) { return raw; }

        if (raw.length < keySize) {
            byte[] padded = new byte[keySize];
            System.arraycopy(raw, 0, padded, keySize - raw.length, raw.length);
            return padded;
        }

        return Arrays.copyOfRange(raw, raw.length - keySize, raw.length);
    }

    public static BigInteger randomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return new BigInteger(bytes);
    }

    public static BigInteger toMontgomery(BigInteger key) {
        return FP.montgomeryMultiplyModOrder(key, Params.MONTGOMERY_R_PRIME);
    }

    public static BigInteger fromMontgomery(BigInteger key) {
        return FP.montgomeryMultiplyModOrder(key, BigInteger.ONE);
    }

    public static BigInteger encode(FieldPoint point) {
        BigInteger y = point.getY().real.add(point.getY().im.shiftLeft(128));
        boolean ySignBit = point.getY().real.compareTo(BigInteger.ZERO) <= 0;
        if (ySignBit) {
            y = y.setBit(255);
        }
        return y;
    }

    public static FieldPoint decode(BigInteger encoded) throws EncryptionException {
        final var y = Params.convertBigIntegerToF2Element(encoded.mod(Key.POW_32));  // TODO: Potential endian problem here
        int signBit = (encoded.compareTo(BigInteger.ZERO) <= 0) ? 1 : 0;
        // int signBit = encoded.testBit(255) ? 1 : 0; TODO maybe this is better?

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
        BigInteger x0 = FP.PUtil.fpMul1271(t, t3);      // x0 = t*t3 //TODO in C this is coming from some pointer P->x0
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

        /* if (is_zero_ct((digit_t*)P->x, NWORDS_FIELD) == true) {
            signDec = ((digit_t*)&P->x[1])[NWORDS_FIELD-1] >> (sizeof(digit_t)*8 - 2);
        } else {
            signDec = ((digit_t*)&P->x[0])[NWORDS_FIELD-1] >> (sizeof(digit_t)*8 - 2);
        } */    // TODO: Convert this to Java somehow
//        int signDec;
//        if (x.real.equals(BigInteger.ZERO) && x.im.equals(BigInteger.ZERO)) {
//            // Entire x coordinate is zero, extract sign from imaginary part
//            signDec = x.im.shiftRight(125).intValue() & 0x3;  // Extract top 2 bits for 127-bit field
//        } else {
//            // x coordinate is non-zero, extract sign from real part
//            signDec = x.real.shiftRight(125).intValue() & 0x3;  // Extract top 2 bits for 127-bit field
//        }
        int signDec = 0;

        if (signBit != signDec) {           // If sign of x-coordinate decoded != input sign bit, then negate x-coordinate
            x = FP2.fp2Neg1271(x);
        }

        FieldPoint point = new FieldPoint(x, y);
        ExtendedPoint testPoint = Curve.pointSetup(point);
        if (!ECC.eccPointValidate(testPoint)) {
            testPoint.getX().im = FP.PUtil.fpNeg1271(testPoint.getX().im);
            point.getX().im = testPoint.getX().im;
            if (!ECC.eccPointValidate(testPoint)) {       // Final point validation
                throw new EncryptionException("");
            }
        }

        return point;
    }

}
