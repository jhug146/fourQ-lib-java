package types;

public class ExtendedPoint {
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
}
