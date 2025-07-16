package crypto.primitives;

import exceptions.EncryptionException;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public interface HashFunction {
    byte[] computeHash(@NotNull BigInteger input, boolean reverse) throws EncryptionException;

    byte[] computeHash(byte[] bytes, boolean reverse) throws EncryptionException;
}
