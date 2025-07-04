package crypto;

import types.data.F2Element;
import types.point.AffinePoint;
import types.point.ExtendedPoint;

import java.math.BigInteger;

public class Test {
    public static void main(String[] args) {
        AffinePoint generator = new AffinePoint(
                new F2Element(BigInteger.ZERO, BigInteger.ZERO),
                new F2Element(BigInteger.valueOf(1), BigInteger.ZERO),
                null
        );

        ExtendedPoint genExt = generator.toExtendedPoint();
        System.out.println(ECCUtil.eccPointValidate(genExt));
        //System.out.println(ECCUtil.eccMul(generator.to, BigInteger.valueOf(0), false));
    }
}
