package types;

public class PreComputedExtendedPoint<Field> {
    public Field xy;
    public Field yx;
    public Field z;
    public Field t;
    public PreComputedExtendedPoint(Field _xy, Field _yx, Field _z, Field _t) {
        xy = _xy;
        yx = _yx;
        z = _z;
        t = _t;
    }

    public PreComputedExtendedPoint<Field> dup() {
        return new PreComputedExtendedPoint<Field>(
              this.xy,
              this.yx,
              this.z,
              this.t
        );
    }

}
