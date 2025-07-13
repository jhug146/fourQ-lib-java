package types.point;


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
    private F2Element x;
    private F2Element y;

    /**
     * Constructs a new point with the given coordinates.
     *
     * @param x the x-coordinate in GF(p^2)
     * @param y the y-coordinate in GF(p^2)
     */
    public FieldPoint(F2Element x, F2Element y) {
        this.x = x;
        this.y = y;
    }

    public boolean isZero() {
        return x.isZero() && y.isZero();
    }

    @Override
    public F2Element getX() {
        return x;
    }

    @Override
    public void setX(F2Element x) {
        this.x = x;
    }

    @Override
    public F2Element getY() {
        return y;
    }

    @Override
    public void setY(F2Element y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
