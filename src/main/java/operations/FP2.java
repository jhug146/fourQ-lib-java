package operations;

import types.*;

import java.math.BigInteger;

public class FP2 {
    // Copy of a GF(p^2) element, output = a
    public static F2Element fp2copy1271(F2Element a) {
        return a;  // BigInteger is immutable, so copy is not needed
    }

    // Zeroing a GF(p^2) element, a = 0
    public static F2Element fp2zero1271(F2Element a) {
        return new F2Element(FP.PUtil.fpZero1271(a.real), FP.PUtil.fpZero1271(a.im));
    }

    // GF(p^2) negation, a = -a in GF((2^127-1)^2)
    public static F2Element fp2neg1271(F2Element a) {
        return new F2Element(FP.PUtil.fpNeg1271(a.real), FP.PUtil.fpNeg1271(a.im));
    }

    // GF(p^2) squaring, c = a^2 in GF((2^127-1)^2)
    public static F2Element fp2sqr1271(F2Element a) {
        BigInteger t3 = FP.PUtil.fpMul1271(a.real, a.im);
        return new F2Element(
                FP.PUtil.fpMul1271(
                        FP.PUtil.fpAdd1271(a.real, a.im),
                        FP.PUtil.fpSub1271(a.real, a.im)
                ),                          // first = (a0+a1)(a0-a1)
                FP.PUtil.fpAdd1271(t3, t3)  // second = 2a0*a1
        );
    }

    // GF(p^2) multiplication, c = a*b in GF((2^127-1)^2)
    public static F2Element fp2mul1271(F2Element a, F2Element b) {
        BigInteger t1 = FP.PUtil.fpMul1271(a.real, b.real);     // t1 = a0*b0
        BigInteger t2 = FP.PUtil.fpMul1271(a.im, b.im);         // t2 = a1*b1
        BigInteger t3 = FP.PUtil.fpAdd1271(a.real, a.im);       // t2 = a1*b1
        BigInteger t4 = FP.PUtil.fpAdd1271(b.real, b.im);       // t4 = b0+b1

        t3 = FP.PUtil.fpMul1271(t3, t4);                        // t3 = (a0+a1)*(b0+b1)
        t3 = FP.PUtil.fpSub1271(t3, t1);                        // t3 = (a0+a1)*(b0+b1) - a0*b0

        return new F2Element(
                FP.PUtil.fpSub1271(t1, t2),                     // first = a0*b0 - a1*b1
                FP.PUtil.fpSub1271(t3, t2)                      // second = (a0+a1)*(b0+b1) - a0*b0 - a1*b1
        );
    }

    // GF(p^2) addition, c = a+b in GF((2^127-1)^2)
    public static F2Element fp2add1271(F2Element a, F2Element b) {
        return new F2Element(
                FP.PUtil.fpAdd1271(a.real, b.real),
                FP.PUtil.fpAdd1271(a.im, b.im)
        );
    }

    // GF(p^2) subtraction, c = a-b in GF((2^127-1)^2)
    public static F2Element fp2sub1271(F2Element a, F2Element b) {
        return new F2Element(
                FP.PUtil.fpSub1271(a.real, b.real),
                FP.PUtil.fpSub1271(a.im, b.im)
        );
    }

    // GF(p^2) addition followed by subtraction, c = 2a-b in GF((2^127-1)^2)
    public static F2Element fp2addsub1271(F2Element a, F2Element b) {
        a = fp2add1271(a, a);
        return fp2sub1271(a, b);
    }

    // GF(p^2) inversion, a = (a0-i*a1)/(a0^2+a1^2)
    public static F2Element fp2inv1271(F2Element a) {
        F2Element t1 = new F2Element(
                FP.PUtil.fpSqr1271(a.real),                 // t1.first = a0^2
                FP.PUtil.fpSqr1271(a.im)                    // t1.second = a1^2
        );

        t1.real = FP.PUtil.fpAdd1271(t1.real, t1.im);       // t1.first = a0^2+a1^2
        t1.real = FP.PUtil.fpInv1271(t1.real);              // t10 = (a0^2+a1^2)^-1
        a.im = FP.PUtil.fpNeg1271(a.im);                    // a = a0-i*a1
        a.real = FP.PUtil.fpMul1271(a.real, t1.real);
        a.im = FP.PUtil.fpMul1271(a.im, t1.real);           // a = (a0-i*a1)*(a0^2+a1^2)^-1
        return a;
    }

    // GF(p^2) division by two c = a/2 mod p
    public static F2Element fp2div1271(F2Element a) {
        return new F2Element(
                FP.PUtil.fpDiv1271(a.im),
                FP.PUtil.fpDiv1271(a.real)
        );
    }
}
