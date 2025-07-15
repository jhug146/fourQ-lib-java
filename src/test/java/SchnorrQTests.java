import api.SchnorrQ;
import utils.ByteArrayUtils;
import exceptions.EncryptionException;
import exceptions.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import types.data.Pair;

import static org.junit.jupiter.api.Assertions.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static utils.ByteArrayReverseMode.REMOVE_LEADING_ZERO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HexFormat;
import java.util.Random;


public class SchnorrQTests {
    private final int HEX_RADIX = 16;
    private final BigInteger VALID_PUBLIC_KEY = new BigInteger("41ae5f6d8fcf295b2b67a57b97fe58674818fa17b04844f697f58099dd08856f", HEX_RADIX);
    private final BigInteger VALID_PRIVATE_KEY = new BigInteger("9aa51ec6af8420987dee03b1453a9eeb8e7bf17db8b7a175b6294ba2095410bd", HEX_RADIX);
    private final BigInteger VALID_SIGNATURE = new BigInteger("f81ec975a9e0d24c480f1456104ca73c2d2785640f45266d03de6b1ef23d9c7edbc5904c4df55027393e3f25cf6a08e889fddd074b2c50e97f5962465e551c00", 16);
    private final byte[] VALID_MESSAGE = HexFormat.of().parseHex("cb");

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
        byte[] tamperedMessage =  ByteArrayUtils.reverseByteArray("The quick brown fox jumps".getBytes(UTF_8), REMOVE_LEADING_ZERO);
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
    void testPublicKeyNotOnCurve() throws EncryptionException {
        BigInteger fakeKey = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16); // clearly invalid
        assertFalse(SchnorrQ.schnorrQVerify(fakeKey, VALID_SIGNATURE, VALID_MESSAGE));
    }

    @Test
    void testVeryLargeMessage() throws Exception {
        byte[] largeMessage = new byte[10_000_000];  // 10 MB
        new java.util.Random().nextBytes(largeMessage);
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, ByteArrayUtils.reverseByteArray(largeMessage, REMOVE_LEADING_ZERO));
        assertFalse(result);  // Likely fails if signature isn't valid for this message
    }

    @Test
    void testValidSignatureRoundTrip() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);
        byte[] msg = ByteArrayUtils.reverseByteArray("api.SchnorrQ test message".getBytes(), REMOVE_LEADING_ZERO);

        BigInteger sig = SchnorrQ.schnorrQSign(sk, pk, msg);
        assertTrue(SchnorrQ.schnorrQVerify(pk, sig, msg));
    }

    @Test
    void testSignatureChangesWithMessage() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);

        byte[] msg1 = ByteArrayUtils.reverseByteArray("Message1".getBytes(), REMOVE_LEADING_ZERO);
        byte[] msg2 = ByteArrayUtils.reverseByteArray("Message2".getBytes(), REMOVE_LEADING_ZERO);

        BigInteger sig1 = SchnorrQ.schnorrQSign(sk, pk, msg1);
        BigInteger sig2 = SchnorrQ.schnorrQSign(sk, pk, msg2);

        assertNotEquals(sig1, sig2, "Signatures for different messages should not match");
    }

    @Test
    void testTamperedMessageFailsVerification() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);
        byte[] msg =  ByteArrayUtils.reverseByteArray("Original".getBytes(), REMOVE_LEADING_ZERO);

        BigInteger sig = SchnorrQ.schnorrQSign(sk, pk, msg);

        byte[] tampered =  ByteArrayUtils.reverseByteArray("OriginalX".getBytes(), REMOVE_LEADING_ZERO);  // Tampered
        assertFalse(SchnorrQ.schnorrQVerify(pk, sig, tampered));
    }

    @Test
    void testLongMessage() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);

        byte[] longMsg = new byte[10_000_000];
        new Random(12345L).nextBytes(longMsg);

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
        for (int i = 0; i < 10_000; i++) testPairGeneration();
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

    // Takes 3-5 minutes to run sometimes beware
    // Runs 100,000 key generation pair tests in the key_gen_tests.txt file
    @Test
    void testManyKeyGens() throws EncryptionException, IOException {
        FileReader input = new FileReader(FILES_PATH + "/key_gen_tests.txt");
        BufferedReader bufRead = new BufferedReader(input);
        String line;

        while ((line = bufRead.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            BigInteger secretKey = new BigInteger(line.substring(14), 16);
            line = bufRead.readLine();
            if (line == null || line.isBlank()) {
                break;
            }
            BigInteger correctPublicKey = new BigInteger(line.substring(14), 16);
            BigInteger testPublicKey = SchnorrQ.schnorrQKeyGeneration(secretKey);
            assertEquals(correctPublicKey, testPublicKey);
        }
    }

    @Test
    void testBrokenKey() throws EncryptionException {
        BigInteger pubKey = new BigInteger("507edd7fe7d21958f270a5f893260600a22485badcd9b1a7433678fd946c2ee4", 16);
        BigInteger secretKey = new BigInteger("375c79e3c979f6354f60018064ed8ea6bb26c6be7f712d4d814ba80942ecf3c2", 16);
        BigInteger genPubKey = SchnorrQ.schnorrQKeyGeneration(secretKey);
        assertEquals(pubKey, genPubKey);
    }

    @Test
    void testBrokenSignature() throws EncryptionException {
        BigInteger secretKey = new BigInteger("e1669de6854996e05c23d5e95e51022e61df5134957a1fecc939e3517ca95604", 16);
        BigInteger pubKey = new BigInteger("e4a87eef77e983ff7b974b3b29f4b141efa2e12de6a17d3a21dac77164788ddf", 16);
        BigInteger correctSignature = new BigInteger("132bf1f7a96c8e5a94202ceeb289ff5c47690bd27a95a5bb7bec35c0c9fcaba8e58c77c6792513d64eb93b42575752b6633e1db6ad86b62e0a53831bd40d0900", 16);
        byte[] message = HexFormat.of().parseHex("f9817e");

        BigInteger genSignature = SchnorrQ.schnorrQSign(secretKey, pubKey, message);
        var a = correctSignature.equals(genSignature);
        if (!a) {
            System.out.println();
            System.out.println(correctSignature.toString(16));
            System.out.println(genSignature.toString(16));
        }
        assertEquals(correctSignature, genSignature);
    }

    // Runs 20,000 signature generation tests in the sig_tests.txt file
    @Test
    void testManySignatures() throws EncryptionException, IOException {
        FileReader input = new FileReader(FILES_PATH + "/sig_tests.txt");
        BufferedReader bufRead = new BufferedReader(input);
        String line;
        while ((line = bufRead.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            BigInteger secretKey = new BigInteger(line.substring(14), 16);
            BigInteger publicKey = new BigInteger(bufRead.readLine().substring(14), 16);
            byte[] message = HexFormat.of().parseHex(bufRead.readLine().substring(11));
            BigInteger correctSignature = new BigInteger(bufRead.readLine().substring(13), 16);
            BigInteger genSignature = SchnorrQ.schnorrQSign(secretKey, publicKey, message);
            assertEquals(correctSignature, genSignature);
        }
    }

    // Runs 20,000 signature verification tests where the signature, message and public key are correct
    @Test
    void testManyTrueVerifications() throws IOException, EncryptionException {
        FileReader input = new FileReader(FILES_PATH + "/sig_tests.txt");
        BufferedReader bufRead = new BufferedReader(input);
        String line;
        int i = 0;
        while ((line = bufRead.readLine()) != null) {
            i++;
            System.out.println(i);
            if (line.isBlank()) {
                continue;
            }
            BigInteger publicKey = new BigInteger(bufRead.readLine().substring(14), 16);
            byte[] message = HexFormat.of().parseHex(bufRead.readLine().substring(11));
            BigInteger signature = new BigInteger(bufRead.readLine().substring(13), 16);
            boolean res = SchnorrQ.schnorrQVerify(publicKey, signature, message);
            assertTrue(res);
        }
    }

    @Test
    void testManyFalseSignatureVerifications() throws IOException, EncryptionException {
        FileReader input = new FileReader(FILES_PATH + "/false_sig_tests.txt");
        BufferedReader bufRead = new BufferedReader(input);
        String line;
        while ((line = bufRead.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            BigInteger publicKey = new BigInteger(bufRead.readLine().substring(14), 16);
            byte[] message = HexFormat.of().parseHex(bufRead.readLine().substring(11));
            BigInteger signature = new BigInteger(bufRead.readLine().substring(13), 16);
            assertFalse(SchnorrQ.schnorrQVerify(publicKey, signature, message));
        }
    }

    @Test
    void testManyFalsePubKeyVerifications() throws IOException, EncryptionException {
        FileReader input = new FileReader(FILES_PATH + "/false_pub_key_tests.txt");
        BufferedReader bufRead = new BufferedReader(input);
        String line;
        while ((line = bufRead.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            BigInteger publicKey = new BigInteger(bufRead.readLine().substring(14), 16);
            byte[] message = HexFormat.of().parseHex(bufRead.readLine().substring(11));
            BigInteger signature = new BigInteger(bufRead.readLine().substring(13), 16);
            assertFalse(SchnorrQ.schnorrQVerify(publicKey, signature, message));
        }
    }

    @Test
    void testManyFalseMessageVerifications() throws IOException, EncryptionException {
        FileReader input = new FileReader(FILES_PATH + "/false_message_tests.txt");
        BufferedReader bufRead = new BufferedReader(input);
        String line;
        while ((line = bufRead.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            BigInteger publicKey = new BigInteger(bufRead.readLine().substring(14), 16);
            byte[] message = ByteArrayUtils.reverseByteArray(HexFormat.of().parseHex(bufRead.readLine().substring(11)), REMOVE_LEADING_ZERO);
            BigInteger signature = new BigInteger(bufRead.readLine().substring(13), 16);
            assertFalse(SchnorrQ.schnorrQVerify(publicKey, signature, message));
        }
    }
}
