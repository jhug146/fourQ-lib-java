package types;

import java.math.BigInteger;

public class F2Elem {
    public BigInteger real;
    public BigInteger im;
    public F2Elem(BigInteger _real, BigInteger _im) {
        real = _real;
        im = _im;
    }

    public boolean isZero() {
        return real.signum() == 0 && im.signum() == 0;
    }
}
