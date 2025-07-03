import java.math.BigInteger;
import java.util.Arrays;

import crypto.CryptoUtil;
import crypto.ECCUtil;
import crypto.HashFunction;
import exceptions.EncryptionException;
import exceptions.InvalidArgumentException;
import operations.FP;
import types.F2Element;
import types.Pair;
import types.FieldPoint;


public class SchnorrQ {
    private static final int KEY_SIZE = 32;
    private static final int TEST_BIT = 128;
    private static final int MAX_SIG_LENGTH = 502;
    private static final BigInteger MAX_SIG_VERIFY = BigInteger.ONE.shiftLeft(MAX_SIG_LENGTH - 1);  // TODO: Check this

    public static BigInteger schnorrQKeyGeneration(BigInteger secretKey) throws EncryptionException {
        final BigInteger hash = HashFunction.computeHash(secretKey);
        final FieldPoint point = ECCUtil.eccMulFixed(hash);
        return CryptoUtil.encode(point);
    }

    public static Pair<BigInteger, BigInteger> schnorrQFullKeyGeneration() throws EncryptionException {
        final BigInteger secretKey = CryptoUtil.randomBytes(KEY_SIZE);
        final BigInteger publicKey = schnorrQKeyGeneration(secretKey);
        return new Pair<>(secretKey, publicKey);
    }

    public static BigInteger schnorrQSign(BigInteger secretKey, BigInteger publicKey, byte[] message) throws EncryptionException {
        final BigInteger kHash = HashFunction.computeHash(secretKey);
        final byte[] bytes = new byte[message.length + 2 * KEY_SIZE];
        System.arraycopy(kHash.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * KEY_SIZE, message.length);

        BigInteger rHash = HashFunction.computeHash(Arrays.copyOfRange(bytes, KEY_SIZE, bytes.length));
        final FieldPoint rPoint = ECCUtil.eccMulFixed(rHash);
        final BigInteger sigStart = CryptoUtil.encode(rPoint);

        System.arraycopy(sigStart.toByteArray(), 0, bytes, 0, KEY_SIZE);
        System.arraycopy(publicKey.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);

        HashFunction.computeHash(bytes);    // Checks whether hashing works on bytes
        rHash = FP.moduloOrder(rHash);
        BigInteger hHash2 = BigInteger.ONE;
        hHash2 = FP.moduloOrder(hHash2);

        BigInteger sigEnd = CryptoUtil.toMontgomery(kHash);
        hHash2 = CryptoUtil.toMontgomery(hHash2);
        sigEnd = FP.montgomeryMultiplyModOrder(sigEnd, hHash2);
        sigEnd = CryptoUtil.fromMontgomery(sigEnd);

        sigEnd = FP.subtractModOrder(rHash, sigEnd);
        return sigStart.add(sigEnd.shiftLeft(KEY_SIZE));
    }

    public static boolean schnorrQVerify(BigInteger publicKey, BigInteger signature, byte[] message) throws EncryptionException, InvalidArgumentException {
        if (!publicKey.testBit(TEST_BIT) || !signature.testBit(TEST_BIT)) {
            throw new InvalidArgumentException(String.format(
                    "Invalid argument: Bit %d is not set to zero in both the public key and signature.",
                    TEST_BIT
            ));
        }

        if (signature.compareTo(MAX_SIG_VERIFY) >= 0) {
            throw new InvalidArgumentException(String.format(
                    "Invalid argument: Signature must be less than 2^%d.",
                    MAX_SIG_LENGTH
            ));
        }

        final byte[] bytes = new byte[message.length + 2 * KEY_SIZE];
        System.arraycopy(signature.toByteArray(), 0, bytes, 0, KEY_SIZE);
        System.arraycopy(publicKey.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * KEY_SIZE, message.length);

        FieldPoint affPoint = ECCUtil.eccMulDouble(
                new BigInteger(Arrays.copyOfRange(bytes, KEY_SIZE, bytes.length - 1)),
                CryptoUtil.decode(publicKey),       // Implicitly checks that public key lies on the curve
                HashFunction.computeHash(bytes)
        );
        if (affPoint == null) {
            return false;
        }

        BigInteger encoded = CryptoUtil.encode(affPoint);
        return encoded.equals(signature);
    }
}
