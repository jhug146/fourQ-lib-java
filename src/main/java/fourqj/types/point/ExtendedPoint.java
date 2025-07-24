package fourqj.types.point;

import fourqj.types.data.F2Element;

import static fourqj.utils.StringUtils.buildString;


public class ExtendedPoint implements Point {
    private F2Element x;
    private F2Element y;
    private final F2Element z;
    private final F2Element ta;
    private final F2Element tb;

    public ExtendedPoint( F2Element x,  F2Element y,  F2Element z,  F2Element ta,  F2Element tb) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ta = ta;
        this.tb = tb;
    }

    @Override
    public F2Element getX() {
        return x;
    }

    @Override
    public void setX( F2Element x) {
        this.x = x;
    }

    @Override
    public F2Element getY() {
        return y;
    }

    @Override
    public void setY( F2Element y) {
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
        return buildString(sb -> {
            sb.append("(");
            sb.append(x);
            sb.append(", ");
            sb.append(y);
            sb.append(", ");
            sb.append(z);
            sb.append(", ");
            sb.append(ta);
            sb.append(", ");
            sb.append(tb);
            sb.append(")");
        });
    }
}
