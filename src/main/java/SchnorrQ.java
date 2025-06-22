import types.Pair;
import types.FieldPoint;
import types.Key;



public class SchnorrQ {
    static final int KEY_SIZE = 32;

    static Key schnorrQKeyGeneration(Key secretKey) {
        FieldPoint<Integer> point;
        Key hash = HashFunction.computeHash(secretKey);     // Returns 64-byte hash of secret key
        point = ECCUtil.eccMulFixed(hash);
        return ECCUtil.encode(point);
    }

    static Pair<Key, Key> schnorrQFullKeyGeneration() {
        Key secretKey = CryptoUtil.randomBytes(KEY_SIZE);
        Key publicKey = schnorrQKeyGeneration(secretKey);
        return new Pair<>(secretKey, publicKey);
    }

    static Key schnorrQSign(Key secretKey, Key publicKey, byte[] message) {

    }

    static boolean schnorrQVerify(Key publicKey, Key signature, byte[] message) {}
}
