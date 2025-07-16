package crypto.primitives;

import utils.BigIntegerUtils;
import utils.ByteArrayUtils;
import constants.Key;
import exceptions.EncryptionException;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static utils.ByteArrayReverseMode.REMOVE_LEADING_ZERO;


/**
 * Cryptographic hash function utilities for FourQ operations.
 * <p>
 * This class provides SHA-512 hashing functionality with support for
 * byte order reversal, which is needed for proper endianness handling
 * in the FourQ implementation. The hash functions are used in key
 * generation, nonce derivation, and challenge computation in signatures.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class SHA512 implements HashFunction {
    /**
     * Computes SHA-512 hash of a BigInteger input with optional byte reversal.
     * 
     * @param input the BigInteger to hash
     * @param reverse whether to reverse the byte order of the result
     * @return the SHA-512 hash as a byte array
     * @throws EncryptionException if the hash algorithm is not available
     */
    @Override
    public byte[] computeHash(@NotNull BigInteger input, boolean reverse) throws EncryptionException {
        return computeHash(BigIntegerUtils.bigIntegerToByte(input, Key.KEY_SIZE,false), reverse);
    }

    /**
     * Computes SHA-512 hash of byte array input with optional byte reversal.
     * <p>
     * This is the core hash function used throughout the FourQ implementation.
     * The reverse option handles endianness requirements for different contexts.
     * 
     * @param bytes the byte array to hash
     * @param reverse whether to reverse the byte order of the result
     * @return the SHA-512 hash as a byte array
     * @throws EncryptionException if the hash algorithm is not available
     */
    @Override
    public byte[] computeHash(byte[] bytes, boolean reverse) throws EncryptionException {
        final String ENCRYPTION_STANDARD = "SHA-512";
        try {
            MessageDigest digest = MessageDigest.getInstance(ENCRYPTION_STANDARD);
            if (reverse) {
                return ByteArrayUtils.reverseByteArray(digest.digest(bytes), REMOVE_LEADING_ZERO);
            }
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(String.format(
                    "No such encryption algorithm: %s\n",
                    ENCRYPTION_STANDARD
            ));
        }
    }
}