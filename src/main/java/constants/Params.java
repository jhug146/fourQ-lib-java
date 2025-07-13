package constants;

import types.data.F2Element;

import java.math.BigInteger;

public class Params {
    public static final int
            W_FIXEDBASE = 5,
            V_FIXEDBASE = 5,
            D_FIXEDBASE = 54,
            E_FIXEDBASE = 10;

    public static final int L_FIXEDBASE = D_FIXEDBASE * W_FIXEDBASE;
    public static final int HEX_RADIX = 16;

    public static final int NWORDS_ORDER = 8;

    public static final int PRE_COMPUTE_TABLE_LENGTH = 8;

    public static final BigInteger PRIME_1271
            = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);
    public static final BigInteger MASK_127 = PRIME_1271;  // Same as 2^127 - 1

    public static final BigInteger W_VARBASE = BigInteger.valueOf(5);
    public static final BigInteger NPOINTS_VARBASE
            = BigInteger.ONE.shiftLeft(W_VARBASE.subtract(BigInteger.TWO).intValue());
    public static final int VPOINTS_FIXEDBASE = (1 << (W_FIXEDBASE-1));
    public static final int NBITS_ORDER_PLUS_ONE = 247;
    public static final int T_VARBASE = (NBITS_ORDER_PLUS_ONE+W_VARBASE.intValueExact()-2)/(W_VARBASE.intValueExact()-1);

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

    public static F2Element PARAMETER_d = new F2Element(
            new BigInteger("00000000000000E40000000000000142", HEX_RADIX),
            new BigInteger("5E472F846657E0FCB3821488F1FC0C8D", HEX_RADIX)
    );

    public static F2Element GENERATOR_x = new F2Element(
            new BigInteger("1A3472237C2FB305286592AD7B3833AA", HEX_RADIX),
            new BigInteger("1E1F553F2878AA9C96869FB360AC77F6", HEX_RADIX)
    );
    public static F2Element GENERATOR_y = new F2Element(
            new BigInteger("0E3FEE9BA120785AB924A2462BCBB287", HEX_RADIX),
            new BigInteger("6E1C4AF8630E024249A7C344844C8B5C", HEX_RADIX)
    );

    public static final BigInteger C1 = new BigInteger("72482C5251A4559C", HEX_RADIX);
    public static final BigInteger C2 = new BigInteger("59F95B0ADD276F6C", HEX_RADIX);
    public static final BigInteger C3 = new BigInteger("7DD2D17C4625FA78", HEX_RADIX);
    public static final BigInteger C4 = new BigInteger("6BC57DEF56CE8877", HEX_RADIX);

    public static final BigInteger B11 = new BigInteger("0906FF27E0A0A196", HEX_RADIX);
    public static final BigInteger B12 = new BigInteger("1363E862C22A2DA0", HEX_RADIX);
    public static final BigInteger B13 = new BigInteger("07426031ECC8030F", HEX_RADIX);
    public static final BigInteger B14 = new BigInteger("084F739986B9E651", HEX_RADIX);

    public static final BigInteger B21 = new BigInteger("1D495BEA84FCC2D4", HEX_RADIX);
    public static final BigInteger B24 = new BigInteger("25DBC5BC8DD167D0", HEX_RADIX);

    public static final BigInteger B31 = new BigInteger("17ABAD1D231F0302", HEX_RADIX);
    public static final BigInteger B32 = new BigInteger("02C4211AE388DA51", HEX_RADIX);
    public static final BigInteger B33 = new BigInteger("2E4D21C98927C49F", HEX_RADIX);
    public static final BigInteger B34 = new BigInteger("0A9E6F44C02ECD97", HEX_RADIX);

    public static final BigInteger B41 = new BigInteger("136E340A9108C83F", HEX_RADIX);
    public static final BigInteger B42 = new BigInteger("3122DF2DC3E0FF32", HEX_RADIX);
    public static final BigInteger B43 = new BigInteger("068A49F02AA8A9B5", HEX_RADIX);
    public static final BigInteger B44 = new BigInteger("18D5087896DE0AEA", HEX_RADIX);

    // Precomputed integers for fast-Babai rounding
    public static final BigInteger ELL1 = new BigInteger("0700000000000000FC5BB5C5EA2BE5DFF75682ACE6A6BD66259686E09D1A7D4F", HEX_RADIX);
    public static final BigInteger ELL2 = new BigInteger("03000000000000008FD4B04CAA6C0F8A2BD235580F468D8DD1BA1D84DD627AFB", HEX_RADIX);
    public static final BigInteger ELL3 = new BigInteger("0000000000000000D038BF8D0BFFBAF6C42BD6C965DCA9029B291A33678C203C", HEX_RADIX);
    public static final BigInteger ELL4 = new BigInteger("03000000000000001B073877A22D841081CBDC3714983D8212E5666B77E7FDC0", HEX_RADIX);

    public static final BigInteger MASK_ALL_ONES =
            new BigInteger("FFFFFFFFFFFFFFFF", HEX_RADIX); // 64-bit all ones mask
}
