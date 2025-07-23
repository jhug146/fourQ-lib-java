package exceptions;

import java.math.BigInteger;

import constants.Key;


public class ValidationErrors {
    public static boolean isSignatureSizeTooLarge(BigInteger signature) {
        for (int i = 0; i < 8; i++) {
            if (signature.testBit(i)) {
                return true;
            }
        }
        return signature.testBit(14) || signature.testBit(15);
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
