package constants;

import types.data.F2Element;

import java.math.BigInteger;
import java.util.Arrays;

public class Params {
    private static final int HEX_RADIX = 16;

    public static final int NWORDS_ORDER = 8;

    public static final int PRE_COMPUTE_TABLE_LENGTH = 8;

    public static final F2Element F2_ZERO = new F2Element(BigInteger.ZERO, BigInteger.ZERO);

    public static final BigInteger PRIME_1271
            = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);
    public static final BigInteger MASK_127 = PRIME_1271;  // Same as 2^127 - 1

    public static final BigInteger W_VARBASE = BigInteger.valueOf(5);
    public static final int W_FIXEDBASE = 5;
    public static final BigInteger NPOINTS_VARBASE
            = BigInteger.ONE.shiftLeft(W_VARBASE.subtract(BigInteger.TWO).intValue());
    public static final int VPOINTS_FIXEDBASE = (1 << (W_FIXEDBASE-1));
    public static final int NBITS_ORDER_PLUS_ONE = 246+1;
    public static final int T_VARBASE = ((NBITS_ORDER_PLUS_ONE+W_VARBASE.intValueExact()-2)/(W_VARBASE.intValueExact()-1));

    public static final BigInteger MONTGOMERY_R_PRIME = new BigInteger(
            "C81DB8795FF3D621173EA5AAEA6B387D3D01B7C72136F61C0006A5F16AC8F9D3",
            HEX_RADIX
    );

    public static final BigInteger CURVE_ORDER = new BigInteger(
            "2FB2540EC7768CE7DFBD004DFE0F7999F05397829CBC14E50029CBC14E5E0A72",
            HEX_RADIX
    );

    public static F2Element PARAMETER_d = new F2Element(
            new BigInteger("00000000000000E40000000000000142", HEX_RADIX),
            new BigInteger("5E472F846657E0FCB3821488F1FC0C8D", HEX_RADIX)
    );

    /*
    public static F2Element PARAMETER_d = convertLongArrayToF2Element(new long[]{ 0x0000000000000142L, 0x00000000000000E4L, 0xB3821488F1FC0C8DL, 0x5E472F846657E0FCL });

     */
    public static F2Element GENERATOR_x = convertLongArrayToF2Element(new long[]{ 0x286592AD7B3833AAL, 0x1A3472237C2FB305L, 0x96869FB360AC77F6L, 0x1E1F553F2878AA9CL });
    public static F2Element GENERATOR_y = convertLongArrayToF2Element(new long[]{ 0xB924A2462BCBB287L, 0x0E3FEE9BA120785AL, 0x49A7C344844C8B5CL, 0x6E1C4AF8630E0242L });
    public static F2Element convertLongArrayToF2Element(long[] toConvert){
        BigInteger realLeastSigDigit1 = BigInteger.valueOf(toConvert[0]);
        BigInteger realLeastSigDigit2 = BigInteger.valueOf(toConvert[1]);
        BigInteger combinedReal = realLeastSigDigit2.shiftLeft(64).or(realLeastSigDigit1);
        BigInteger fieldElementReal = combinedReal.mod(PRIME_1271);

        BigInteger imLeastSigDigit1 = BigInteger.valueOf(toConvert[2]);
        BigInteger imLeastSigDigit2 = BigInteger.valueOf(toConvert[3]);
        BigInteger combinedIm = imLeastSigDigit2.shiftLeft(64).or(imLeastSigDigit1);
        BigInteger fieldElementIm = combinedIm.mod(PRIME_1271);

        return new F2Element(
                fieldElementReal,
                fieldElementIm
        );
    }

    /**
     * Convert BigInteger to F2Element by splitting into real and imaginary parts
     * Assumes the BigInteger contains two 127-bit field elements packed together
     */
    public static F2Element convertBigIntegerToF2Element(BigInteger toConvert) {
        if (toConvert == null) {
            return new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        }

        // Extract real part (lower 127 bits)
        BigInteger realPart = toConvert.and(MASK_127);
        BigInteger fieldElementReal = realPart.mod(PRIME_1271);

        // Extract imaginary part (upper bits, shifted down)
        BigInteger imagPart = toConvert.shiftRight(127).and(MASK_127);
        BigInteger fieldElementIm = imagPart.mod(PRIME_1271);

        return new F2Element(fieldElementReal, fieldElementIm);
    }


    // Fixed integer constants for the decomposition
    // Close "offset" vector
    public static final BigInteger C1 = new BigInteger("72482C5251A4559C", HEX_RADIX);
    public static final BigInteger C2 = new BigInteger("59F95B0ADD276F6C", HEX_RADIX);
    public static final BigInteger C3 = new BigInteger("7DD2D17C4625FA78", HEX_RADIX);
    public static final BigInteger C4 = new BigInteger("6BC57DEF56CE8877", HEX_RADIX);

    // Optimal basis vectors
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
    // Note: C arrays are little-endian, so we reverse the order for BigInteger
    public static final BigInteger ELL1 = new BigInteger("07FC5BB5C5EA2BE5DFF75682ACE6A6BD66259686E09D1A7D4F", HEX_RADIX);
    public static final BigInteger ELL2 = new BigInteger("038FD4B04CAA6C0F8A2BD235580F468D8DD1BA1D84DD627AFB", HEX_RADIX);
    public static final BigInteger ELL3 = new BigInteger("D038BF8D0BFFBAF6C42BD6C965DCA9029B291A33678C203C", HEX_RADIX);
    public static final BigInteger ELL4 = new BigInteger("031B073877A22D841081CBDC3714983D8212E5666B77E7FDC0", HEX_RADIX);

    public static final BigInteger MASK_ALL_ONES =
            new BigInteger("FFFFFFFFFFFFFFFF", HEX_RADIX); // 64-bit all ones mask
}
