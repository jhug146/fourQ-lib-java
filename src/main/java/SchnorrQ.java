import java.math.BigInteger;
import java.util.Arrays;

import types.Pair;
import types.FieldPoint;
import types.Key;


// TODO: Error handling in all methods
public class SchnorrQ {
    static final int KEY_SIZE = 32;

    public static Key schnorrQKeyGeneration(Key secretKey) {
        final Key hash = HashFunction.computeHash(secretKey);     // Returns 64-byte hash of secret key
        final FieldPoint<Integer> point = ECCUtil.eccMulFixed(hash);
        return ECCUtil.encode(point);
    }

    public static Pair<Key, Key> schnorrQFullKeyGeneration() {
        final Key secretKey = CryptoUtil.randomBytes(KEY_SIZE);
        final Key publicKey = schnorrQKeyGeneration(secretKey);
        return new Pair<>(secretKey, publicKey);
    }

    public static Key schnorrQSign(Key secretKey, Key publicKey, byte[] message) {
        final Key kHash = HashFunction.computeHash(secretKey);
        final byte[] bytes = new byte[message.length + 2 * KEY_SIZE];
        System.arraycopy(kHash.key.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * KEY_SIZE, message.length);

        Key rHash = HashFunction.computeHash(Arrays.copyOfRange(bytes, KEY_SIZE, bytes.length));
        final FieldPoint<Integer> rPoint = ECCUtil.eccMulFixed(rHash);
        final Key sigStart = ECCUtil.encode(rPoint);

        System.arraycopy(sigStart.key.toByteArray(), 0, bytes, 0, KEY_SIZE);
        System.arraycopy(publicKey.key.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);

        final Key hHash = HashFunction.computeHash(bytes);
        rHash = FP.moduloOrder(rHash);
        Key hHash2 = new Key(0, KEY_SIZE * 2);
        hHash2 = FP.moduloOrder(hHash2);

        Key sigEnd = CryptoUtil.toMontgomery(kHash);
        hHash2 = CryptoUtil.toMontgomery(hHash2);
        sigEnd = FP.montgomeryMultiplyModOrder(sigEnd, hHash2);
        sigEnd = CryptoUtil.fromMontgomery(sigEnd);

        sigEnd = FP.subtractModOrder(rHash, sigEnd);
        BigInteger signature = sigStart.key.add(sigEnd.key.shiftLeft(KEY_SIZE));
        return new Key(signature, KEY_SIZE * 2);
    }

    public static boolean schnorrQVerify(Key publicKey, Key signature, byte[] message) {
        // TODO: Validate arguments
        // TODO: Verify that 'A' is on the curve
        final byte[] bytes = new byte[message.length + 2 * KEY_SIZE];
        System.arraycopy(signature.key.toByteArray(), 0, bytes, 0, KEY_SIZE);
        System.arraycopy(publicKey.key.toByteArray(), 0, bytes, KEY_SIZE, KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * KEY_SIZE, message.length);

        // TODO: Check this hash worked
        Key hash = HashFunction.computeHash(bytes);
        // TODO: Finish this
        return false;
    }
}
