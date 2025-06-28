package operations;

import constants.Params;
import types.F2Element;
import types.FieldPoint;

import java.math.BigInteger;

public class Eccp2Core {

    // namespace for prime-based utility functions in p^2.
    public interface p2util {
        // Copy of a GF(p^2) element, output = a
        static F2Element fp2copy1271(F2Element a) {
            return a;  // BigInteger is immutable, so copy is not needed
        }

        // Zeroing a GF(p^2) element, a = 0
        static F2Element fp2zero1271(F2Element a) {
            return new F2Element(FP.putil.fpzero1271(a.first), FP.putil.fpzero1271(a.second));
        }

        // GF(p^2) negation, a = -a in GF((2^127-1)^2)
        static F2Element fp2neg1271(F2Element a) {
            return new F2Element(FP.putil.fpneg1271(a.first), FP.putil.fpneg1271(a.second));
        }

        // GF(p^2) squaring, c = a^2 in GF((2^127-1)^2)
        static F2Element fp2sqr1271(F2Element a) {
            BigInteger t3 = FP.putil.fpmul1271(a.first, a.second);
            return new F2Element(
                    FP.putil.fpmul1271(
                            FP.putil.fpadd1271(a.first, a.second),
                            FP.putil.fpsub1271(a.first, a.second)
                    ), // first = (a0+a1)(a0-a1)
                    FP.putil.fpadd1271(t3, t3)  // second = 2a0*a1
            );
        }

        // GF(p^2) multiplication, c = a*b in GF((2^127-1)^2)
        static F2Element fp2mul1271(F2Element a, F2Element b) {
            BigInteger t1 = FP.putil.fpmul1271(a.first, b.first);   // t1 = a0*b0
            BigInteger t2 = FP.putil.fpmul1271(a.second, b.second); // t2 = a1*b1
            BigInteger t3 = FP.putil.fpadd1271(a.first, a.second);  // t2 = a1*b1
            BigInteger t4 = FP.putil.fpadd1271(b.first, b.second);  // t4 = b0+b1

            t3 = FP.putil.fpmul1271(t3, t4);                        // t3 = (a0+a1)*(b0+b1)
            t3 = FP.putil.fpsub1271(t3, t1);                        // t3 = (a0+a1)*(b0+b1) - a0*b0

            return new F2Element(
                    FP.putil.fpsub1271(t1, t2),                     // first = a0*b0 - a1*b1
                    FP.putil.fpsub1271(t3, t2)                      // second = (a0+a1)*(b0+b1) - a0*b0 - a1*b1
            );
        }

        // GF(p^2) addition, c = a+b in GF((2^127-1)^2)
        static F2Element fp2add1271(F2Element a, F2Element b) {
            return new F2Element(
                    FP.putil.fpadd1271(a.first, b.first),
                    FP.putil.fpadd1271(a.second, b.second)
            );
        }

        // GF(p^2) subtraction, c = a-b in GF((2^127-1)^2)
        static F2Element fp2sub1271(F2Element a, F2Element b) {
            return new F2Element(
                    FP.putil.fpsub1271(a.first, b.first),
                    FP.putil.fpsub1271(a.second, b.second)
            );
        }

        // GF(p^2) addition followed by subtraction, c = 2a-b in GF((2^127-1)^2)
        static F2Element fp2addsub1271(F2Element a, F2Element b) {
            a = fp2add1271(a, a);
            return fp2sub1271(a, b);
        }

        // GF(p^2) inversion, a = (a0-i*a1)/(a0^2+a1^2)
        static F2Element fp2inv1271(F2Element a) {
            F2Element t1 = new F2Element(
                    FP.putil.fpsqr1271(a.first),                // t1.first = a0^2
                    FP.putil.fpsqr1271(a.second)                // t1.second = a1^2
            );

            t1.first = FP.putil.fpadd1271(t1.first, t1.second); // t1.first = a0^2+a1^2
            t1.first = FP.putil.fpinv1271(t1.first);            // t10 = (a0^2+a1^2)^-1
            a.second = FP.putil.fpneg1271(a.second);            // a = a0-i*a1
            a.first = FP.putil.fpmul1271(a.first, t1.first);
            a.second = FP.putil.fpmul1271(a.second, t1.first);  // a = (a0-i*a1)*(a0^2+a1^2)^-1
            return a;
        }
    }

    /*
     CURVE/SCALAR FUNCTIONS
     */

    // TODO CLEAR WORDS IMPLEMENTATION NEEDED?

    // Set generator
    // Output: P = (x,y)
    // TODO VeRy unsure about this.
    public static void eccset(FieldPoint P) {
        P.x = Params.GENERATOR_X;    // X1
        P.y = Params.GENERATOR_Y;    // Y1
    }



}
