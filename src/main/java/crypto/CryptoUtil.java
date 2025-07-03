package crypto;

import constants.Params;
import exceptions.EncryptionException;
import operations.FP;
import operations.FP2;
import types.point.ExtendedPoint;
import types.data.F2Element;
import types.point.FieldPoint;

import java.math.BigInteger;
import java.security.SecureRandom;

public class CryptoUtil {
    private static final BigInteger POW_32 = BigInteger.ONE.shiftLeft(32);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final F2Element PARAM_D_F2 = ECCUtil.convertToF2Element(Params.PARAMETER_D);
    private static final F2Element ONE = new F2Element(BigInteger.ONE, BigInteger.ZERO);

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
        final var y = ECCUtil.convertToF2Element(encoded.mod(POW_32));  // TODO: Potential endian problem here
        int signBit = (encoded.compareTo(BigInteger.ZERO) <= 0) ? 1 : 0;

        F2Element u = FP2.fp2Sqr1271(y);
        F2Element v = FP2.fp2Mul1271(u, PARAM_D_F2);
        u = FP2.fp2Sub1271(u, ONE);
        v = FP2.fp2Add1271(v, ONE);

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

        /* if (is_zero_ct((digit_t*)P->x, NWORDS_FIELD) == true) {
            signDec = ((digit_t*)&P->x[1])[NWORDS_FIELD-1] >> (sizeof(digit_t)*8 - 2);
        } else {
            signDec = ((digit_t*)&P->x[0])[NWORDS_FIELD-1] >> (sizeof(digit_t)*8 - 2);
        } */    // TODO: Convert this to Java somehow
        int signDec = 0;

        if (signBit != signDec) {           // If sign of x-coordinate decoded != input sign bit, then negate x-coordinate
            x = FP2.fp2Neg1271(x);
        }

        FieldPoint point = new FieldPoint(x, y);
        ExtendedPoint testPoint = ECCUtil.pointSetup(point);
        if (!ECCUtil.eccPointValidate(testPoint)) {
            testPoint.getX().im = FP.PUtil.fpNeg1271(testPoint.getX().im);
            point.getX().im = testPoint.getX().im;
            if (!ECCUtil.eccPointValidate(testPoint)) {       // Final point validation
                throw new EncryptionException("");
            }
        }

        return point;
    }

}
