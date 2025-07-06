import crypto.core.ECC;
import exceptions.EncryptionException;
import types.point.FieldPoint;

import java.math.BigInteger;

public class Experiment {
    public static void main(String[] args) throws EncryptionException {
        FieldPoint generator = ECC.eccSet();
        System.out.println(generator);
        System.out.println(ECC.eccMul(generator, BigInteger.ONE, false));
    }
}
