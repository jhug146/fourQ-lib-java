package types.data;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Objects;

public class F2Element {
    public static final F2Element ONE = new F2Element(BigInteger.ONE, BigInteger.ONE);

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

    public F2Element dup() {
        return new F2Element(
                this.real,
                this.im
        );
    }

    @Override
    public String toString() {
        return "0x" + real.toString(16) + " + 0x" + im.toString(16) + "i";
    }

    public F2Element applyMasks(@NotNull F2Element point2, @NotNull BigInteger mask) {
        return new F2Element(
                mask.and(this.real.xor(point2.real)).xor(this.real),
                mask.and(this.im.xor(point2.im)).xor(this.im)
        );
    }
}
