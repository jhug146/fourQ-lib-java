package fourqj.constants;

import java.math.BigInteger;
import fourqj.types.data.F2Element;

/**
 * Mathematical fourqj.constants and parameters for the FourQ elliptic curve cryptographic implementation.
 * <p>
 * This class contains all the cryptographic fourqj.constants used throughout the FourQ library including:
 * - Field parameters for the Mersenne prime field GF(2^127-1)
 * - Curve parameters and generator point coordinates
 * - Montgomery arithmetic fourqj.constants for efficient modular operations
 * - Precomputation table parameters for optimized scalar multiplication
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0.0
 */
public final class Params {
    
    // ========== GENERAL CONSTANTS ==========
    
    /** Hexadecimal radix for parsing hex string fourqj.constants */
    public static final int HEX_RADIX = 16;
    
    /** Number of words used for curve order representation */
    public static final int NWORDS_ORDER = 8;
    
    // ========== FIELD PARAMETERS ==========
    
    /** The Mersenne prime p = 2^127 - 1 used as the base field characteristic */
    public static final BigInteger PRIME_1271 = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);
    
    /** Bit mask for extracting lower 127 bits: 2^127 - 1 */
    public static final BigInteger MASK_127 = PRIME_1271;
    
    // ========== SCALAR MULTIPLICATION PARAMETERS ==========
    
    /** Window size for variable-base scalar multiplication */
    public static final BigInteger W_VARBASE = BigInteger.valueOf(5);
    
    /** Number of precomputed points for variable-base multiplication: 2^(W_VARBASE-2) */
    public static final BigInteger N_POINTS_VARBASE = BigInteger.ONE.shiftLeft(W_VARBASE.subtract(BigInteger.valueOf(2)).intValue());
    
    /** Number of bits in curve order plus one for scalar decomposition */
    public static final int N_BITS_ORDER_PLUS_ONE = 247;
    
    /** Number of scalar digits for variable-base multiplication */
    public static final int T_VARBASE = (N_BITS_ORDER_PLUS_ONE + W_VARBASE.intValue() - 2) / (W_VARBASE.intValue() - 1);
    
    // ========== MONTGOMERY ARITHMETIC CONSTANTS ==========
    
    /** Montgomery constant R' for efficient modular reduction */
    public static final BigInteger MONTGOMERY_R_PRIME = new BigInteger("0006A5F16AC8F9D33D01B7C72136F61C173EA5AAEA6B387DC81DB8795FF3D621", HEX_RADIX);
    
    /** Montgomery constant r' for modular inverse operations */
    public static final BigInteger MONTGOMERY_r_PRIME = new BigInteger("F32702FDAFC1C074BCE409ED76B5DB21D75E78B8D1FCDCF3E12FE5F079BC3929", HEX_RADIX);
    
    // ========== CURVE PARAMETERS ==========
    
    /** Order of the FourQ curve's prime subgroup */
    public static final BigInteger CURVE_ORDER = new BigInteger("0029CBC14E5E0A72F05397829CBC14E5DFBD004DFE0F79992FB2540EC7768CE7", HEX_RADIX);
    
    /** Curve parameter d in the twisted Edwards equation: -x^2 + y^2 = 1 + d*x^2*y^2 */
    public static final F2Element PARAMETER_D = new F2Element(
        new BigInteger("00000000000000E40000000000000142", HEX_RADIX),
        new BigInteger("5E472F846657E0FCB3821488F1FC0C8D", HEX_RADIX)
    );
    
    // ========== GENERATOR POINT COORDINATES ==========
    
    /** X-coordinate of the FourQ curve generator point in GF(p^2) */
    public static final F2Element GENERATOR_X = new F2Element(
        new BigInteger("1A3472237C2FB305286592AD7B3833AA", HEX_RADIX),
        new BigInteger("1E1F553F2878AA9C96869FB360AC77F6", HEX_RADIX)
    );
    
    /** Y-coordinate of the FourQ curve generator point in GF(p^2) */
    public static final F2Element GENERATOR_Y = new F2Element(
        new BigInteger("0E3FEE9BA120785AB924A2462BCBB287", HEX_RADIX),
        new BigInteger("6E1C4AF8630E024249A7C344844C8B5C", HEX_RADIX)
    );

    public static final int noOffset = 0;
    public static final int signPositive = 1;

    // Private constructor to prevent instantiation
    private Params() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}