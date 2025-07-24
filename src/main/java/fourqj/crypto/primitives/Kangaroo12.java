package fourqj.crypto.primitives;

import org.bouncycastle.crypto.digests.*;

/**
 * Kangaroo-12 hash function implementation for FourQ operations.
 * <p>
 * This class provides Kangaroo-12 hashing functionality with support for
 * byte order reversal, which is needed for proper endianness handling
 * in the FourQ implementation. Kangaroo-12 is a high-performance cryptographic
 * hash function that provides excellent security and speed characteristics.
 * <p>
 * The hash functions are used in key generation, nonce derivation, and 
 * challenge computation in signatures as an alternative to SHA-512.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0.0
 */
public class Kangaroo12 implements HashFunction {
    final private static int offset = 0;

    @Override
    public byte[] computeHash(byte[] bytes, boolean reverse) {
        byte[] result = new byte[HASH_OUTPUT_SIZE];
        Kangaroo.KangarooTwelve digest = new Kangaroo.KangarooTwelve();
        digest.update(bytes, offset, bytes.length);
        digest.doFinal(result, offset, HASH_OUTPUT_SIZE);
        return digestOrReverseDigest(reverse, result);
    }
}