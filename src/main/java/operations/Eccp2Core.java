package operations;

import types.F2Element;

public class Eccp2Core {

    // namespace for prime-based utility functions.
    public interface p2util {
        // Copy of a GF(p^2) element, output = a
        static F2Element fp2copy1271(F2Element a) {
           return new F2Element(FP.putil.fpcopy1271(a.first), FP.putil.fpcopy1271(a.second));
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
            return null;
        }

        // GF(p^2) multiplication, c = a*b in GF((2^127-1)^2)
        static F2Element fp2mul1271(F2Element a, F2Element b) {
            return null;
        }

        // GF(p^2) addition, c = a+b in GF((2^127-1)^2)
        static F2Element fp2add1271(F2Element a, F2Element b) {
            return null;
        }

        // GF(p^2) subtraction, c = a-b in GF((2^127-1)^2)
        static F2Element fp2sub1271(F2Element a, F2Element b) {
            return null;
        }

        // GF(p^2) addition followed by subtraction, c = 2a-b in GF((2^127-1)^2)
        static F2Element fp2addsub1271(F2Element a, F2Element b) {
            return null;
        }

        // GF(p^2) inversion, a = (a0-i*a1)/(a0^2+a1^2)
        static F2Element fp2inv1271(F2Element a) {
            return null;
        }
    }

    /*
     CURVE/SCALAR FUNCTIONS
     */


}
