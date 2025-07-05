package crypto;

import crypto.util.ECC;
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
        ECC.eccSet(generator);

        //ECCUtil.eccMul(generator, BigInteger.TEN, false);

        System.out.println(new BigInteger("f068e2d286047d0a", 16));
        System.out.println(new BigInteger(Long.toUnsignedString(0xf068e2d286047d0aL), 16));

        ExtendedPoint genExt = generator.toExtendedPoint();
        //System.out.println(ECCUtil.eccPointValidate(genExt));
       // System.out.println(Params.PARAMETER_d.real + " i " + Params.PARAMETER_d.im);
        //System.out.println(ECCUtil.eccMul(generator.to, BigInteger.valueOf(0), false));
    }
}
