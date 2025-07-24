package fourqj.constants;

import java.math.BigInteger;

public class Key {
    public static final int KEY_SIZE = 32;
    public static final int SIGNATURE_SIZE = KEY_SIZE * 2;
    public static final int PUB_TEST_BIT = 135;
    public static final int SIG_TEST_BIT = 391;
    public static final int MAX_SIG_LENGTH = 502;
    public static final BigInteger POW_256 = BigInteger.ONE.shiftLeft(256);
    public static final BigInteger POW_128 = BigInteger.ONE.shiftLeft(128);
}
