package fourqj.utils;

import java.util.function.Supplier;

public final class ExceptionUtils {

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static <T, E extends Exception> T tryOrThrow(ThrowingSupplier<T> supplier, E exception) throws E {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw exception;
        }
    }

    public static <E extends Exception> void tryOrThrow(ThrowingRunnable runnable, E exception) throws E {
        try {
            runnable.run();
        } catch (Exception e) {
            throw exception;
        }
    }

    public static <T> T tryOrThrow(ThrowingSupplier<T> supplier, RuntimeException exception) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw exception;
        }
    }

    public static void tryOrThrow(ThrowingRunnable runnable, RuntimeException exception) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw exception;
        }
    }

    public static <T> T tryOrThrow(ThrowingSupplier<T> supplier, Supplier<RuntimeException> exceptionSupplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw exceptionSupplier.get();
        }
    }

    public static void tryOrThrow(ThrowingRunnable runnable, Supplier<RuntimeException> exceptionSupplier) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw exceptionSupplier.get();
        }
    }

    private ExceptionUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}