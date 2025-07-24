package fourqj.api;

import fourqj.crypto.primitives.HashFunction;
import fourqj.crypto.primitives.Kangaroo12;
import fourqj.utils.BigIntegerUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HexFormat;

class SchnorrQK12Test {

    private final HashFunction k12 = new Kangaroo12();
    private final SchnorrQ schnorrQ = new SchnorrQ(k12);

    @Test
    void schnorrQSign() {
        byte[] message = HexFormat.of().parseHex("da5fc4f10c7f3f020a02c1be37b96ba4987426aee9de2ad42351024feffb3302");
        BigInteger secretKey = new BigInteger("58aabff86c0cd56874851343be2a7023b83c4760d0b70ec3e1e2ccffa5c5a8ae", 16);
        BigInteger publicKey = new BigInteger("88b5035d1ba0860b1c26ae5a4a070a52f1596d10d816bad98862eb75f77f9fad", 16);
        BigInteger signed = schnorrQ.schnorrQSign(secretKey, publicKey, message);

        byte[] signature = BigIntegerUtils.bigIntegerToByte(signed, 64, false);
        byte[] expected = HexFormat.of().parseHex("16d18a2f1a9f3403a5549f9fa3f0242458c077b22867d0db7bb6b8a23824fe1fae90b8df0b81858eb77058bf0928ad55247fa027e2cf6ee440b91ad839042300");
        Assertions.assertArrayEquals(expected, signature);
    }

    @Test
    void schnorrQSignWithNonceK() {
        byte[] message = HexFormat.of().parseHex("da5fc4f10c7f3f020a02c1be37b96ba4987426aee9de2ad42351024feffb3302");
        BigInteger publicKey = new BigInteger("88b5035d1ba0860b1c26ae5a4a070a52f1596d10d816bad98862eb75f77f9fad", 16);
        byte[] hashK = HexFormat.of().parseHex("1d335317cb32506c484aa582c78b89053deddc7ea3a600749a69515a800aa861dad6767145fd7e802d2d4b256681794c23195a4034d14d7f0dcd67cadafa0701");
        BigInteger signed = schnorrQ.schnorrQSignWithNonceK(() -> hashK, publicKey, message);

        byte[] signature = BigIntegerUtils.bigIntegerToByte(signed, 64, false);
        byte[] expected = HexFormat.of().parseHex("16d18a2f1a9f3403a5549f9fa3f0242458c077b22867d0db7bb6b8a23824fe1fae90b8df0b81858eb77058bf0928ad55247fa027e2cf6ee440b91ad839042300");
        Assertions.assertArrayEquals(expected, signature);
    }

    @Test
    void schnorrQVerify() {
        BigInteger publicKey = new BigInteger("88b5035d1ba0860b1c26ae5a4a070a52f1596d10d816bad98862eb75f77f9fad", 16);
        BigInteger signature = new BigInteger("16d18a2f1a9f3403a5549f9fa3f0242458c077b22867d0db7bb6b8a23824fe1fae90b8df0b81858eb77058bf0928ad55247fa027e2cf6ee440b91ad839042300", 16);
        byte[] message = HexFormat.of().parseHex("da5fc4f10c7f3f020a02c1be37b96ba4987426aee9de2ad42351024feffb3302");
        Assertions.assertTrue(schnorrQ.schnorrQVerify(publicKey, signature, message));

        Assertions.assertFalse(schnorrQ.schnorrQVerify(publicKey, signature, HexFormat.of().parseHex("aa5fc4f10c7f3f020a02c1be37b96ba4987426aee9de2ad42351024feffb3302")));
    }

    @Test
    void schnorrQVerify_GivenSignatureDoesNotMatch_ThenFalse() {
        BigInteger publicKey = new BigInteger("88b5035d1ba0860b1c26ae5a4a070a52f1596d10d816bad98862eb75f77f9fad", 16);
        BigInteger signature = new BigInteger("16d18a2f1a9f3403a5549f9fa3f0242458c077b22867d0db7bb6b8a23824fe1fae90b8df0b81858eb77058bf0928ad55247fa027e2cf6ee440b91ad839042300", 16);
        byte[] invalidMessage = HexFormat.of().parseHex("aa5fc4f10c7f3f020a02c1be37b96ba4987426aee9de2ad42351024feffb3302");
        Assertions.assertFalse(schnorrQ.schnorrQVerify(publicKey, signature, invalidMessage));

    }

}