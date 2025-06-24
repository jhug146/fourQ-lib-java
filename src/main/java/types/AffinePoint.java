package types;

public class AffinePoint<Field> {
    Field xy;
    Field yx;
    Field t;
    AffinePoint(Field _xy, Field _yx, Field _t) {
        xy = _xy;
        yx = _yx;
        t = _t;
    }
}
