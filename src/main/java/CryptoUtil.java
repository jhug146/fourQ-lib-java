import constants.Params;
import operations.FP;
import types.F2Element;
import types.FieldPoint;

import java.math.BigInteger;
import java.security.SecureRandom;

public class CryptoUtil {
    private static final SecureRandom secureRandom = new SecureRandom();

    static BigInteger randomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return new BigInteger(bytes);
    }

    static BigInteger toMontgomery(BigInteger key) {        // TODO: I'm unsure about the argument type here
        return FP.montgomeryMultiplyModOrder(key, Params.MONTGOMERY_R_PRIME);
    }

    static BigInteger fromMontgomery(BigInteger key) {
        return FP.montgomeryMultiplyModOrder(key, BigInteger.ONE);
    }

    static BigInteger encode(FieldPoint<F2Element> point) {
        BigInteger y = point.y.real.add(point.y.im.shiftLeft(128));
        boolean ySignBit = point.y.real.compareTo(BigInteger.ZERO) <= 0;

        if (ySignBit) {
            y = y.setBit(255);
        }
        return y;
    }

    static FieldPoint<F2Element> decode(BigInteger encoded) {}

}
