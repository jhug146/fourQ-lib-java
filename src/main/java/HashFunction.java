import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class HashFunction {
    static BigInteger computeHash(BigInteger input) throws NoSuchAlgorithmException {
        return computeHash(input.toByteArray());
    }

    static BigInteger computeHash(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        return new BigInteger(digest.digest(bytes));
    }
}
