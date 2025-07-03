package types;

import java.math.BigInteger;
import java.util.Objects;

public class AffinePoint<Field> {
    public Field x;
    public Field y;
    public Field t;

    public AffinePoint(Field x, Field y, Field t) {
        this.x = x;
        this.y = y;
        this.t = t;
    }

    public AffinePoint() {
        this.x = (Field) new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        this.y = (Field) new F2Element(BigInteger.ZERO, BigInteger.ZERO);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AffinePoint<?> that)) return false;
        return this.x.equals(that.x) && this.y.equals(that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, t);
    }
}
