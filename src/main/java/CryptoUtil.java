import java.math.BigInteger;
import java.security.SecureRandom;

public class CryptoUtil {
    static BigInteger randomBytes(int size) {
        SecureRandom secureRandom = new SecureRandom();    // TODO: Maybe a faster way
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return new BigInteger(bytes);
    }

    static BigInteger toMontgomery(BigInteger key) {        // TODO: I'm unsure about the argument type here
        return FP.montgomeryMultiplyModOrder(key, FourQConstants.MONTGOMERY_R_PRIME);
    }

    static BigInteger fromMontgomery(BigInteger key) {
        return FP.montgomeryMultiplyModOrder(key, FourQConstants.ONE);
    }
}
