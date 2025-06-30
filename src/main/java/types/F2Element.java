package types;

import java.math.BigInteger;

public class F2Element {
    public BigInteger first;
    public BigInteger second;

    public F2Element(BigInteger first, BigInteger second) {
        this.first = first;
        this.second = second;
    }

    public F2Element() {
        first = BigInteger.ZERO;
        second = BigInteger.ZERO;
    }

    public boolean isZero() {
        return first.signum() == 0 && second.signum() == 0;
    }
}
