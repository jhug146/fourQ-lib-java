package crypto;

import crypto.core.ECC;
import exceptions.EncryptionException;
import types.point.AffinePoint;
import types.point.FieldPoint;

import java.math.BigInteger;

public class Test {
    public static void main(String[] args) throws EncryptionException {
        FieldPoint generator = ECC.eccSet();
        System.out.println(generator);
        FieldPoint res = ECC.eccMul(generator, BigInteger.ONE, false);
        System.out.println(res);
    }
}
