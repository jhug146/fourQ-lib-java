package fourqj.types.point;

import fourqj.types.data.F2Element;

import static fourqj.utils.StringUtils.buildString;

public class PreComputedExtendedPoint implements TablePoint {
    private F2Element xy;
    private F2Element yx;
    private final F2Element z;
    private F2Element t;

    public PreComputedExtendedPoint( F2Element xy,  F2Element yx,  F2Element z,  F2Element t) {
        this.xy = xy;
        this.yx = yx;
        this.z = z;
        this.t = t;
    }

    public PreComputedExtendedPoint dup() {
        return new PreComputedExtendedPoint(this.xy, this.yx, this.z, this.t);
    }

    @Override
    public F2Element getX() {
        return xy;
    }

    @Override
    public void setX( F2Element x) {
        this.xy = x;
    }

    @Override
    public F2Element getY() {
        return yx;
    }

    @Override
    public void setY( F2Element y) {
        this.yx = y;
    }


    public F2Element getZ() { return z; }

    @Override
    public F2Element getT() {
        return t;
    }

    @Override
    public void setT( F2Element t) {
        this.t = t;
    }

    @Override
    public String toString() {
        return buildString(sb -> {
            sb.append("(xy = ");
            sb.append(xy);
            sb.append(", yx = ");
            sb.append(yx);
            sb.append(", z = ");
            sb.append(z);
            sb.append(", t = ");
            sb.append(t);
            sb.append(")");
        });
    }
}
