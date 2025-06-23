import java.math.BigInteger;
import java.security.SecureRandom;

import types.Key;

public class CryptoUtil {
    private static final int HEX_RADIX = 16;
    private static final BigInteger MONTGOMERY_R_VAL = new BigInteger(
            "0xC81DB8795FF3D621173EA5AAEA6B387D3D01B7C72136F61C0006A5F16AC8F9D3",
            HEX_RADIX
    );
    private static final Key MONTGOMERY_R_PRIME = new Key(MONTGOMERY_R_VAL, 256);
    private static final Key ONE = new Key(1, 256);

    static Key randomBytes(int size) {
        SecureRandom secureRandom = new SecureRandom();    // TODO: Maybe a faster way
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return new Key(bytes);
    }

    static Key toMontgomery(Key key) {        // TODO: I'm unsure about the argument type here
        return FP.montgomeryMultiplyModOrder(key, MONTGOMERY_R_PRIME);
    }

    static Key fromMontgomery(Key key) {
        return FP.montgomeryMultiplyModOrder(key, ONE);
    }
}
