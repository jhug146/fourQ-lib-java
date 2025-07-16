package constants;

import java.math.BigInteger;

import types.data.F2Element;


public class Params {
    public static final int HEX_RADIX = 16;
    public static final int NWORDS_ORDER = 8;

    public static final BigInteger PRIME_1271
            = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);
    public static final BigInteger MASK_127 = PRIME_1271;  // Same as 2^127 - 1

    public static final BigInteger W_VARBASE = BigInteger.valueOf(5);
    public static final BigInteger N_POINTS_VARBASE
            = BigInteger.ONE.shiftLeft(W_VARBASE.subtract(BigInteger.TWO).intValue());
    public static final int N_BITS_ORDER_PLUS_ONE = 247;
    public static final int T_VARBASE = (N_BITS_ORDER_PLUS_ONE + W_VARBASE.intValueExact() - 2) / (W_VARBASE.intValueExact() - 1);

    public static final BigInteger MONTGOMERY_R_PRIME = new BigInteger(
            "0006A5F16AC8F9D33D01B7C72136F61C173EA5AAEA6B387DC81DB8795FF3D621",
            HEX_RADIX
    );
    public static final BigInteger MONTGOMERY_r_PRIME = new BigInteger(
            "F32702FDAFC1C074BCE409ED76B5DB21D75E78B8D1FCDCF3E12FE5F079BC3929",
            HEX_RADIX
    );

    public static final BigInteger CURVE_ORDER = new BigInteger(
            "0029CBC14E5E0A72F05397829CBC14E5DFBD004DFE0F79992FB2540EC7768CE7",
            HEX_RADIX
    );

    public static F2Element PARAMETER_D = new F2Element(
            new BigInteger("00000000000000E40000000000000142", HEX_RADIX),
            new BigInteger("5E472F846657E0FCB3821488F1FC0C8D", HEX_RADIX)
    );

    public static F2Element GENERATOR_X = new F2Element(
            new BigInteger("1A3472237C2FB305286592AD7B3833AA", HEX_RADIX),
            new BigInteger("1E1F553F2878AA9C96869FB360AC77F6", HEX_RADIX)
    );
    public static F2Element GENERATOR_Y = new F2Element(
            new BigInteger("0E3FEE9BA120785AB924A2462BCBB287", HEX_RADIX),
            new BigInteger("6E1C4AF8630E024249A7C344844C8B5C", HEX_RADIX)
    );
}
