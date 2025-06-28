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
    }
}
