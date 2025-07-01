package constants;

import types.F2Element;

import java.math.BigInteger;

public class Params {
    private static final int HEX_RADIX = 16;

    public static final int RADIX = 32; //TODO confirm this and below
    public static final int RADIX64 = 64; //TODO confirm this

    public static final int NWORDS_ORDER = 8;
    public static final int NWORDS_FIELD = 4;

    public static final BigInteger PRIME_1271
            = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);
    public static final BigInteger MASK_127 = PRIME_1271;  // Same as 2^127 - 1

    public static final BigInteger W_VARBASE = BigInteger.valueOf(5);
    public static final BigInteger NPOINTS_VARBASE
            = BigInteger.ONE.shiftLeft(W_VARBASE.subtract(BigInteger.TWO).intValue());

    public static final BigInteger MONTGOMERY_R_PRIME = new BigInteger(
            "C81DB8795FF3D621173EA5AAEA6B387D3D01B7C72136F61C0006A5F16AC8F9D3",
            HEX_RADIX
    );
    public static final BigInteger CURVE_ORDER = new BigInteger(
            "2FB2540EC7768CE7DFBD004DFE0F7999F05397829CBC14E50029CBC14E5E0A72",
            HEX_RADIX
    );

    public static final BigInteger GENERATOR_X = new BigInteger(
            "286592AD7B3833AA1A3472237C2FB30596869FB360AC77F61E1F553F2878AA9C",
            HEX_RADIX
    );

    public static final BigInteger GENERATOR_Y = new BigInteger(
            "B924A2462BCBB2870E3FEE9BA120785A49A7C344844C8B5C6E1C4AF8630E0242",
            HEX_RADIX
    );

    public static final BigInteger PARAMETER_D = new BigInteger(
            "000000000000014200000000000000E4B3821488F1FC0C8D5E472F846657E0FC",
            HEX_RADIX
    );
}
