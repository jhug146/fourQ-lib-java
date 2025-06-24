import types.Key;

import java.math.BigInteger;

public class Constants {
    private static final int HEX_RADIX = 16;
    private static final BigInteger MONTGOMERY_R_VAL = new BigInteger(
            "0xC81DB8795FF3D621173EA5AAEA6B387D3D01B7C72136F61C0006A5F16AC8F9D3",
            HEX_RADIX
    );
    private static final BigInteger CURVE_ORDER_VAL = new BigInteger(
            "0x2FB2540EC7768CE7DFBD004DFE0F7999F05397829CBC14E50029CBC14E5E0A72",
            HEX_RADIX
    );

    static final Key MONTGOMERY_R_PRIME = new Key(MONTGOMERY_R_VAL, 256);
    static final Key CURVE_ORDER = new Key(CURVE_ORDER_VAL, 256);
    static final Key ONE = new Key(1, 256);
}
