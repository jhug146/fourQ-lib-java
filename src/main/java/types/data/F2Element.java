package types.data;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Represents an element in the quadratic extension field GF((2^127-1)^2).
 * 
 * Elements are represented as a + bi where a and b are elements of the base
 * field GF(2^127-1) and i^2 = -1. This representation is fundamental to
 * the FourQ curve implementation.
 * 
 * The class provides basic operations and utilities for working with
 * quadratic field elements, including equality testing, duplication,
 * and mask applications for constant-time operations.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class F2Element {
    public static final F2Element ONE = new F2Element(BigInteger.ONE, BigInteger.ZERO);

    public BigInteger real;
    public BigInteger im;
    /**
     * Constructs a new quadratic field element.
     * 
     * @param _real the real part (coefficient of 1)
     * @param _im the imaginary part (coefficient of i)
     */
    public F2Element(BigInteger _real, BigInteger _im) {
        real = _real;
        im = _im;
    }

    /**
     * Tests whether this element is the zero element (0 + 0i).
     * 
     * @return true if both real and imaginary parts are zero
     */
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

    /**
     * Creates a duplicate of this quadratic field element.
     * 
     * @return a new F2Element with the same real and imaginary parts
     */
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

    /**
     * Applies conditional masking for constant-time point operations.
     * 
     * This method is used in constant-time algorithms to conditionally
     * select between two field elements based on a mask, without revealing
     * which element was selected through timing analysis.
     * 
     * @param point2 the alternative element to potentially select
     * @param mask the selection mask (all 1s to select point2, all 0s for this)
     * @return the conditionally selected element
     */
    public F2Element applyMasks(@NotNull F2Element point2, @NotNull BigInteger mask) {
        return new F2Element(
                mask.and(this.real.xor(point2.real)).xor(this.real),
                mask.and(this.im.xor(point2.im)).xor(this.im)
        );
    }
}
