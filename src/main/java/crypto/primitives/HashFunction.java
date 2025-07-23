package crypto.primitives;

import java.math.BigInteger;

import org.jetbrains.annotations.NotNull;

import exceptions.EncryptionException;

/**
 * Defines a cryptographic hash function interface for the FourQ library.
 * <p>
 * This interface provides a contract for hash function implementations used in
 * cryptographic operations, particularly in the SchnorrQ class.
 * Implementations should provide secure hash computation with support for different
 * input types and byte order configurations.
 * <p>
 * Hash functions implementing this interface are expected to be cryptographically
 * secure and suitable for use in digital signatures, key derivation, and other
 * security-critical operations.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 * @see crypto.primitives.SHA512
 * @see crypto.primitives.Kangaroo12
 */
public interface HashFunction {
    
    /**
     * Computes the cryptographic hash of a BigInteger input.
     * <p>
     * This method converts the BigInteger to its byte representation and
     * computes its hash. The byte order of the input can be controlled
     * through the reverse parameter.
     * 
     * @param input the BigInteger value to hash; must not be null
     * @param reverse if true, reverses the byte order of the input before hashing;
     *                if false, uses the natural byte order
     * @return the computed hash as a byte array
     * @throws EncryptionException if the hash computation fails due to
     *                           invalid input or internal cryptographic errors
     * @throws IllegalArgumentException if input is null
     */
    byte[] computeHash(@NotNull BigInteger input, boolean reverse) throws EncryptionException;

    /**
     * Computes the cryptographic hash of a byte array input.
     * <p>
     * This method directly hashes the provided byte array. The byte order
     * of the input can be controlled through the reverse parameter.
     * 
     * @param bytes the byte array to hash; must not be null
     * @param reverse if true, reverses the byte order of the input before hashing;
     *                if false, uses the input bytes as-is
     * @return the computed hash as a byte array
     * @throws EncryptionException if the hash computation fails due to
     *                           invalid input or internal cryptographic errors
     * @throws IllegalArgumentException if bytes is null
     */
    byte[] computeHash(byte @NotNull [] bytes, boolean reverse) throws EncryptionException;
}
