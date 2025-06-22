package types;

import java.math.BigInteger;

public class Key {
    public BigInteger key;
    private int size;    // Size of key in bytes
    public Key(int _key, int _size) {
        key = BigInteger.valueOf(_key);
        size = _size;
    }

    public Key(byte[] bytes) {
        key = new BigInteger(bytes);
        size = bytes.length;
    }

    public Key(Key _key, int _size) {
        key = _key.key;
        size = _size;
    }

    public Key(BigInteger _key, int _size) {
        key = _key;
        size = _size;
    }

    void setSize(int _size) {
        if (size > 0) {
            size = _size;
        }
    }

    int getSize() {
        return size;
    }
}
