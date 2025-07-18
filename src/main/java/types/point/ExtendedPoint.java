package types.point;

import org.jetbrains.annotations.NotNull;

import types.data.F2Element;

import static utils.StringUtils.buildString;


public class ExtendedPoint implements Point {
    @NotNull private F2Element x;
    @NotNull private F2Element y;
    @NotNull private final F2Element z;
    @NotNull private final F2Element ta;
    @NotNull private final F2Element tb;

    public ExtendedPoint(@NotNull F2Element _x, @NotNull F2Element _y, @NotNull F2Element _z, @NotNull F2Element _ta, @NotNull F2Element _tb) {
        x = _x;
        y = _y;
        z = _z;
        ta = _ta;
        tb = _tb;
    }

    @Override
    @NotNull
    public F2Element getX() {
        return x;
    }

    @Override
    public void setX(@NotNull F2Element x) {
        this.x = x;
    }

    @Override
    @NotNull
    public F2Element getY() {
        return y;
    }

    @Override
    public void setY(@NotNull F2Element y) {
        this.y = y;
    }

    @NotNull
    public F2Element getZ() {
        return z;
    }

    @NotNull
    public F2Element getTa() {
        return ta;
    }

    @NotNull
    public F2Element getTb() {
        return tb;
    }

    @NotNull
    public ExtendedPoint dup() {
        return new ExtendedPoint(x.dup(), y.dup(), z.dup(), ta.dup(), tb.dup());
    }

    @Override
    @NotNull
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
