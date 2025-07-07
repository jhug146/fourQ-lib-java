import constants.Key;
import crypto.CryptoUtil;
import crypto.core.ECC;
import exceptions.EncryptionException;
import types.point.FieldPoint;

import java.math.BigInteger;
import java.util.Arrays;

public class Experiment {
    public static void main(String[] args) throws EncryptionException {
        BigInteger key = new BigInteger("ef62ed2fc90ffbdaca6f63fc92dd53c579828a0ccf44b6c922ddb596ec3e9102", 16);
        BigInteger pubKey = SchnorrQ.schnorrQKeyGeneration(key);
        System.out.println(pubKey.toString(16));
    }
}
