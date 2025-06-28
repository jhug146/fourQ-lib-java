import java.math.BigInteger;

public class FourQConstants {
    private static final int HEX_RADIX = 16;

    static final int RADIX = 32; //TODO confirm this and below
    static final int RADIX64 = 64; //TODO confirm this

    static final BigInteger prime1271
            = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);

    static final BigInteger MONTGOMERY_R_PRIME = new BigInteger(
            "0xC81DB8795FF3D621173EA5AAEA6B387D3D01B7C72136F61C0006A5F16AC8F9D3",
            HEX_RADIX
    );
    static final BigInteger CURVE_ORDER = new BigInteger(
            "0x2FB2540EC7768CE7DFBD004DFE0F7999F05397829CBC14E50029CBC14E5E0A72",
            HEX_RADIX
    );
    static final BigInteger ONE = BigInteger.ONE;
}
