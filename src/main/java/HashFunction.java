import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import types.Key;

public class HashFunction {
    static Key computeHash(Key input) throws NoSuchAlgorithmException {
        return computeHash(input.key.toByteArray());
    }

    static Key computeHash(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        return new Key(digest.digest(bytes));
    }
}
