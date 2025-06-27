package types;

public class ExtendedPoint<Field> {
    Field x;
    Field y;
    Field z;
    Field ta;
    Field tb;
    public ExtendedPoint(Field _x, Field _y, Field _z, Field _ta, Field _tb) {
        x = _x;
        y = _y;
        z = _z;
        ta = _ta;
        tb = _tb;
    }
}
