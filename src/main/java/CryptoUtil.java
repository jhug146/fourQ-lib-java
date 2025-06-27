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
        return FP.montgomeryMultiplyModOrder(key, Constants.MONTGOMERY_R_PRIME);
    }

    static BigInteger fromMontgomery(BigInteger key) {
        return FP.montgomeryMultiplyModOrder(key, BigInteger.ONE);
    }
}
