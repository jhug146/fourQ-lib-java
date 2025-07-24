package fourqj.api;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

import fourqj.constants.Params;
import fourqj.crypto.primitives.HashFunction;
import fourqj.crypto.core.ECC;
import fourqj.crypto.primitives.SHA512;
import fourqj.utils.BigIntegerUtils;
import fourqj.utils.ByteArrayUtils;
import fourqj.constants.Key;
import fourqj.utils.CryptoUtils;
import fourqj.exceptions.EncryptionException;
import fourqj.exceptions.InvalidArgumentException;
import fourqj.fieldoperations.FP;
import fourqj.types.data.Pair;
import fourqj.types.point.FieldPoint;

import static fourqj.exceptions.ValidationErrors.*;
import static fourqj.utils.BigIntegerUtils.copyBigIntegerToByteArray;
import static fourqj.utils.ByteArrayReverseMode.*;
import static fourqj.utils.ByteArrayUtils.copyByteArrayToByteArray;
import static fourqj.utils.ByteArrayUtils.reverseByteArray;


/**
 * Implementation of fourqj.api.SchnorrQ digital signature scheme over the FourQ elliptic curve.
 * <p>
 * FourQ is a high-security, high-performance elliptic curve that targets the 128-bit 
 * security level. It operates over the finite field GF((2^127-1)^2) and uses a 
 * four-dimensional Gallant-Lambert-Vanstone decomposition for efficient scalar 
 * multiplications. This implementation provides:
 * <p>
 * - Public key generation from private keys
 * - Complete key pair generation  
 * - Message signing using fourqj.api.SchnorrQ scheme
 * - Signature verification
 * <p>
 * The fourqj.api.SchnorrQ signature scheme provides strong security guarantees including
 * existential unforgeability under chosen message attacks (EUF-CMA) in the
 * random oracle model.
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0.0
 */
public class SchnorrQ {
    private final HashFunction hashFunction;

    public SchnorrQ() {
        hashFunction = new SHA512();
    }

