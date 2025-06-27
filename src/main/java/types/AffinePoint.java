package types;

public class AffinePoint<Field> {
    public Field xy;
    public Field yx;
    public Field t;
    public AffinePoint(Field _xy, Field _yx, Field _t) {
        xy = _xy;
        yx = _yx;
        t = _t;
    }
}
