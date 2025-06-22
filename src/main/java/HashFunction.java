import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import types.Key;

public class HashFunction {
    static Key computeHash(Key input) {
        return computeHash(input.key.toByteArray());
    }

    static Key computeHash(byte[] bytes) {}
}
