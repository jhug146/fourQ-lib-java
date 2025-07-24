package fourqj.utils;

import fourqj.constants.Key;
import fourqj.constants.Params;
import fourqj.crypto.core.ECC;
import fourqj.crypto.primitives.HashFunction;
import fourqj.exceptions.EncryptionException;
import fourqj.fieldoperations.FP;
import fourqj.types.point.FieldPoint;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static fourqj.utils.BigIntegerUtils.copyBigIntegerToByteArray;
import static fourqj.utils.ByteArrayUtils.copyByteArrayToByteArray;

/**
 * Utility class providing functional interfaces for validation operations and cryptographic chains.
 *
 * @author Naman Malhotra, James Hughff
 * @since 1.0.3
 */
public final class SchnorrQUtils {
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public interface ValidationChain<T> {
        ValidationChain<T> validate(Predicate<T> condition, Supplier<RuntimeException> exceptionSupplier);
        
        default ValidationChain<T> validate(Predicate<T> condition, ThrowingRunnable throwingRunnable) {
            return validate(condition, () -> {
                try {
                    throwingRunnable.run();
                    // Not reached if throwingRunnable throws expected
                    return new RuntimeException("Expected exception was not thrown");
                }
                catch (RuntimeException e) { return e; }
                catch (Exception e) { return new RuntimeException(e); }
            });
        }

        default void notNull(String message) {
            validate(Objects::nonNull, () -> new IllegalArgumentException(message));
        }

        static <T> ValidationChain<T> of(T value) {
            return new ValidationChainImpl<>(value);
        }
    }

    private record ValidationChainImpl<T>(T value) implements ValidationChain<T> {

        @Override
            public ValidationChain<T> validate(Predicate<T> condition, Supplier<RuntimeException> exceptionSupplier) {
                if (!condition.test(value)) throw exceptionSupplier.get();
                return this;
            }
        }

    /**
     * Functional interface for building byte arrays with fluent copy operations.
     * Simplifies common buffer management patterns.
     */
    public static class BufferBuilder {
        private final byte[] buffer;

        private BufferBuilder(byte[] message) {
            this.buffer = new byte[message.length + Key.SIGNATURE_SIZE];
        }

        public static BufferBuilder forMessage(byte[] message) {
            return new BufferBuilder(message);
        }

        public BufferBuilder copyByteArray(byte[] src, int srcOffset, int destOffset, int length) {
            copyByteArrayToByteArray(src, srcOffset, buffer, destOffset, length);
            return this;
        }

        public BufferBuilder copyByteArray(byte[] src, int destOffset) {
            copyByteArrayToByteArray(src, Params.noOffset, buffer, destOffset, src.length);
            return this;
        }

        public BufferBuilder copyBigInteger(BigInteger value, int size, int destOffset) {
            copyBigIntegerToByteArray(value, size, buffer, destOffset);
            return this;
        }

        public byte[] build() {
            return buffer;
        }
    }

    /**
     * Functional interface for chaining cryptographic operations.
     * Simplifies common patterns like hash → transform → encode.
     */
    @FunctionalInterface
    public interface CryptoOperationChain<T> {
        T execute() throws EncryptionException;

        /** Creates a hash→BigInteger chain for byte array inputs. */
        static CryptoOperationChain<BigInteger> hashToBigInteger(HashFunction hashFunction, byte[] input, boolean reverse) {
            return () -> new BigInteger(Params.signPositive, hashFunction.computeHash(input, reverse));
        }

        /** Creates a hash→BigInteger→ECC→encode chain for key generation with BigInteger input. */
        static CryptoOperationChain<BigInteger> hashToEncodedPoint(HashFunction hashFunction, BigInteger input, boolean reverse) {
            return () -> {
                BigInteger hash = new BigInteger(Params.signPositive, hashFunction.computeHash(input, reverse));
                FieldPoint point = ECC.eccMulFixed(hash);
                return CryptoUtils.encode(point);
            };
        }

        /** Creates a hash→BigInteger→moduloOrder chain for byte array input. */
        static CryptoOperationChain<BigInteger> hashToModuloOrder(HashFunction hashFunction, byte[] input, boolean reverse) {
            return () -> {
                BigInteger hash = new BigInteger(Params.signPositive, hashFunction.computeHash(input, reverse));
                return FP.moduloOrder(hash);
            };
        }
    }
}