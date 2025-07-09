import exceptions.EncryptionException;
import exceptions.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import types.data.Pair;

import static org.junit.jupiter.api.Assertions.*;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Random;


public class SchnorrQTests {
    private final int HEX_RADIX = 16;
    private final BigInteger VALID_PUBLIC_KEY = new BigInteger("dac0dedd92a1aa4da1342c4e2184686456a12a6ec8fdd8c594dc181353760c81", HEX_RADIX);
    private final BigInteger VALID_PRIVATE_KEY = new BigInteger("04ba23f508755f08869609c4aa784ad278cddfe94f101b09ed83ffd71511ee8e", HEX_RADIX);
    private final BigInteger VALID_SIGNATURE = new BigInteger("5f69d09df6e3bbe7ead33300d20b171fa7000c40e5a78fdc3daa8bad663d020bf34428735ede3bee44b4ca2d9f05c3c21fb3babcec613777cfb5d9fdffa32800", 16);
    private final byte[] VALID_MESSAGE = "a".getBytes(UTF_8);

    private final String FILES_PATH = System.getProperty("user.dir") + "/src/test/java/files";

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
    void testCorruptedSignatureFormat() {
        BigInteger malformedSignature = new BigInteger("-1");  // Possibly invalid
        assertThrows(EncryptionException.class, () -> SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, malformedSignature, VALID_MESSAGE));
    }

    @Test
    void testPublicKeyHighBitSet() {
        BigInteger keyWithMSB = BigInteger.ONE.shiftLeft(128);  // MSB set
        assertThrows(InvalidArgumentException.class, () ->
                SchnorrQ.schnorrQVerify(keyWithMSB, VALID_SIGNATURE, VALID_MESSAGE)
        );
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
    void testVeryLargeMessage() throws Exception {
        byte[] largeMessage = new byte[10_000_000];  // 10 MB
        new java.util.Random().nextBytes(largeMessage);
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, largeMessage);
        assertFalse(result);  // Likely fails if signature isn't valid for this message
    }

    private static byte[] getRandomMessage() {
        byte[] msg = new byte[32];
        new Random().nextBytes(msg);
        return msg;
    }

    @Test
    void testValidSignatureRoundTrip() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);
        byte[] msg = "SchnorrQ test message".getBytes(StandardCharsets.UTF_8);

        BigInteger sig = SchnorrQ.schnorrQSign(sk, pk, msg);
        assertTrue(SchnorrQ.schnorrQVerify(pk, sig, msg));
    }

    @Test
    void testSignatureChangesWithMessage() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);

        byte[] msg1 = "Message1".getBytes();
        byte[] msg2 = "Message2".getBytes();

        BigInteger sig1 = SchnorrQ.schnorrQSign(sk, pk, msg1);
        BigInteger sig2 = SchnorrQ.schnorrQSign(sk, pk, msg2);

        assertNotEquals(sig1, sig2, "Signatures for different messages should not match");
    }

    @Test
    void testTamperedMessageFailsVerification() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);
        byte[] msg = "Original".getBytes();

        BigInteger sig = SchnorrQ.schnorrQSign(sk, pk, msg);

        byte[] tampered = "OriginalX".getBytes();  // Tampered
        assertFalse(SchnorrQ.schnorrQVerify(pk, sig, tampered));
    }

    @Test
    void testLongMessage() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
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
        assertEquals(genPublicKey, publicKey);
    }

    @Test
    void testManyPairGeneration() throws EncryptionException {
        for (int i = 0; i < 10_000; i++) {
            testPairGeneration();
        }
    }

    @Test
    void testSign() throws EncryptionException {
        BigInteger signature = SchnorrQ.schnorrQSign(VALID_PRIVATE_KEY, VALID_PUBLIC_KEY, VALID_MESSAGE);
        assertEquals(signature, VALID_SIGNATURE);
    }

    @Test
    void testVerify() throws EncryptionException {
        assertTrue(SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, VALID_MESSAGE));
    }

    @Test
    void testManyKeyGens() throws EncryptionException, IOException {
        FileReader input = new FileReader(FILES_PATH + "/key_gen_tests.txt");
        BufferedReader bufRead = new BufferedReader(input);
        String myLine;

        while ( !(myLine = bufRead.readLine()).isBlank())
        {
            String tempLine = myLine;
            BigInteger correctPublicKey = new BigInteger(myLine.substring(14, 78), 16);
            myLine = bufRead.readLine();
            if (myLine.isBlank()) {
                break;
            }
            BigInteger secretKey = new BigInteger(myLine.substring(14, 78), 16);
            BigInteger testPublicKey = SchnorrQ.schnorrQKeyGeneration(secretKey);
            assertEquals(correctPublicKey, testPublicKey);
        }
    }

    @Test
    void testBrokenKey() throws EncryptionException {
        BigInteger pubKey = new BigInteger("91a909b0961e603fbcf4b7fbf489d7201a2ce69de3750081bf85b3ce226e6ffb", 16);
        BigInteger secretKey = new BigInteger("26d52d2764e10bef383cd8fa53f62d2b6930d63e7e1f108817cecff0f1d472dc", 16);
        BigInteger genPubKey = SchnorrQ.schnorrQKeyGeneration(secretKey);
        assertEquals(pubKey, genPubKey);
    }
}
