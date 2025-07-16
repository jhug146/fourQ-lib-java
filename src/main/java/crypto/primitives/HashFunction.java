package crypto.primitives;

import java.math.BigInteger;

import org.jetbrains.annotations.NotNull;

import exceptions.EncryptionException;


public interface HashFunction {
    byte[] computeHash(@NotNull BigInteger input, boolean reverse) throws EncryptionException;

    byte[] computeHash(byte[] bytes, boolean reverse) throws EncryptionException;
}