    public SchnorrQ(HashFunction hash) {
        this.hashFunction = hash;
    }

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
    public BigInteger schnorrQKeyGeneration(BigInteger secretKey) throws EncryptionException {
        if (secretKey == null) { throw new InvalidArgumentException("Secret key cannot be null."); }
        BigInteger hash = new BigInteger(Params.signPositive, hashFunction.computeHash(secretKey, true));
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
    public Pair<BigInteger, BigInteger> schnorrQFullKeyGeneration() throws EncryptionException {
        final BigInteger secretKey = CryptoUtils.randomBytes(Key.KEY_SIZE);
        final BigInteger publicKey = schnorrQKeyGeneration(secretKey);
        return new Pair<>(secretKey, publicKey);
    }

    /**
     * Creates an fourqj.api.SchnorrQ digital signature for the given message.
     * <p>
     * The signing process follows the fourqj.api.SchnorrQ protocol:
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
    public BigInteger schnorrQSign(
            BigInteger secretKey,
            BigInteger publicKey,
            byte[] message
    ) throws EncryptionException {
        if (secretKey == null) { throw new InvalidArgumentException("Secret key cannot be null."); }
        if (publicKey == null) { throw new InvalidArgumentException("Public key cannot be null."); }
        final byte[] kHash = hashFunction.computeHash(secretKey, false);
        final byte[] bytes = SchnorrHelper.createBuffer(message);

        // Use second half of kHash as nonce seed for, deterministic signing
        copyByteArrayToByteArray(kHash, Key.KEY_SIZE, bytes, Key.KEY_SIZE, Key.KEY_SIZE);
        copyByteArrayToByteArray(message, Params.noOffset, bytes, Key.SIGNATURE_SIZE, message.length);

        // Compute nonce r = H(nonce_seed || message)
        final BigInteger rHash = new BigInteger(Params.signPositive, hashFunction.computeHash(Arrays.copyOfRange(bytes, Key.KEY_SIZE, bytes.length), true));
        final BigInteger sigStart = CryptoUtils.encode(ECC.eccMulFixed(rHash));

        // Prepare challenge hash input: R || publicKey || message
        copyBigIntegerToByteArray(sigStart, Key.KEY_SIZE, bytes, Params.noOffset);
        copyBigIntegerToByteArray(publicKey, Key.KEY_SIZE, bytes, Key.KEY_SIZE);

        final BigInteger hHash2 = FP.moduloOrder(new BigInteger(Params.signPositive, hashFunction.computeHash(bytes, true)));

        // Use Montgomery arithmetic for efficient modular operations
        // Sequentially builds up the sigEnd BigInteger.
        final BigInteger sigEnd = BigIntegerUtils.buildBigInteger(
            new BigInteger(Params.signPositive, ByteArrayUtils.reverseByteArray(kHash, Optional.of(REMOVE_LEADING_ZERO))),
            CryptoUtils::toMontgomery,
            x -> FP.montgomeryMultiplyModOrder(x, CryptoUtils.toMontgomery(hHash2)),
            CryptoUtils::fromMontgomery,
            x -> FP.subtractModOrder(FP.moduloOrder(rHash), x)
        );

        return new BigInteger(Params.signPositive, ByteArrayUtils.concatenate(
            BigIntegerUtils.bigIntegerToByte(sigStart, Key.KEY_SIZE, false),
            reverseByteArray(BigIntegerUtils.bigIntegerToByte(sigEnd, Key.KEY_SIZE, false), Optional.empty())
        ));
    }

    /**
     * Verifies an fourqj.api.SchnorrQ digital signature against a message and public key.
     * <p>
     * The verification process follows the fourqj.api.SchnorrQ protocol:
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
     * @throws fourqj.exceptions.ValidationException if verification fails due to cryptographic errors
     * @throws InvalidArgumentException if inputs fail validation checks
     * @throws IllegalArgumentException if publicKey or signature is null
     */
    public boolean schnorrQVerify(
            BigInteger publicKey,
            BigInteger signature,
            byte[] message
    ) throws EncryptionException {
        SchnorrHelper.validateVerifyInputs(publicKey, signature);

        final byte[] bytes = SchnorrHelper.createBuffer(message);
        copyBigIntegerToByteArray(signature, Key.SIGNATURE_SIZE, bytes, Params.noOffset);
        copyBigIntegerToByteArray(publicKey, Key.KEY_SIZE, bytes, Key.KEY_SIZE);
        copyByteArrayToByteArray(message, Params.noOffset, bytes, Key.SIGNATURE_SIZE, message.length);

        // Compute s*G + H*publicKey using double scalar multiplication
        final FieldPoint affPoint = ECC.eccMulDouble(
                CryptoUtils.extractSignatureTopBytesReverse(signature),
                CryptoUtils.decode(publicKey),       // Implicitly checks that public key lies on the curve
                new BigInteger(Params.signPositive, hashFunction.computeHash(bytes, true))
        );

        // Verify that computed point equals the commitment R from signature
        return CryptoUtils.encode(affPoint).equals(signature.divide(Key.POW_256));
    }

    private interface SchnorrHelper {
        static void validateVerifyInputs(BigInteger publicKey, BigInteger signature) throws InvalidArgumentException {
            // Security check: ensure specific bit is zero for both inputs
            if (signature == null) throw new InvalidArgumentException("Signature cannot be null.");
            else if (publicKey == null) throw new InvalidArgumentException("Public key cannot be null.");
            else if (publicKey.testBit(Key.PUB_TEST_BIT)) publicKeyError();
            else if (signature.testBit(Key.SIG_TEST_BIT)) signatureError();
            else if (isSignatureSizeTooLarge(signature)) signatureSizeError();
        }

        static byte[] createBuffer(byte[] message) {
            return new byte[message.length + Key.SIGNATURE_SIZE];
        }
    }
}
