import types.Pair;
import types.Key;

public class SchnorrQ {
    static Key schnorrQKeyGeneration(Key secretKey) {}

    static Pair<Key, Key> schnorrQFullKeyGeneration() {}

    static Key schnorrQSign(Key secretKey, Key publicKey, byte[] message) {}

    static boolean schnorrQVerify(Key publicKey, Key signature, byte[] message) {}
}
