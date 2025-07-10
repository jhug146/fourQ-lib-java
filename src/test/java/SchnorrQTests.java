import constants.ArrayUtils;
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
import java.util.HexFormat;
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
    void testPublicKeyNotOnCurve() throws EncryptionException {
        BigInteger fakeKey = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16); // clearly invalid
        assertFalse(SchnorrQ.schnorrQVerify(fakeKey, VALID_SIGNATURE, VALID_MESSAGE));
    }

    @Test
    void testVeryLargeMessage() throws Exception {
        byte[] largeMessage = new byte[10_000_000];  // 10 MB
        new java.util.Random().nextBytes(largeMessage);
        boolean result = SchnorrQ.schnorrQVerify(VALID_PUBLIC_KEY, VALID_SIGNATURE, largeMessage);
        assertFalse(result);  // Likely fails if signature isn't valid for this message
    }

    @Test
    void testValidSignatureRoundTrip() throws Exception {
        BigInteger sk = VALID_PRIVATE_KEY;
        BigInteger pk = SchnorrQ.schnorrQKeyGeneration(sk);
        byte[] msg = "SchnorrQ test message".getBytes();

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
        BigInteger secretKey = new BigInteger("b38e773b492b10cc01e8e69522fc7726fffb61172bf2cec092853000618200a7", 16);
        BigInteger pubKey = new BigInteger("c3b4478a779e7d48335f9c4d0776af72bc200c5e21070179d1c4a5b6eeef49e1", 16);
        BigInteger correctSignature = new BigInteger("f0e275abe8d041ef96ed38706f7a433dbce1ddb65f5c68c869cbb16b96f7533d2c7a79b3ea427c6c54c793f047980ce78925db9be9494cf35a239a90812a0200", 16);
        byte[] message = ArrayUtils.reverseByteArray(HexFormat.of().parseHex("c56fa643fd9fef1e236fe88459c4d2097c00d03ca891a8b9e417da7b161925db88cb1f856a0ea48d7d8c12d750e4e0cce4b0088c41b04626c720a1dd39c7b9c192d847fde6eb8a64779c3bc8811b9465cb9df20d4d38331558d4f2929bab532e839a2b6a85b5cefd5209c5d3245938eff62afc44622f59bb044b4d9ff7a0cd7a3bf8e4c0aeb2bd00bb82d3dfdc0bcfd236cb1698fb6f53ffbba09eb2416c2c7c64113c12c3fa127f7ce55e58f12d2b27f941bff4b113f36cb3911ef4fd4a70625bad741fa7879e236cfc7c5d2aa78423e8441799570a050a9b23ff996e6ffbc91c6fe8c3f686e7638363c0ad0a45d0f289e78ce0f191ea8cb5e925235920ec7590d539865b20e9de83aa8b8def5b7f78420b58339d42c0522ce575850661fa9636331c92530670d6b0fc639f57e3179aee6fcd8bb18ddddd73526279b45d0fea902b7ce431edbae1e91e80400197daef06a87bb8355895a8abf8215f553049e55cc6c98db3846f9ca2efdca387b7928d5f0d459466db3d11d35e70288fb90deb7fd778325be7cefdd7aba05e6232ebc1403155a60c92b7dff1270780e0146b60ebe39246cb6143a20ce300"));

        BigInteger genSignature = SchnorrQ.schnorrQSign(secretKey, pubKey, (message));
        var a = correctSignature.equals(genSignature);
        if (!a) {
            System.out.println();
            System.out.println(correctSignature.toString(16));
            System.out.println(genSignature.toString(16));
        }
        assertEquals(correctSignature, genSignature);
    }

    @Test
    void testManySignatures() throws EncryptionException, IOException {
        FileReader input = new FileReader(FILES_PATH + "/sig_tests.txt");
        BufferedReader bufRead = new BufferedReader(input);
        String line;

        int i = 0;
        while ((line = bufRead.readLine()) != null) {
            i++;
            if (line.isBlank()) {
                continue;
            }
            System.out.println(i);
            BigInteger secretKey = new BigInteger(line.substring(14), 16);
            BigInteger publicKey = new BigInteger(bufRead.readLine().substring(14), 16);
            byte[] message = HexFormat.of().parseHex(bufRead.readLine().substring(11));
            BigInteger correctSignature = new BigInteger(bufRead.readLine().substring(13), 16);
            // In the following, the message byte array must be reversed since the generated signature assumes the message array was in little endian,
                // to match, the message array must hence be reversed. This is only applicable here and not in general use cases.
            BigInteger genSignature = SchnorrQ.schnorrQSign(secretKey, publicKey, ArrayUtils.reverseByteArray(message));
            var a = correctSignature.equals(genSignature);
            if (!a) {
                System.out.println();
                System.out.printf("Expected: %s\n", correctSignature.toString(16));
                System.out.printf("Actual: %s\n", genSignature.toString(16));
                System.out.printf("Public Key: %s\n", publicKey.toString(16));
                System.out.printf("Secret Key: %s\n", secretKey.toString(16));
                System.out.printf("Message: %s\n", ArrayUtils.reverseByteArray(message));
                System.out.printf("Correct Sig: %s\n", correctSignature.toString(16));
            }
            assertEquals(correctSignature, genSignature);
        }
    }
}
