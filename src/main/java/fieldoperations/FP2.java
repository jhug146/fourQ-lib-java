package fieldoperations;

import java.math.BigInteger;

import org.jetbrains.annotations.NotNull;

import types.data.F2Element;


/**
 * Quadratic extension field arithmetic for GF((2^127-1)^2).
 * <p>
 * This class implements arithmetic in the quadratic extension field
 * GF(p^2) where p = 2^127-1. Elements are represented as a + bi where
 * a and b are elements of the base field GF(p) and i^2 = -1.
 * <p>
 * The FourQ curve is defined over this quadratic extension field,
 * allowing for more efficient curve operations compared to curves
 * over prime fields of similar security levels.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class FP2 {
    /**
     * Creates a copy of a GF(p^2) element.
     * <p>
     * Since BigInteger objects are immutable, this simply returns the input.
     * 
     * @param a the element to copy
     * @return a copy of the input element
     */
    @NotNull
    public static F2Element fp2Copy1271(@NotNull F2Element a) {
        return new F2Element(a.real, a.im);
    }

    // GF(p^2) negation, a = -a in GF((2^127-1)^2)
    @NotNull
    public static F2Element fp2Neg1271(@NotNull F2Element a) {
        return new F2Element(FP.PUtil.fpNeg1271(a.real), FP.PUtil.fpNeg1271(a.im));
    }

    /**
     * Squares an element in GF(p^2) using optimized formulas.
     * <p>
     * For element a = a0 + a1*i, computes a^2 using the identity:
     * a^2 = (a0 + a1)(a0 - a1) + 2*a0*a1*i
     * This saves one multiplication compared to general multiplication.
     * 
     * @param a the element to square
     * @return a^2 in GF(p^2)
     */
    @NotNull
    public static F2Element fp2Sqr1271(@NotNull F2Element a) {
        BigInteger t3 = FP.PUtil.fpMul1271(a.real, a.im);
        return new F2Element(
                FP.PUtil.fpMul1271(
                        FP.PUtil.fpAdd1271(a.real, a.im),
                        FP.PUtil.fpSub1271(a.real, a.im)
                ),                          // first = (a0+a1)(a0-a1)
                FP.PUtil.fpAdd1271(t3, t3)  // second = 2a0*a1
        );
    }

    /**
     * Multiplies two elements in GF(p^2) using the relation i^2 = -1.
     * <p>
     * For elements a = a0 + a1*i and b = b0 + b1*i, computes:
     * c = a*b = (a0*b0 - a1*b1) + (a0*b1 + a1*b0)*i
     * 
     * @param a first multiplicand
     * @param b second multiplicand
     * @return the product a*b in GF(p^2)
     */
    @NotNull
    public static F2Element fp2Mul1271(@NotNull F2Element a, @NotNull F2Element b) {
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
    @NotNull
    public static F2Element fp2Add1271(@NotNull F2Element a, @NotNull F2Element b) {
        return new F2Element(
                FP.PUtil.fpAdd1271(a.real, b.real),
                FP.PUtil.fpAdd1271(a.im, b.im)
        );
    }

    // GF(p^2) subtraction, c = a-b in GF((2^127-1)^2)
    @NotNull
    public static F2Element fp2Sub1271(@NotNull F2Element a, @NotNull F2Element b) {
        return new F2Element(
                FP.PUtil.fpSub1271(a.real, b.real),
                FP.PUtil.fpSub1271(a.im, b.im)
        );
    }

    // GF(p^2) addition followed by subtraction, c = 2a-b in GF((2^127-1)^2)
    @NotNull
    public static F2Element fp2AddSub1271(@NotNull F2Element a, @NotNull F2Element b) {
        a = fp2Add1271(a, a);
        return fp2Sub1271(a, b);
    }

    /**
     * Computes the multiplicative inverse of an element in GF(p^2).
     * <p>
     * For element a = a0 + a1*i, computes a^(-1) using the formula:
     * a^(-1) = (a0 - a1*i) / (a0^2 + a1^2)
     * where the division is performed in the base field GF(p).
     * 
     * @param a the element to invert (must be non-zero)
     * @return the multiplicative inverse a^(-1)
     */
    @NotNull
    public static F2Element fp2Inv1271(@NotNull F2Element a) {
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
    @NotNull
    public static F2Element fp2Div1271(@NotNull F2Element a) {
        return new F2Element(
                FP.PUtil.fpDiv1271(a.im),
                FP.PUtil.fpDiv1271(a.real)
        );
    }
}
