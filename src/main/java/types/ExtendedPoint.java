package types;


public class ExtendedPoint implements Point {
    public F2Element x;
    public F2Element y;
    public F2Element z;
    public F2Element ta;
    public F2Element tb;
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
    public F2Element getY() {
        return y;
    }

    public F2Element getZ() {
        return z;
    }

    @Override
    public void setX(F2Element x) {
        this.x = x;
    }

    @Override
    public void setY(F2Element y) {
        this.y = y;
    }

    public void setZ(F2Element z) {
        this.z = z;
    }
}
