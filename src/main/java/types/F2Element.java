package types;

import java.math.BigInteger;

public class F2Element {
    public BigInteger first;
    public BigInteger second;
    public F2Element(BigInteger _first, BigInteger _second) {
        first = _first;
        second = _second;
    }

    public boolean isZero() {
        return first.signum() == 0 && second.signum() == 0;
    }
}
