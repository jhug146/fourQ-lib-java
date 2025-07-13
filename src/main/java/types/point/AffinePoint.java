package types.point;

import constants.Params;
import exceptions.TablePointCastException;
import types.data.F2Element;

import java.math.BigInteger;
import java.util.Objects;

public class AffinePoint implements TablePoint {
    private F2Element x;
    private F2Element y;
    private F2Element t;

    public AffinePoint(F2Element x, F2Element y, F2Element t) {
        this.x = x;
        this.y = y;
        this.t = t;
    }

    public AffinePoint() {
        this.x = F2Element.ONE.dup();
        this.y = F2Element.ONE.dup();
    }

    public ExtendedPoint toExtendedPoint() {
        return new ExtendedPoint(this.x, this.y, new F2Element(BigInteger.ONE, BigInteger.ZERO), this.x, this.y);
    }

    @Override
    public AffinePoint dup() {
        return new AffinePoint(x, y, t);
    }

    @Override
    public int getTableLength() {
        return Params.VPOINTS_FIXEDBASE;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AffinePoint that)) return false;
        return this.x.equals(that.x) && this.y.equals(that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, t);
    }

    @Override
    public PreComputedExtendedPoint toPreComputedExtendedPoint() {
        throw new TablePointCastException("Trying to cast Affine to unsupported PreComputedExtendedPoint via TableLookup.");
    }

    @Override
    public AffinePoint toAffinePoint() {
        return this;
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

    public void setY(F2Element y) {
        this.y = y;
    }

    @Override
    public F2Element getT() {
        return t;
    }

    @Override
    public void setT(F2Element t) {
        this.t = t;
    }

    @Override
    public F2Element getZ() {
        return null;
    }

    @Override
    public void setZ(F2Element z) {
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + t + ")";
    }
}
