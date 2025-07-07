package constants;

import java.math.BigInteger;

public class Key {
    public static final int KEY_SIZE = 32;
    public static final int TEST_BIT = 128;
    public static final int MAX_SIG_LENGTH = 502;
    public static final BigInteger MAX_SIG_VERIFY = BigInteger.ONE.shiftLeft(MAX_SIG_LENGTH - 1);  // TODO: Check this
}
