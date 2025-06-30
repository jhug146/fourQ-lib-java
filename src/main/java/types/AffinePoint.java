package types;

import java.math.BigInteger;

public class AffinePoint<Field> {
    public F2Element x;
    public F2Element y;
    public Field t; //TODO This seems to be unneeded, from the c typedef.
    public AffinePoint(F2Element x, F2Element y, Field _t) {
        this.x = x;
        this.y = y;
        t = _t;
    }

    public AffinePoint() {
        this.x = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        this.y = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
    }


}
