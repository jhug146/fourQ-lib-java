package types.point;


import org.jetbrains.annotations.NotNull;
import types.data.F2Element;

/**
 * Represents a point on the FourQ elliptic curve in affine coordinates.
 * <p>
 * This class stores curve points as (x, y) coordinates where both x and y
 * are elements of the quadratic extension field GF((2^127-1)^2). Affine
 * coordinates are the most natural representation but require field
 * inversions for point arithmetic.
 * <p>
 * The class implements the Point interface and provides basic point
 * operations including coordinate access and zero testing.
 *
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class FieldPoint implements Point {
    @NotNull private F2Element x;
    @NotNull private F2Element y;

    /**
     * Constructs a new point with the given coordinates.
     *
     * @param x the x-coordinate in GF(p^2)
     * @param y the y-coordinate in GF(p^2)
     */
    public FieldPoint(@NotNull F2Element x, @NotNull F2Element y) {
        this.x = x;
        this.y = y;
    }

    public boolean isZero() {
        return x.isZero() && y.isZero();
    }

    @Override
    @NotNull
    public F2Element getX() {
        return x;
    }

    @Override
    public void setX(@NotNull F2Element x) {
        this.x = x;
    }

    @Override
    @NotNull
    public F2Element getY() {
        return y;
    }

    @Override
    public void setY(@NotNull F2Element y) {
        this.y = y;
    }

    @Override
    @NotNull
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
