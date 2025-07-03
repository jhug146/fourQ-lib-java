package types;

import constants.Params;

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
        this.x = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        this.y = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
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
    public void filterMaskForEach(
            TablePoint tempPoint,
            BigInteger mask,
            boolean modZ
    ) {
        x = x.applyMasks(tempPoint.getX(), mask);
        y = y.applyMasks(tempPoint.getY(), mask);
        t = t.applyMasks(tempPoint.getT(), mask);
    }

    @Override
    public F2Element getX() {
        return x;
    }

    @Override
    public F2Element getY() {
        return y;
    }

    @Override
    public F2Element getT() {
        return t;
    }

    @Override
    public F2Element getZ() {
        return null;
    }

    @Override
    public void setX(F2Element x) {
        this.x = x;
    }

    public void setY(F2Element y) {
        this.y = y;
    }

    @Override
    public void setT(F2Element t) {
        this.t = t;
    }

    @Override
    public void setZ(F2Element z) {}
}
