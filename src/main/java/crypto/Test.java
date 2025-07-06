package crypto;

import constants.Params;
import crypto.core.Conversions;
import crypto.core.Curve;
import crypto.core.ECC;
import exceptions.EncryptionException;
import types.point.AffinePoint;
import types.point.ExtendedPoint;
import types.point.FieldPoint;
import types.point.PreComputedExtendedPoint;

import java.math.BigInteger;

public class Test {
    public static void main(String[] args) throws EncryptionException {
        ExtendedPoint p = Curve.pointSetup(ECC.eccSet());
        /*p = ECC.eccDouble(p);
        System.out.println(p);
        PreComputedExtendedPoint q = Conversions.r1ToR3(p);
        System.out.println(q);*/




        PreComputedExtendedPoint[] table = ECC.eccPrecomp(p);
        for (PreComputedExtendedPoint point : table) {
            System.out.println(point);
        }
    }
}
