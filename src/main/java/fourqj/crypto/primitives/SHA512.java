package fourqj.crypto.primitives;

import java.security.MessageDigest;

import fourqj.exceptions.EncryptionException;

import static fourqj.utils.ExceptionUtils.tryOrThrow;


/**
 * Cryptographic hash function utilities for FourQ operations.
 * <p>
 * This class provides SHA-512 hashing functionality with support for
 * byte order reversal, which is needed for proper endianness handling
 * in the FourQ implementation. The hash functions are used in key
 * generation, nonce derivation, and challenge computation in signatures.
 *
 * @author Naman Malhotra, James Hughff
 * @since 1.0.0
 */
public class SHA512 implements HashFunction {

    final private static String ENCRYPTION_STANDARD = "SHA-512";

    /**
     * Computes SHA-512 hash of byte array input with optional byte reversal.
     * <p>
     * This is the core hash function used throughout the FourQ implementation.
     * The reverse option handles endianness requirements for different contexts.
     * Produces a hash of {@link #HASH_OUTPUT_SIZE} bytes (64 bytes).
     *
     * @param bytes   the byte array to hash
     * @param reverse whether to reverse the byte order of the result
     * @return the SHA-512 hash as a byte array of length {@link #HASH_OUTPUT_SIZE}
     * @throws EncryptionException if the hash algorithm is not available
     */
    @Override
    public byte[] computeHash(byte[] bytes, boolean reverse) throws EncryptionException {
        return tryOrThrow(() -> {
                MessageDigest digest = MessageDigest.getInstance(ENCRYPTION_STANDARD);
                return digestOrReverseDigest(reverse, digest.digest(bytes));
            }, new EncryptionException(String.format("No such encryption algorithm: %s", ENCRYPTION_STANDARD))
        );
    }
}