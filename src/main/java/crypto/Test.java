package crypto;

import constants.Params;
import crypto.core.Curve;
import crypto.core.ECC;
import exceptions.EncryptionException;
import field.operations.FP;
import field.operations.FP2;
import types.data.F2Element;
import types.point.AffinePoint;
import types.point.ExtendedPoint;
import types.point.FieldPoint;
import types.point.PreComputedExtendedPoint;

import java.math.BigInteger;

public class Test {
    public static void main(String[] args) throws EncryptionException {
        FieldPoint generator = ECC.eccSet();
        System.out.println(generator);
        System.out.println(ECC.eccMul(generator, BigInteger.ONE, false));
    }
}
