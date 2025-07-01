package types;

import java.math.BigInteger;
import java.util.Objects;

public class F2Element {
    public BigInteger real;
    public BigInteger im;
    public F2Element(BigInteger _real, BigInteger _im) {
        real = _real;
        im = _im;
    }

    public boolean isZero() {
        return real.signum() == 0 && im.signum() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof F2Element f2Element)) return false;
        return this.real.equals(f2Element.real) && this.im.equals(f2Element.im);
    }

    @Override
    public int hashCode() {
        return Objects.hash(real, im);
    }
}
