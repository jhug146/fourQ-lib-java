import constants.Params;
import exceptions.EncryptionException;
import exceptions.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import types.data.Pair;

import static org.junit.jupiter.api.Assertions.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Random;


public class SchnorrQTests {
    private final int HEX_RADIX = 16;
    private final BigInteger VALID_PRIVATE_KEY = new BigInteger("1489641c237dabe74622c6ee30529aeb85da467414230ff95364d12585e3eb", HEX_RADIX);
    private final BigInteger VALID_PUBLIC_KEY = new BigInteger("8f83d7a1e3b11b9bf1ea1cd3877b53a21aee081ecaf948763df67a5b53b422165c3d0e7d3cd9c41e59d5ce58ae037401214e48544588867f59aa883b74", HEX_RADIX);
    private final BigInteger VALID_SIGNATURE = new BigInteger("987654321098765432109876543210987654321");
    private final byte[] VALID_MESSAGE = "The quick brown fox".getBytes(UTF_8);

    @Test
    void testValidSignature() throws EncryptionException {
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, VALID_MESSAGE);
        assertTrue(result, "Valid signature should return true");
    }

    @Test
    void testInvalidSignature() throws EncryptionException {
        BigInteger tamperedSignature = VALID_SIGNATURE.add(BigInteger.ONE);
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, tamperedSignature, VALID_MESSAGE);
        assertFalse(result, "Invalid signature should return false");
    }

    @Test
    void testInvalidMessage() throws EncryptionException {
        byte[] tamperedMessage = "The quick brown fox jumps".getBytes(UTF_8);
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, tamperedMessage);
        assertFalse(result, "Signature should be invalid for altered message");
    }

    @Test
    void testInvalidPublicKey() throws EncryptionException {
        BigInteger tamperedKey = VALID_PUBLIC_KEY.add(BigInteger.TEN);
        boolean result = SchnorrQ.schnorrQVerify(tamperedKey, VALID_SIGNATURE, VALID_MESSAGE);
        assertFalse(result, "Signature should be invalid for wrong public key");
    }

    @Test
    void testEmptyMessage() throws EncryptionException {
        byte[] emptyMessage = new byte[0];
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, emptyMessage);
        assertFalse(result, "Empty message should not produce valid signature");
    }

    @Test
    void testNullMessage() {
        assertThrows(InvalidArgumentException.class, () -> SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, null), "Null message should throw InvalidArgumentException");
    }

    @Test
    void testNullSignature() {
        assertThrows(InvalidArgumentException.class, () -> SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, null, VALID_MESSAGE), "Null signature should throw InvalidArgumentException");
    }

    @Test
    void testCorruptedSignatureFormat() {
        BigInteger malformedSignature = new BigInteger("-1");  // Possibly invalid
        assertThrows(EncryptionException.class, () -> SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, malformedSignature, VALID_MESSAGE));
    }

    @Test
    void testPublicKeyHighBitSet() {
        BigInteger keyWithMSB = BigInteger.ONE.shiftLeft(127);  // MSB set
        assertThrows(InvalidArgumentException.class, () -> SchnorrQ.schnorrQVerify(keyWithMSB, VALID_SIGNATURE, VALID_MESSAGE));
    }

    @Test
    void testSignatureHighBitSet() {
        BigInteger sigWithMSB = BigInteger.ONE.shiftLeft(127).add(BigInteger.valueOf(12345));
        assertThrows(InvalidArgumentException.class, () -> SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, sigWithMSB, VALID_MESSAGE));
    }

    @Test
    void testSignatureTrailingBytesInvalid() {
        // Simulate a signature with trailing bits set (not valid for 64-byte signature encoding)
        BigInteger badSig = BigInteger.ONE.shiftLeft(504)  // Bit 63 of byte 63
                .add(BigInteger.ONE.shiftLeft(502)); // Bits 6 and 7 of byte 62

        assertThrows(InvalidArgumentException.class, () -> SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, badSig, VALID_MESSAGE));
    }

    @Test
    void testPublicKeyNotOnCurve() {
        BigInteger fakeKey = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16); // clearly invalid

        assertThrows(EncryptionException.class, () -> SchnorrQ.schnorrQVerify(fakeKey, VALID_SIGNATURE, VALID_MESSAGE));
    }

    @Test
    void testRandomInvalidSignatureMismatch() throws Exception {
        BigInteger randomSig = new BigInteger(512, new java.util.Random());
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, randomSig, VALID_MESSAGE);
        assertFalse(result);
    }

    @Test
    void testVeryLargeMessage() throws Exception {
        byte[] largeMessage = new byte[10_000_000];  // 10 MB
        new java.util.Random().nextBytes(largeMessage);
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, largeMessage);
        assertFalse(result);  // Likely fails if signature isn't valid for this message
    }


    private static BigInteger getValidSecretKey() {
        return new BigInteger("11223344556677889900aabbccddeeff00112233445566778899aabbccddeeff", 16)
                .mod(Params.CURVE_ORDER);
    }

    private static byte[] getRandomMessage() {
        byte[] msg = new byte[32];
        new Random().nextBytes(msg);
        return msg;
    }

    @Test
    void testValidSignatureRoundTrip() throws Exception {
        BigInteger sk = getValidSecretKey();
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);
        byte[] msg = "SchnorrQ test message".getBytes(StandardCharsets.UTF_8);

        BigInteger sig = SchnorrQ.schnorrQSign(sk, pk, msg);
        assertTrue(SchnorrQ.schnorrQVerify(pk, sig, msg));
    }

    @Test
    void testSignatureChangesWithMessage() throws Exception {
        BigInteger sk = getValidSecretKey();
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);

        byte[] msg1 = "Message1".getBytes();
        byte[] msg2 = "Message2".getBytes();

        BigInteger sig1 = SchnorrQ.schnorrQSign(sk, pk, msg1);
        BigInteger sig2 = SchnorrQ.schnorrQSign(sk, pk, msg2);

        assertNotEquals(sig1, sig2, "Signatures for different messages should not match");
    }

    @Test
    void testTamperedMessageFailsVerification() throws Exception {
        BigInteger sk = getValidSecretKey();
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);
        byte[] msg = "Original".getBytes();

        BigInteger sig = SchnorrQ.schnorrQSign(sk, pk, msg);

        byte[] tampered = "OriginalX".getBytes();  // Tampered
        assertFalse(SchnorrQ.schnorrQVerify(pk, sig, tampered));
    }

    @Test
    void testNullSecretKeyThrows() {
        BigInteger pk = BigInteger.ONE;
        byte[] msg = "msg".getBytes();
        assertThrows(EncryptionException.class, () ->
                SchnorrQ.schnorrQSign(null, pk, msg)
        );
    }


    @Test
    void testAllZeroKeyFails() {
        BigInteger zero = BigInteger.ZERO;
        byte[] msg = "test".getBytes();

        assertThrows(EncryptionException.class, () ->
                SchnorrQ.schnorrQSign(zero, zero, msg)
        );
    }

    @Test
    void testSignatureIsExactly64Bytes() throws Exception {
        BigInteger sk = getValidSecretKey();
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);
        byte[] msg = getRandomMessage();

        BigInteger signature = SchnorrQ.schnorrQSign(sk, pk, msg);
        byte[] sigBytes = signature.toByteArray();
        assertTrue(sigBytes.length <= 64, "Signature should be <= 64 bytes including leading zero padding");
    }

    @Test
    void testLongMessage() throws Exception {
        BigInteger sk = getValidSecretKey();
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);

        byte[] longMsg = new byte[10_000_000];
        new Random().nextBytes(longMsg);

        BigInteger sig = SchnorrQ.schnorrQSign(sk, pk, longMsg);
        assertTrue(SchnorrQ.schnorrQVerify(pk, sig, longMsg));
    }









    // Extra important tests comparing results to the C code
    @Test
    void testPublicKeyGeneration() throws EncryptionException {
        BigInteger publicKey = SchnorrQ.schnorrQKeyGeneration(VALID_PRIVATE_KEY);
        assertEquals(publicKey, VALID_PUBLIC_KEY);
    }

    @Test
    void testPairGeneration() throws EncryptionException {
        Pair<BigInteger, BigInteger> keys = SchnorrQ.schnorrQFullKeyGeneration();
        BigInteger secretKey = keys.first;
        BigInteger publicKey = keys.second;
        BigInteger genPublicKey = SchnorrQ.schnorrQKeyGeneration(secretKey);
        assertEquals(publicKey, genPublicKey);
    }

    @Test
    void testSign() throws EncryptionException {
        BigInteger signature = SchnorrQ.schnorrQSign(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY, VALID_MESSAGE);
        assertEquals(signature, VALID_SIGNATURE);
    }

    @Test
    void testVerify() throws EncryptionException {

    }
}
