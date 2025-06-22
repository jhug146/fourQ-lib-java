import java.security.SecureRandom;

import types.Key;

public class CryptoUtil {
    static Key randomBytes(int size) {
        SecureRandom secureRandom = new SecureRandom();    // TODO: Maybe a faster way
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return new Key(bytes);
    }
}
