package constants;

import java.math.BigInteger;

public class Key {
    public static final int KEY_SIZE = 32;
    public static final int TEST_BIT = 128;
    public static final int MAX_SIG_LENGTH = 502;
    public static final BigInteger POW_256 = BigInteger.ONE.shiftLeft(256);
    public static final BigInteger POW_128 = BigInteger.ONE.shiftLeft(128);
}
