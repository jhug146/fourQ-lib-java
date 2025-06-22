package types;

import java.math.BigInteger;

public class Key {
    BigInteger key;
    final int size;    // Size of key in bytes
    public Key(int _key, int _size) {
        key = BigInteger.valueOf(_key);
        size = _size;
    }

    public Key(byte[] bytes) {
        key = new BigInteger(bytes);
        size = bytes.length;
    }
}
