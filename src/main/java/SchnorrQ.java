import java.math.BigInteger;
import java.util.Arrays;

import constants.ArrayUtils;
import constants.Key;
import crypto.CryptoUtil;
import crypto.core.ECC;
import crypto.HashFunction;
import exceptions.EncryptionException;
import exceptions.InvalidArgumentException;
import field.operations.FP;
import types.data.Pair;
import types.point.FieldPoint;

import static constants.ArrayUtils.reverseByteArray;


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
        byte[] signBuilder = new byte[message.length + 2 * Key.KEY_SIZE];

        System.arraycopy(
                ArrayUtils.bigIntegerToByte(kHash, 2 * Key.KEY_SIZE, false),
                0,
                signBuilder,
                Key.KEY_SIZE+message.length,
                Key.KEY_SIZE
        );
        System.arraycopy(message, 0, signBuilder, Key.KEY_SIZE, message.length);

        BigInteger rHash = HashFunction.computeHashReversed(Arrays.copyOfRange(signBuilder, Key.KEY_SIZE, signBuilder.length));
        final FieldPoint rPoint = ECC.eccMulFixed(rHash);
        final BigInteger sigStart = CryptoUtil.encode(rPoint);

        byte[] publicKeyBytes = ArrayUtils.bigIntegerToByte(publicKey, Key.KEY_SIZE, false);
        byte[] revBytes = reverseByteArray(signBuilder, true);
        System.arraycopy(
                ArrayUtils.bigIntegerToByte(sigStart, Key.KEY_SIZE, false),
                0,
                revBytes,
                0,
                Key.KEY_SIZE
        );
        System.arraycopy(publicKeyBytes, 0, revBytes, Key.KEY_SIZE, Key.KEY_SIZE);
        signBuilder = revBytes.clone();

        rHash = FP.moduloOrder(rHash);
        BigInteger hHash2 = HashFunction.computeHash(signBuilder);
        hHash2 = FP.moduloOrder(hHash2);

        BigInteger sigEnd = CryptoUtil.toMontgomery(kHash);
        hHash2 = CryptoUtil.toMontgomery(hHash2);
        sigEnd = FP.montgomeryMultiplyModOrder(sigEnd, hHash2);
        sigEnd = CryptoUtil.fromMontgomery(sigEnd);

        sigEnd = FP.subtractModOrder(rHash, sigEnd);

        byte[] sigStartBytes = ArrayUtils.bigIntegerToByte(sigStart, Key.KEY_SIZE, false);
        byte[] sigEndBytes   = reverseByteArray(ArrayUtils.bigIntegerToByte(sigEnd,   Key.KEY_SIZE, false), false);
        return new BigInteger(1, ArrayUtils.concat(sigStartBytes, sigEndBytes));
    }

    public static boolean schnorrQVerify(BigInteger publicKey, BigInteger signature, byte[] message) throws EncryptionException {
        if (signature == null) {
            throw new InvalidArgumentException("Signature is Null");
        }

        if (publicKey.testBit(Key.TEST_BIT) || signature.testBit(Key.TEST_BIT)) {
            throw new InvalidArgumentException(String.format(
                    "Invalid argument: Bit %d is not set to zero in both the public key and signature.",
                    Key.TEST_BIT
            ));
        }

        if (signature.and(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)).compareTo(BigInteger.ONE.shiftLeft(246)) < 0){
            throw new InvalidArgumentException(String.format(
                    "Invalid argument: Signature must be less than 2^%d.",
                    Key.MAX_SIG_LENGTH
            ));
        }

        final byte[] bytes = new byte[message.length + 2 * Key.KEY_SIZE];
        System.arraycopy(signature.toByteArray(), 0, bytes, 0, Key.KEY_SIZE);
        System.arraycopy(ArrayUtils.bigIntegerToByte(publicKey, Key.KEY_SIZE, true), 0, bytes, Key.KEY_SIZE, Key.KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * Key.KEY_SIZE, message.length);

        var a = CryptoUtil.decode(publicKey); // Implicitly checks that public key lies on the curve

        FieldPoint affPoint = ECC.eccMulDouble(
                new BigInteger(Arrays.copyOfRange(bytes, Key.KEY_SIZE, bytes.length - 1)),
                a,
                HashFunction.computeHash(bytes)
        );

        BigInteger encoded = CryptoUtil.encode(affPoint);
        return encoded.equals(signature);
    }
}
