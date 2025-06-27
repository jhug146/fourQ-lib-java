import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import types.F2Element;
import types.Pair;
import types.FieldPoint;


// TODO: Error handling in all methods
public class SchnorrQ {
    private static final int KEY_SIZE = 32;

    public static BigInteger schnorrQKeyGeneration(BigInteger secretKey) throws NoSuchAlgorithmException {
        final BigInteger hash = HashFunction.computeHash(secretKey);     // Returns 64-byte hash of secret key
        final FieldPoint<F2Element> point = ECCUtil.eccMulFixed(hash);
        return ECCUtil.encode(point);
    }

    public static Pair<BigInteger, BigInteger> schnorrQFullKeyGeneration() throws NoSuchAlgorithmException {
        final BigInteger secretKey = CryptoUtil.randomBytes(KEY_SIZE);
        final BigInteger publicKey = schnorrQKeyGeneration(secretKey);
        return new Pair<>(secretKey, publicKey);
    }

    public static BigInteger schnorrQSign(BigInteger secretKey, BigInteger publicKey, byte[] message) throws NoSuchAlgorithmException {
        final BigInteger kHash = HashFunction.computeHash(secretKey);
        final byte[] bytes = new byte[message.length + 2 * KEY_SIZE];
        System.arraycopy(kHash.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * KEY_SIZE, message.length);

        BigInteger rHash = HashFunction.computeHash(Arrays.copyOfRange(bytes, KEY_SIZE, bytes.length));
        final FieldPoint<F2Element> rPoint = ECCUtil.eccMulFixed(rHash);
        final BigInteger sigStart = ECCUtil.encode(rPoint);

        System.arraycopy(sigStart.toByteArray(), 0, bytes, 0, KEY_SIZE);
        System.arraycopy(publicKey.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);

        final BigInteger hHash = HashFunction.computeHash(bytes);
        rHash = FP.moduloOrder(rHash);
        BigInteger hHash2 = BigInteger.valueOf(1);
        hHash2 = FP.moduloOrder(hHash2);

        BigInteger sigEnd = CryptoUtil.toMontgomery(kHash);
        hHash2 = CryptoUtil.toMontgomery(hHash2);
        sigEnd = FP.montgomeryMultiplyModOrder(sigEnd, hHash2);
        sigEnd = CryptoUtil.fromMontgomery(sigEnd);

        sigEnd = FP.subtractModOrder(rHash, sigEnd);
        return sigStart.add(sigEnd.shiftLeft(KEY_SIZE));
    }

    public static boolean schnorrQVerify(BigInteger publicKey, BigInteger signature, byte[] message) throws NoSuchAlgorithmException{
        // TODO: Validate arguments
        // TODO: Verify that 'A' is on the curve
        final byte[] bytes = new byte[message.length + 2 * KEY_SIZE];
        System.arraycopy(signature.toByteArray(), 0, bytes, 0, KEY_SIZE);
        System.arraycopy(publicKey.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * KEY_SIZE, message.length);

        // TODO: Check this hash worked
        BigInteger hash = HashFunction.computeHash(bytes);
        // TODO: Finish this
        return false;
    }
}
