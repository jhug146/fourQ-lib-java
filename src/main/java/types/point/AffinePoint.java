package types.point;

import types.data.F2Element;

import java.util.Objects;

public class AffinePoint implements TablePoint {
    private F2Element x;
    private F2Element y;
    private F2Element t;

    public AffinePoint(F2Element x, F2Element y, F2Element t) {
        this.x = x;
        this.y = y;
        this.t = t;
    }

    public AffinePoint() {
        this.x = F2Element.ONE.dup();
        this.y = F2Element.ONE.dup();
    }

    @Override
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
    public F2Element getX() {
        return x;
    }

    @Override
    public void setX(F2Element x) {
        this.x = x;
    }

    @Override
    public F2Element getY() {
        return y;
    }

    public void setY(F2Element y) {
        this.y = y;
    }

    @Override
    public F2Element getT() {
        return t;
    }

    @Override
    public void setT(F2Element t) {
        this.t = t;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + t + ")";
    }
}
