package types.point;


import types.data.F2Element;

public class ExtendedPoint implements Point {
    private F2Element x;
    private F2Element y;
    private final F2Element z;
    private final F2Element ta;
    private final F2Element tb;

    public ExtendedPoint(F2Element _x, F2Element _y, F2Element _z, F2Element _ta, F2Element _tb) {
        x = _x;
        y = _y;
        z = _z;
        ta = _ta;
        tb = _tb;
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

    public F2Element getZ() {
        return z;
    }

    public F2Element getTa() {
        return ta;
    }

    public F2Element getTb() {
        return tb;
    }

    public ExtendedPoint dup() {
        return new ExtendedPoint(x.dup(), y.dup(), z.dup(), ta.dup(), tb.dup());
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ", " + ta + ", " + tb + ")";
    }
}
