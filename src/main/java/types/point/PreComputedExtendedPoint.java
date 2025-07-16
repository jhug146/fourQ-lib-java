package types.point;

import org.jetbrains.annotations.NotNull;
import types.data.F2Element;


public class PreComputedExtendedPoint implements TablePoint {
    public F2Element xy;
    public F2Element yx;
    public F2Element z;
    public F2Element t;

    public PreComputedExtendedPoint(F2Element _xy, F2Element _yx, F2Element _z, F2Element _t) {
        xy = _xy;
        yx = _yx;
        z = _z;
        t = _t;
    }

    @NotNull
    public PreComputedExtendedPoint dup() {
        return new PreComputedExtendedPoint(this.xy, this.yx, this.z, this.t);
    }

    @Override
    public F2Element getX() {
        return xy;
    }

    @Override
    public void setX(F2Element x) {
        this.xy = x;
    }

    @Override
    public F2Element getY() {
        return yx;
    }

    @Override
    public void setY(F2Element y) {
        this.yx = y;
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
    public String toString() {
        return "(xy = " + xy + ", yx = " + yx + ", z = " + z + ", t = " + t + ")";
    }
}
