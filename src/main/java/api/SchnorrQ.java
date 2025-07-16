package api;

import java.math.BigInteger;
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

import crypto.primitives.HashFunction;
import crypto.core.ECC;
import crypto.primitives.SHA512;
import utils.BigIntegerUtils;
import utils.ByteArrayUtils;
import utils.CryptoUtils;
import exceptions.EncryptionException;
import exceptions.InvalidArgumentException;
import field.operations.FP;
import types.data.Pair;
import types.point.FieldPoint;
import constants.Key;

import static exceptions.ValidationErrors.*;
import static utils.ByteArrayReverseMode.*;
import static utils.ByteArrayUtils.addLeadingZeros;
import static utils.ByteArrayUtils.reverseByteArray;


/**
 * Implementation of api.SchnorrQ digital signature scheme over the FourQ elliptic curve.
 * <p>
 * FourQ is a high-security, high-performance elliptic curve that targets the 128-bit 
 * security level. It operates over the finite field GF((2^127-1)^2) and uses a 
 * four-dimensional Gallant-Lambert-Vanstone decomposition for efficient scalar 
 * multiplications. This implementation provides:
 * <p>
 * - Public key generation from private keys
 * - Complete key pair generation  
 * - Message signing using api.SchnorrQ scheme
 * - Signature verification
 * <p>
 * The api.SchnorrQ signature scheme provides strong security guarantees including
 * existential unforgeability under chosen message attacks (EUF-CMA) in the
 * random oracle model.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class SchnorrQ {
    private static final HashFunction hashFunction = new SHA512();
    /**
     * Generates a public key from the given private key using the FourQ curve.
     * <p>
     * The key generation process involves:
     * 1. Computing SHA-512 hash of the private key with byte reversal
     * 2. Performing scalar multiplication with the curve generator point
     * 3. Encoding the resulting point into compressed format
     * 
     * @param secretKey the private key as a BigInteger (must be non-null)
     * @return the corresponding public key encoded as a BigInteger
     * @throws EncryptionException if the cryptographic operations fail
     * @throws IllegalArgumentException if secretKey is null
     */
    @NotNull
    public static BigInteger schnorrQKeyGeneration(@NotNull BigInteger secretKey) throws EncryptionException {
        BigInteger hash = new BigInteger(1, hashFunction.computeHash(secretKey, true));
        final FieldPoint point = ECC.eccMulFixed(hash);
        return CryptoUtils.encode(point);
    }

    /**
     * Generates a complete public-private key pair using cryptographically secure randomness.
     * <p>
     * This method creates a fresh private key using a secure random number generator
     * and derives the corresponding public key. The private key is generated with
     * sufficient entropy for 128-bit security.
     * 
     * @return a Pair containing (privateKey, publicKey) as BigInteger values
     * @throws EncryptionException if key generation fails due to cryptographic errors
     */
    @NotNull
    public static Pair<BigInteger, BigInteger> schnorrQFullKeyGeneration() throws EncryptionException {
        final BigInteger secretKey = CryptoUtils.randomBytes(Key.KEY_SIZE);
        final BigInteger publicKey = schnorrQKeyGeneration(secretKey);
        return new Pair<>(secretKey, publicKey);
    }

    /**
     * Creates an api.SchnorrQ digital signature for the given message.
     * <p>
     * The signing process follows the api.SchnorrQ protocol:
     * 1. Derive a deterministic nonce from the secret key
     * 2. Compute the commitment R = r*G where r is the nonce
     * 3. Compute the challenge hash H(R || publicKey || message)
     * 4. Compute the response s = r - H * secretKey (mod order)
     * 5. Return signature as (R || s)
     * 
     * @param secretKey the signer's private key (must be non-null)
     * @param publicKey the signer's public key for verification (must be non-null)
     * @param message the message bytes to be signed
     * @return the signature as a 64-byte BigInteger (32 bytes R + 32 bytes s)
     * @throws EncryptionException if signing fails due to cryptographic errors
     * @throws IllegalArgumentException if secretKey or publicKey is null
     */
    @NotNull
    public static BigInteger schnorrQSign(
            @NotNull BigInteger secretKey,
            @NotNull BigInteger publicKey,
            byte[] message
    ) throws EncryptionException {
        final byte[] kHash = hashFunction.computeHash(secretKey, false);
        byte[] bytes = new byte[message.length + 2 * Key.KEY_SIZE];
        // Use second half of kHash as nonce seed for deterministic signing
        System.arraycopy(
                kHash,
                Key.KEY_SIZE,
                bytes,
                Key.KEY_SIZE,
                Key.KEY_SIZE
        );
        System.arraycopy(
                message,
                0,
                bytes,
                Key.KEY_SIZE * 2,
                message.length
        );

        // Compute nonce r = H(nonce_seed || message)
        BigInteger rHash = new BigInteger(
                1,
                hashFunction.computeHash(
                        Arrays.copyOfRange(bytes, Key.KEY_SIZE, bytes.length),
                        true
                )
        );
        final FieldPoint rPoint = ECC.eccMulFixed(rHash);
        final BigInteger sigStart = CryptoUtils.encode(rPoint);

        // Prepare challenge hash input: R || publicKey || message
        byte[] publicKeyBytes = BigIntegerUtils.bigIntegerToByte(publicKey, Key.KEY_SIZE, false);
        System.arraycopy(
                BigIntegerUtils.bigIntegerToByte(sigStart, Key.KEY_SIZE, false),
                0,
                bytes,
                0,
                Key.KEY_SIZE
        );
        System.arraycopy(
                publicKeyBytes,
                0,
                bytes,
                Key.KEY_SIZE,
                Key.KEY_SIZE
        );

        BigInteger hHash2 = new BigInteger(1, hashFunction.computeHash(bytes, true));
        rHash = FP.moduloOrder(rHash);
        hHash2 = FP.moduloOrder(hHash2);

        // Use Montgomery arithmetic for efficient modular operations
        BigInteger sigEnd = CryptoUtils.toMontgomery(new BigInteger(1, ByteArrayUtils.reverseByteArray(kHash, REMOVE_LEADING_ZERO)));
        hHash2 = CryptoUtils.toMontgomery(hHash2);
        sigEnd = FP.montgomeryMultiplyModOrder(sigEnd, hHash2);
        sigEnd = CryptoUtils.fromMontgomery(sigEnd);
        // Compute s = r - H * secretKey (mod order)
        sigEnd = FP.subtractModOrder(rHash, sigEnd);

        byte[] sigStartBytes = BigIntegerUtils.bigIntegerToByte(sigStart, Key.KEY_SIZE, false);
        byte[] sigEndBytes   = reverseByteArray(BigIntegerUtils.bigIntegerToByte(sigEnd, Key.KEY_SIZE, false), KEEP_LEADING_ZERO);
        return new BigInteger(1, ByteArrayUtils.concat(sigStartBytes, sigEndBytes));
    }

    /**
     * Verifies an api.SchnorrQ digital signature against a message and public key.
     * <p>
     * The verification process follows the api.SchnorrQ protocol:
     * 1. Parse signature into commitment R and response s
     * 2. Compute challenge hash H(R || publicKey || message)
     * 3. Verify equation: s*G + H*publicKey = R
     * <p>
     * This method includes several security checks:
     * - Validates that specific bits are properly set to zero
     * - Ensures signature is within valid range
     * - Verifies the public key lies on the curve
     * 
     * @param publicKey the signer's public key for verification (must be non-null)
     * @param signature the signature to verify as a 64-byte BigInteger (must be non-null)
     * @param message the original message bytes that was signed
     * @return true if the signature is valid, false otherwise
     * @throws EncryptionException if verification fails due to cryptographic errors
     * @throws InvalidArgumentException if inputs fail validation checks
     * @throws IllegalArgumentException if publicKey or signature is null
     */
    public static boolean schnorrQVerify(@NotNull BigInteger publicKey, @NotNull BigInteger signature, byte[] message) throws EncryptionException {
        // Security check: ensure specific bit is zero for both inputs
        if (publicKey.testBit(Key.PUB_TEST_BIT)) { publicKeyError(); }
        if (signature.testBit(Key.SIG_TEST_BIT)) { signatureError(); }
        // Validate signature is within acceptable range
        if (isSignatureSizeTooLarge(signature)) { signatureSizeError(); }

        final byte[] bytes = new byte[message.length + 2 * Key.KEY_SIZE];
        System.arraycopy(BigIntegerUtils.bigIntegerToByte(signature, Key.KEY_SIZE*2, false), 0, bytes, 0, Key.KEY_SIZE);
        System.arraycopy(BigIntegerUtils.bigIntegerToByte(publicKey, Key.KEY_SIZE, false), 0, bytes, Key.KEY_SIZE, Key.KEY_SIZE);
        System.arraycopy(message, 0, bytes, 2 * Key.KEY_SIZE, message.length);

        final BigInteger sig32 = signature.mod(Key.POW_256);
        final byte[] sig32Array = addLeadingZeros(sig32.toByteArray(), Key.KEY_SIZE + 1);
        // Compute s*G + H*publicKey using double scalar multiplication

        FieldPoint affPoint = ECC.eccMulDouble(
                new BigInteger(1, ByteArrayUtils.reverseByteArray(sig32Array, REMOVE_TRAILING_ZERO)),
                CryptoUtils.decode(publicKey),       // Implicitly checks that public key lies on the curve
                new BigInteger(1, hashFunction.computeHash(bytes, true))
        );

        final BigInteger encoded = CryptoUtils.encode(affPoint);
        // Verify that computed point equals the commitment R from signature
        return encoded.equals(signature.divide(Key.POW_256));
    }
}
