package types;

public class ExtendedAffinePoint<Field> {
    Field xy;
    Field yx;
    Field t;
    ExtendedAffinePoint(Field _xy, Field _yx, Field _t) {
        xy = _xy;
        yx = _yx;
        t = _t;
    }
}
