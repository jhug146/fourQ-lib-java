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
import org.jetbrains.annotations.NotNull;
import types.data.Pair;
import types.point.FieldPoint;

import static constants.ArrayUtils.reverseByteArray;
import static constants.ArrayUtils.reverseByteArrayKeepLeadingZero;


public class SchnorrQ {
    public static BigInteger schnorrQKeyGeneration(@NotNull BigInteger secretKey) throws EncryptionException {
        BigInteger hash = HashFunction.computeHash(secretKey, true);
        final FieldPoint point = ECC.eccMulFixed(hash);
        return CryptoUtil.encode(point);
    }

    public static Pair<BigInteger, BigInteger> schnorrQFullKeyGeneration() throws EncryptionException {
        final BigInteger secretKey = CryptoUtil.randomBytes(Key.KEY_SIZE);
        final BigInteger publicKey = schnorrQKeyGeneration(secretKey);
        return new Pair<>(secretKey, publicKey);
    }

    public static BigInteger schnorrQSign(
            @NotNull BigInteger secretKey,
            @NotNull BigInteger publicKey,
            byte[] message
    ) throws EncryptionException {
        final BigInteger kHash = HashFunction.computeHash(secretKey, true);
        byte[] bytes = new byte[message.length + 2 * Key.KEY_SIZE];

        System.arraycopy(
                ArrayUtils.bigIntegerToByte(kHash, 2 * Key.KEY_SIZE, false),
                0,
                bytes,
                Key.KEY_SIZE+message.length,
                Key.KEY_SIZE
        );
        System.arraycopy(message, 0, bytes, Key.KEY_SIZE, message.length);

        BigInteger rHash = HashFunction.computeHashReversed(Arrays.copyOfRange(bytes, Key.KEY_SIZE, bytes.length), bytes.length - Key.KEY_SIZE);
        final FieldPoint rPoint = ECC.eccMulFixed(rHash);
        final BigInteger sigStart = CryptoUtil.encode(rPoint);

        byte[] publicKeyBytes = ArrayUtils.bigIntegerToByte(publicKey, Key.KEY_SIZE, false);
        byte[] revBytes = reverseByteArray(bytes, true);
        revBytes = ArrayUtils.concat(new byte[bytes.length - revBytes.length], revBytes);
        System.arraycopy(
                ArrayUtils.bigIntegerToByte(sigStart, Key.KEY_SIZE, false),
                0,
                revBytes,
                0,
                Key.KEY_SIZE
        );
        System.arraycopy(publicKeyBytes, 0, revBytes, Key.KEY_SIZE, Key.KEY_SIZE);
        bytes = revBytes.clone();

        rHash = FP.moduloOrder(rHash);
        BigInteger hHash2 = HashFunction.computeHash(bytes, true);
        hHash2 = FP.moduloOrder(hHash2);

        BigInteger sigEnd = CryptoUtil.toMontgomery(kHash);
        hHash2 = CryptoUtil.toMontgomery(hHash2);
        sigEnd = FP.montgomeryMultiplyModOrder(sigEnd, hHash2);
        sigEnd = CryptoUtil.fromMontgomery(sigEnd);

        sigEnd = FP.subtractModOrder(rHash, sigEnd);

        byte[] sigStartBytes = ArrayUtils.bigIntegerToByte(sigStart, Key.KEY_SIZE, false);
        byte[] sigEndBytes   = reverseByteArrayKeepLeadingZero(ArrayUtils.bigIntegerToByte(sigEnd,   Key.KEY_SIZE, false));
        return new BigInteger(1, ArrayUtils.concat(sigStartBytes, sigEndBytes));
    }

    public static boolean schnorrQVerify(@NotNull BigInteger publicKey, @NotNull BigInteger signature, byte[] message) throws EncryptionException {
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
        System.arraycopy(ArrayUtils.bigIntegerToByte(publicKey, Key.KEY_SIZE, false), 0, bytes, Key.KEY_SIZE, Key.KEY_SIZE);
        System.arraycopy(ArrayUtils.reverseByteArray(message), 0, bytes, 2 * Key.KEY_SIZE, message.length);

        final BigInteger sig32 = signature.mod(Key.POW_256);
        FieldPoint affPoint = ECC.eccMulDouble(
                ArrayUtils.reverseBigInteger(sig32),
                CryptoUtil.decode(publicKey),       // Implicitly checks that public key lies on the curve
                HashFunction.computeHash(bytes, true)
        );

        final BigInteger encoded = CryptoUtil.encode(affPoint);
        return encoded.equals(signature.divide(Key.POW_256));
    }
}
