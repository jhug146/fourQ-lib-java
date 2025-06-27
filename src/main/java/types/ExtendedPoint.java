package types;

public class ExtendedPoint<Field> {
    public Field x;
    public Field y;
    public Field z;
    public Field ta;
    public Field tb;
    public ExtendedPoint(Field _x, Field _y, Field _z, Field _ta, Field _tb) {
        x = _x;
        y = _y;
        z = _z;
        ta = _ta;
        tb = _tb;
    }
}
