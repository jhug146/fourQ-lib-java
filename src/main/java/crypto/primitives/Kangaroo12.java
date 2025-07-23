package crypto.primitives;

import java.math.BigInteger;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.bouncycastle.crypto.digests.*;

import utils.BigIntegerUtils;
import utils.ByteArrayUtils;
import exceptions.EncryptionException;
import constants.Key;

import static utils.ByteArrayReverseMode.REMOVE_LEADING_ZERO;

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
 * @since 1.0
 */
public class Kangaroo12 implements HashFunction {
    private static final int DEFAULT_DIGEST_LENGTH = 32;
    private static final byte[] FOURQ_PERSONALIZATION = "FourQ-Kangaroo12".getBytes();
    
    /**
     * Computes Kangaroo-12 hash of a BigInteger input with optional byte reversal.
     * 
     * @param input the BigInteger to hash
     * @param reverse whether to reverse the byte order of the result
     * @return the Kangaroo-12 hash as a byte array
     * @throws EncryptionException if the hash computation fails
     */
    @Override
    public byte[] computeHash(@NotNull BigInteger input, boolean reverse) throws EncryptionException {
        return computeHash(BigIntegerUtils.bigIntegerToByte(input, Key.KEY_SIZE, false), reverse);
    }

    @Override
    public byte[] computeHash(byte @NotNull [] bytes, boolean reverse) throws EncryptionException {
        try {
            // Create Kangaroo-12 digest with FourQ personalization
            Kangaroo.KangarooTwelve digest = new Kangaroo.KangarooTwelve();
            
            // Initialize with personalization parameters
            Kangaroo.KangarooParameters params = new Kangaroo.KangarooParameters.Builder()
                .setPersonalisation(FOURQ_PERSONALIZATION) // TODO this is optional
                .build();
            digest.init(params);
            
            // Update with input data
            digest.update(bytes, 0, bytes.length);
            
            // Finalize and get the hash
            byte[] result = new byte[DEFAULT_DIGEST_LENGTH];
            digest.doFinal(result, 0);
            
            // Apply byte reversal if requested
            if (reverse) return ByteArrayUtils.reverseByteArray(result, Optional.of(REMOVE_LEADING_ZERO));
            
            return result;
        } catch (Exception e) {
            throw new EncryptionException(String.format(
                "Failed to compute Kangaroo-12 hash: %s", 
                e.getMessage()
            ));
        }
    }
}