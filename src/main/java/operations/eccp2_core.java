package operations;

import types.F2Element;

import java.math.BigInteger;

public class eccp2_core {

    // namespace for prime-based utility functions.
    public interface putil {
        // Copy of a GF(p^2) element, output = a
        static F2Element fp2copy1271(F2Element a) {
           return new F2Element(FP.putil.fpcopy1271(a.first), FP.putil.fpcopy1271(a.second));
        }

        // Zeroing a GF(p^2) element, a = 0
        static F2Element fp2zero1271(F2Element a) {
            return new F2Element(FP.putil.fpzero1271(a.first), FP.putil.fpzero1271(a.second))
        }
    }
}
