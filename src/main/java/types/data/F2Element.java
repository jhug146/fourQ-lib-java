package types.data;

import java.math.BigInteger;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import constants.Params;
import utils.StringUtils;

import static utils.StringUtils.buildString;


/**
 * Represents an element in the quadratic extension field GF((2^127-1)^2).
 * <p>
 * Elements are represented as a + bi where a and b are elements of the base
 * field GF(2^127-1) and i^2 = -1. This representation is fundamental to
 * the FourQ curve implementation.
 * <p>
 * The class provides basic operations and utilities for working with
 * quadratic field elements, including equality testing, duplication,
 * and mask applications for constant-time operations.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class F2Element {
    public static final F2Element ONE = new F2Element(BigInteger.ONE, BigInteger.ZERO);
    public static final F2Element ZERO = new F2Element(BigInteger.ZERO, BigInteger.ZERO);

    public BigInteger real;
    public BigInteger im;
    /**
     * Constructs a new quadratic field element.
     * @param _real the real part (coefficient of 1)
     * @param _im the imaginary part (coefficient of i)
     */
    public F2Element(BigInteger _real, BigInteger _im) {
        real = _real;
        im = _im;
    }

    /**
     * Tests whether this element is the zero element (0 + 0i).
     * @return true if both real and imaginary parts are zero
     */
    public boolean isZero() {
        return real.signum() == 0 && im.signum() == 0;
    }

    @Override
    public boolean equals(Object o) {
        return switch (o) {
            case F2Element f2Element -> this.real.equals(f2Element.real) && this.im.equals(f2Element.im);
            case null, default -> false;
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(real, im);
    }

    /**
     * Creates a duplicate of this quadratic field element.
     * @return a new F2Element with the same real and imaginary parts
     */
    @NotNull
    public F2Element dup() {
        return new F2Element(
                this.real,
                this.im
        );
    }

    @Override
    public String toString() {
        return buildString(sb -> {
            sb.append("0x");
            sb.append(real.toString(Params.HEX_RADIX));
            sb.append(" + 0x");
            sb.append(im.toString(Params.HEX_RADIX));
            sb.append("i");
        });
    }
}
