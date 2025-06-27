package types;

import java.math.BigInteger;

<<<<<<< HEAD:src/main/java/types/F2Elem.java
public class F2Elem {
    public BigInteger real;
    public BigInteger im;
    public F2Elem(BigInteger _real, BigInteger _im) {
        real = _real;
        im = _im;
=======
public class F2Element {
    public BigInteger first;
    public BigInteger second;
    public F2Element(BigInteger _first, BigInteger _second) {
        first = _first;
        second = _second;
>>>>>>> 948116873ff5dc54f86c12512e37cac007296044:src/main/java/types/F2Element.java
    }

    public boolean isZero() {
        return real.signum() == 0 && im.signum() == 0;
    }
}
