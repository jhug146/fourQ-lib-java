package types;

import constants.Params;

import java.math.BigInteger;
import java.util.Objects;

public class AffinePoint<Field> implements Point<Field> {
    public Field x;
    public Field y;
    public Field t;

    public AffinePoint(Field x, Field y, Field t) {
        this.x = x;
        this.y = y;
        this.t = t;
    }

    public AffinePoint() {
        this.x = (Field) new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        this.y = (Field) new F2Element(BigInteger.ZERO, BigInteger.ZERO);
    }

    @Override
    public int getTableLength() {
        return Params.VPOINTS_FIXEDBASE;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AffinePoint<?> that)) return false;
        return this.x.equals(that.x) && this.y.equals(that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, t);
    }

    public void filterMaskForEach(
            PreComputedExtendedPoint<F2Element> tempPoint,
            BigInteger mask,
            boolean modifyZ
    ) {
        x = (Field) ((F2Element) x).applyMasks(tempPoint.xy, mask);
        y = (Field) ((F2Element) y).applyMasks(tempPoint.yx, mask);
    }

    @Override
    public F2Element getX() {
        return (F2Element) x;
    }

    public void setX(F2Element x) {
        this.x = (Field) x;
    }

    @Override
    public F2Element getY() {
        return (F2Element) y;
    }

    @Override
    public F2Element getZ() {
        return null;
    }

    public void setY(F2Element y) {
        this.y = (Field) y;
    }

    @Override
    public void setZ(F2Element z) {

    }

    @Override
    public F2Element getT() {
        return (F2Element) t;
    }

    public void setT(F2Element t) {
        this.t = (Field) t;
    }
}
