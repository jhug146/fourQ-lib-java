package types.point;

import org.jetbrains.annotations.NotNull;

import types.data.F2Element;


public class PreComputedExtendedPoint implements TablePoint {
    @NotNull private F2Element xy;
    @NotNull private F2Element yx;
    @NotNull private final F2Element z;
    @NotNull private F2Element t;

    public PreComputedExtendedPoint(@NotNull F2Element _xy, @NotNull F2Element _yx, @NotNull F2Element _z, @NotNull F2Element _t) {
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
    @NotNull
    public F2Element getX() {
        return xy;
    }

    @Override
    public void setX(@NotNull F2Element x) {
        this.xy = x;
    }

    @Override
    @NotNull
    public F2Element getY() {
        return yx;
    }

    @Override
    public void setY(@NotNull F2Element y) {
        this.yx = y;
    }

    @NotNull
    public F2Element getZ() { return z; }

    @Override
    @NotNull
    public F2Element getT() {
        return t;
    }

    @Override
    public void setT(@NotNull F2Element t) {
        this.t = t;
    }

    @Override
    @NotNull
    public String toString() {
        return "(xy = " + xy + ", yx = " + yx + ", z = " + z + ", t = " + t + ")";
    }
}
