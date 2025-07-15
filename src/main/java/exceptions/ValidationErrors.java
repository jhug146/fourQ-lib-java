package exceptions;

import constants.Key;

import java.math.BigInteger;

public class ValidationErrors {
    public static boolean checkSignatureSize(BigInteger signature) {
        return signature.and(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)).compareTo(BigInteger.ONE.shiftLeft(246)) < 0;
    }

    public static void publicKeyError() throws InvalidArgumentException {
        throw new InvalidArgumentException(String.format(
                "Invalid argument: Bit %d is not set to zero in both the public key.",
                Key.PUB_TEST_BIT
        ));
    }

    public static void signatureError() throws InvalidArgumentException {
        throw new InvalidArgumentException(String.format(
                "Invalid argument: Bit %d is not set to zero in both the signature.",
                Key.SIG_TEST_BIT
        ));
    }

    public static void signatureSizeError() throws InvalidArgumentException {
        throw new InvalidArgumentException(String.format(
                "Invalid argument: Signature must be less than 2^%d.",
                Key.MAX_SIG_LENGTH
        ));
    }
}
