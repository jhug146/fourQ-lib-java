import java.math.BigInteger;
import java.security.SecureRandom;

import types.Key;

public class CryptoUtil {
    static Key randomBytes(int size) {
        SecureRandom secureRandom = new SecureRandom();    // TODO: Maybe a faster way
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return new Key(bytes);
    }

    static Key toMontgomery(Key key) {        // TODO: I'm unsure about the argument type here
        return FP.montgomeryMultiplyModOrder(key, Constants.MONTGOMERY_R_PRIME);
    }

    static Key fromMontgomery(Key key) {
        return FP.montgomeryMultiplyModOrder(key, Constants.ONE);
    }
}
