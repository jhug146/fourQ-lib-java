package types.point;

import org.jetbrains.annotations.NotNull;
import types.data.F2Element;

import java.util.Objects;

public class AffinePoint implements TablePoint {
    @NotNull private F2Element x;
    @NotNull private F2Element y;
    @NotNull private F2Element t;

    public AffinePoint(@NotNull F2Element x, @NotNull F2Element y, @NotNull F2Element t) {
        this.x = x;
        this.y = y;
        this.t = t;
    }

    public AffinePoint() {
        this.x = F2Element.ONE.dup();
        this.y = F2Element.ONE.dup();
        this.t = F2Element.ZERO;
    }

    @Override
    @NotNull
    public AffinePoint dup() {
        return new AffinePoint(x, y, t);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AffinePoint that)) return false;
        return this.x.equals(that.x) && this.y.equals(that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, t);
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

    public void setY(@NotNull F2Element y) {
        this.y = y;
    }

    @Override
    @NotNull
    public F2Element getT() {
        return t;
    }

    @Override
    public void setT(@NotNull F2Element t) {
        this.t = t;
    }

    @Override
    @NotNull
    public String toString() {
        return "(" + x + ", " + y + ", " + t + ")";
    }
}
