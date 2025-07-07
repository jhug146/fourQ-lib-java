import java.math.BigInteger;
import java.util.Arrays;

import constants.Key;
import crypto.CryptoUtil;
import crypto.core.ECC;
import crypto.HashFunction;
import exceptions.EncryptionException;
import exceptions.InvalidArgumentException;
import field.operations.FP;
import types.data.Pair;
import types.point.FieldPoint;


public class SchnorrQ {
    public static BigInteger schnorrQKeyGeneration(BigInteger secretKey) throws EncryptionException {
        BigInteger hash = HashFunction.computeHash(secretKey);
        final FieldPoint point = ECC.eccMulFixed(hash);
        return CryptoUtil.encode(point);
    }

    public static Pair<BigInteger, BigInteger> schnorrQFullKeyGeneration() throws EncryptionException {
        final BigInteger secretKey = CryptoUtil.randomBytes(Key.KEY_SIZE);
        final BigInteger publicKey = schnorrQKeyGeneration(secretKey);
        return new Pair<>(secretKey, publicKey);
    }

    public static BigInteger schnorrQSign(
            BigInteger secretKey,
            BigInteger publicKey,
            byte[] message
    ) throws EncryptionException {
        final BigInteger kHash = HashFunction.computeHash(secretKey);
        final byte[] bytes = new byte[message.length + 2 * Key.KEY_SIZE];

        System.arraycopy(kHash.toByteArray(), 0, bytes, Key.KEY_SIZE, Key.KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * Key.KEY_SIZE, message.length);

        BigInteger rHash = HashFunction.computeHash(Arrays.copyOfRange(bytes, Key.KEY_SIZE, bytes.length));
        final FieldPoint rPoint = ECC.eccMulFixed(rHash);
        final BigInteger sigStart = CryptoUtil.encode(rPoint);

        byte[] publicKeyBytes = CryptoUtil.bigIntegerToByte(publicKey, Key.KEY_SIZE, false);
        System.arraycopy(sigStart.toByteArray(), 0, bytes, 0, Key.KEY_SIZE);
        System.arraycopy(publicKeyBytes, 0, bytes, Key.KEY_SIZE, Key.KEY_SIZE);

        HashFunction.computeHash(bytes);    // Checks whether hashing works on bytes
        rHash = FP.moduloOrder(rHash);
        BigInteger hHash2 = BigInteger.ONE;
        hHash2 = FP.moduloOrder(hHash2);

        BigInteger sigEnd = CryptoUtil.toMontgomery(kHash);
        hHash2 = CryptoUtil.toMontgomery(hHash2);
        sigEnd = FP.montgomeryMultiplyModOrder(sigEnd, hHash2);
        sigEnd = CryptoUtil.fromMontgomery(sigEnd);

        sigEnd = FP.subtractModOrder(rHash, sigEnd);
        return sigStart.add(sigEnd.shiftLeft(Key.KEY_SIZE));
    }

    public static boolean schnorrQVerify(BigInteger publicKey, BigInteger signature, byte[] message) throws EncryptionException {
        if (signature == null) {
            throw new InvalidArgumentException("Signature is Null");
        }
        if (!publicKey.testBit(Key.TEST_BIT) || !signature.testBit(Key.TEST_BIT)) {
            throw new InvalidArgumentException(String.format(
                    "Invalid argument: Bit %d is not set to zero in both the public key and signature.",
                    Key.TEST_BIT
            ));
        }

        if (signature.compareTo(Key.MAX_SIG_VERIFY) >= 0) {
            throw new InvalidArgumentException(String.format(
                    "Invalid argument: Signature must be less than 2^%d.",
                    Key.MAX_SIG_LENGTH
            ));
        }

        final byte[] bytes = new byte[message.length + 2 * Key.KEY_SIZE];
        System.arraycopy(signature.toByteArray(), 0, bytes, 0, Key.KEY_SIZE);
        System.arraycopy(publicKey.toByteArray(), 0, bytes, Key.KEY_SIZE, Key.KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * Key.KEY_SIZE, message.length);

        FieldPoint affPoint = ECC.eccMulDouble(
                new BigInteger(Arrays.copyOfRange(bytes, Key.KEY_SIZE, bytes.length - 1)),
                CryptoUtil.decode(publicKey),       // Implicitly checks that public key lies on the curve
                HashFunction.computeHash(bytes)
        );

        BigInteger encoded = CryptoUtil.encode(affPoint);
        return encoded.equals(signature);
    }
}
