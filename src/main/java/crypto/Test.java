package crypto;

import constants.Params;
import crypto.core.ECC;
import exceptions.EncryptionException;
import field.operations.FP;
import field.operations.FP2;
import types.data.F2Element;
import types.point.AffinePoint;
import types.point.ExtendedPoint;
import types.point.FieldPoint;

import java.math.BigInteger;

public class Test {
    public static void main(String[] args) throws EncryptionException {
        AffinePoint generator = new AffinePoint(
                new F2Element(BigInteger.ZERO, BigInteger.ZERO),
                new F2Element(BigInteger.valueOf(1), BigInteger.ZERO),
                null
        );
        ECC.eccSet(generator);

//        System.out.println(generator.getX());
//        System.out.println(generator.getY());
        //ECCUtil.eccMul(generator, BigInteger.TEN, false);

        //System.out.println(new BigInteger("f068e2d286047d0a", 16));
        //System.out.println(new BigInteger(Long.toUnsignedString(0xf068e2d286047d0aL), 16));

        ExtendedPoint genExt = generator.toExtendedPoint();
//        System.out.println(ECC.eccPointValidate(genExt));
//        System.out.println(Params.PARAMETER_d.real + " i " + Params.PARAMETER_d.im);
//        System.out.println(convertToField(generator).getX());
//        System.out.println(convertToField(generator).getY());
        System.out.println(ECC.eccMul(convertToField(generator), BigInteger.ONE, false));
//        System.out.println(ECC.eccMul(convertToField(generator), BigInteger.ONE, false).getY());
//        System.out.println(Params.T_VARBASE);
//        System.out.println(FP2.fp2Add1271(
//                new F2Element(BigInteger.valueOf(2910894007959172010), new 10846532345338296310i,),
//                new F2Element(y=13340966418444432007 + 5307425384924220252i),
//        ));
        System.out.println(new BigInteger("1a3472237c2fb305286592ad7b3833aa", 16));
    }

    private static FieldPoint convertToField(AffinePoint affine) {
        return new FieldPoint(affine.getX(), affine.getY());
    }
}
