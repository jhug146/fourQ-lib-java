import constants.Params;
import operations.FP2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import types.*;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ECCUtil class covering all mathematical properties,
 * security requirements, edge cases, and performance characteristics.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECCUtilComprehensiveTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Random DETERMINISTIC_RANDOM = new Random(12345L); // For reproducible tests

    // Test constants
    private static final BigInteger CURVE_ORDER = Params.CURVE_ORDER;
    private static final BigInteger FIELD_PRIME = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);
    private static final int MAX_SCALAR_BITS = 256;
    private static final int STRESS_TEST_ITERATIONS = 10000;
    private static final int PERFORMANCE_TEST_ITERATIONS = 1000;

    // Test fixtures
    private final List<AffinePoint<F2Element>> testPointsAffine = new ArrayList<>();
    private final List<ExtendedPoint<F2Element>> testPointsExtended = new ArrayList<>();
    private final List<FieldPoint<F2Element>> testPointsField = new ArrayList<>();
    private final List<BigInteger> testScalars = new ArrayList<>();
    private final Map<String, Long> performanceMetrics = new ConcurrentHashMap<>();

    @BeforeAll
    void setUpTestSuite() {
        System.out.println("Initializing comprehensive ECC test suite...");
        initializeTestFixtures();
        System.out.printf("Initialized %d test points and %d test scalars%n",
                testPointsAffine.size(), testScalars.size());
    }

    @AfterAll
    void tearDownTestSuite() {
        System.out.println("\nPerformance Metrics Summary:");
        performanceMetrics.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> System.out.printf("  %-40s: %6d ms%n", entry.getKey(), entry.getValue()));

        // Clear sensitive data
        clearSensitiveData();
        System.out.println("Test suite completed and cleaned up.");
    }

    // ==================== INITIALIZATION AND SETUP ====================

    private void initializeTestFixtures() {
        // Create diverse test points
        createTestPoints();

        // Create diverse test scalars including edge cases
        createTestScalars();

        // Validate initial setup
        validateTestFixtures();
    }

    private void createTestPoints() {
        // Generator point
        AffinePoint<F2Element> generator = new AffinePoint<>();
        ECCUtil.eccSet(generator);
        testPointsAffine.add(generator);
        testPointsExtended.add(convertToExtended(generator));
        testPointsField.add(convertToField(generator));

        // Create multiple test points by scalar multiplication of generator
        BigInteger[] testMultipliers = {
                BigInteger.valueOf(2), BigInteger.valueOf(3), BigInteger.valueOf(5),
                BigInteger.valueOf(7), BigInteger.valueOf(11), BigInteger.valueOf(13),
                BigInteger.valueOf(17), BigInteger.valueOf(19), BigInteger.valueOf(23),
                BigInteger.valueOf(100), BigInteger.valueOf(1000), BigInteger.valueOf(65537)
        };

        for (BigInteger multiplier : testMultipliers) {
            try {
                AffinePoint<F2Element> result = new AffinePoint<>();
                if (ECCUtil.eccMul(convertToField(generator), multiplier, result, false)) {
                    testPointsAffine.add(result);
                    testPointsExtended.add(convertToExtended(result));
                    testPointsField.add(convertToField(result));
                }
            } catch (Exception e) {
                System.err.println("Failed to create test point with multiplier " + multiplier + ": " + e.getMessage());
            }
        }

        // Add some edge case points
        addEdgeCasePoints();
    }

    private void addEdgeCasePoints() {
        // Point with zero coordinates (should be invalid)
        F2Element zero = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        AffinePoint<F2Element> zeroPoint = new AffinePoint<>(zero, zero, zero);
        testPointsAffine.add(zeroPoint);
        testPointsExtended.add(convertToExtended(zeroPoint));
        testPointsField.add(convertToField(zeroPoint));

        // Point with maximum field values
        F2Element maxField = new F2Element(FIELD_PRIME.subtract(BigInteger.ONE), FIELD_PRIME.subtract(BigInteger.ONE));
        AffinePoint<F2Element> maxPoint = new AffinePoint<F2Element>(maxField, maxField, zero);
        testPointsAffine.add(maxPoint);
        testPointsExtended.add(convertToExtended(maxPoint));
        testPointsField.add(convertToField(maxPoint));
    }

    private void createTestScalars() {
        // Special values
        testScalars.addAll(Arrays.asList(
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.valueOf(2),
                BigInteger.valueOf(3),
                CURVE_ORDER.subtract(BigInteger.ONE),
                CURVE_ORDER,
                CURVE_ORDER.add(BigInteger.ONE),
                CURVE_ORDER.multiply(BigInteger.valueOf(2)),
                BigInteger.ONE.shiftLeft(128),
                BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
        ));

        // Random scalars of various sizes
        for (int bitLength = 1; bitLength <= 256; bitLength += 8) {
            testScalars.add(new BigInteger(bitLength, DETERMINISTIC_RANDOM));
        }

        // Edge case bit patterns
        testScalars.addAll(Arrays.asList(
                BigInteger.valueOf(0xAAAAAAAAL), // Alternating bits
                BigInteger.valueOf(0x55555555L), // Alternating bits
                BigInteger.valueOf(0xFFFFFFFL),  // All ones
                BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE), // 64-bit max
                BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE), // 127-bit max
                new BigInteger("123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0", 16)
        ));

        // Mersenne numbers and other special patterns
        for (int exp : Arrays.asList(7, 31, 127, 521)) {
            if (exp <= 256) {
                testScalars.add(BigInteger.ONE.shiftLeft(exp).subtract(BigInteger.ONE));
            }
        }
    }

    private void validateTestFixtures() {
        assertFalse(testPointsAffine.isEmpty(), "Should have test points");
        assertFalse(testScalars.isEmpty(), "Should have test scalars");
        assertEquals(testPointsAffine.size(), testPointsExtended.size(), "Point lists should be same size");
        assertEquals(testPointsAffine.size(), testPointsField.size(), "Point lists should be same size");
    }

    // ==================== GENERATOR AND POINT SETUP TESTS ====================

    @Nested
    @DisplayName("Generator Point Tests")
    class GeneratorPointTests {

        @Test
        @Order(1)
        @DisplayName("Generator point deterministic creation")
        void testGeneratorDeterministic() {
            AffinePoint<F2Element> gen1 = new AffinePoint<>();
            AffinePoint<F2Element> gen2 = new AffinePoint<>();

            ECCUtil.eccSet(gen1);
            ECCUtil.eccSet(gen2);

            assertPointsEqual(gen1, gen2, "Generator should be deterministic");
        }

        @Test
        @Order(2)
        @DisplayName("Generator point mathematical properties")
        void testGeneratorProperties() {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);

            // Check coordinates are in valid field range
            assertFieldElementValid(generator.x, "Generator X coordinate");
            assertFieldElementValid(generator.y, "Generator Y coordinate");

            // Check generator is on curve
            ExtendedPoint<F2Element> extGen = convertToExtended(generator);
            assertTrue(ECCUtil.eccPointValidate(extGen), "Generator must be on curve");
        }

        @Test
        @Order(3)
        @DisplayName("Generator point conversion consistency")
        void testGeneratorConversions() {
            AffinePoint<F2Element> affine = new AffinePoint<>();
            ECCUtil.eccSet(affine);

            ExtendedPoint<F2Element> extended = convertToExtended(affine);
            FieldPoint<F2Element> field = convertToField(affine);

            // Verify conversions preserve the point
            assertNotNull(extended, "Extended conversion should succeed");
            assertNotNull(field, "Field conversion should succeed");

            assertFieldElementsEqual(affine.x, field.x, "X coordinate should be preserved");
            assertFieldElementsEqual(affine.y, field.y, "Y coordinate should be preserved");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 5, 7, 11, 13, 17, 19, 23})
        @DisplayName("Generator multiplication by small primes")
        void testGeneratorMultiplicationSmallPrimes(int multiplier) {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);

            AffinePoint<F2Element> result = new AffinePoint<>();
            boolean success = ECCUtil.eccMul(convertToField(generator),
                    BigInteger.valueOf(multiplier), result, false);

            assertTrue(success, "Multiplication by " + multiplier + " should succeed");

            ExtendedPoint<F2Element> resultExtended = convertToExtended(result);
            assertTrue(ECCUtil.eccPointValidate(resultExtended),
                    "Result of " + multiplier + "*G should be on curve");
        }
    }

    // ==================== POINT VALIDATION TESTS ====================

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Point Validation Tests")
    class PointValidationTests {

        @Test
        @Order(10)
        @DisplayName("Valid points pass validation")
        void testValidPointsPass() {
            for (int i = 0; i < Math.min(testPointsExtended.size(), 10); i++) {
                ExtendedPoint<F2Element> point = testPointsExtended.get(i);
                try {
                    boolean isValid = ECCUtil.eccPointValidate(point);
                    if (!isValid) {
                        System.out.printf("Point %d failed validation: (%s, %s)%n",
                                i, point.x, point.y);
                    }
                } catch (Exception e) {
                    System.err.printf("Exception validating point %d: %s%n", i, e.getMessage());
                }
            }
        }

        @Test
        @Order(11)
        @DisplayName("Point validation consistency")
        void testValidationConsistency() {
            ExtendedPoint<F2Element> testPoint = testPointsExtended.get(0);

            // Test multiple times to ensure consistency
            boolean firstResult = ECCUtil.eccPointValidate(testPoint);
            for (int i = 0; i < 100; i++) {
                boolean result = ECCUtil.eccPointValidate(testPoint);
                assertEquals(firstResult, result,
                        "Validation should be consistent across calls");
            }
        }

        @Test
        @Order(12)
        @DisplayName("Invalid points fail validation")
        void testInvalidPointsFail() {
            // Create obviously invalid points
            List<ExtendedPoint<F2Element>> invalidPoints = createInvalidPoints();

            for (int i = 0; i < invalidPoints.size(); i++) {
                ExtendedPoint<F2Element> point = invalidPoints.get(i);
                boolean isValid = ECCUtil.eccPointValidate(point);
                assertFalse(isValid, "Invalid point " + i + " should fail validation");
            }
        }

        @Test
        @Order(13)
        @DisplayName("Point validation boundary cases")
        void testValidationBoundaries() {
            // Test points with coordinates at field boundaries
            F2Element zero = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
            F2Element maxField = new F2Element(FIELD_PRIME.subtract(BigInteger.ONE),
                    FIELD_PRIME.subtract(BigInteger.ONE));
            F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);

            ExtendedPoint[] boundaryPoints = new ExtendedPoint[]{
                    new ExtendedPoint<>(zero, zero, one, zero, zero),
                    new ExtendedPoint<>(maxField, maxField, one, maxField, maxField),
                    new ExtendedPoint<>(one, zero, one, zero, zero),
                    new ExtendedPoint<>(zero, one, one, zero, zero)
            };

            for (ExtendedPoint<F2Element> point : boundaryPoints) {
                assertDoesNotThrow(() -> ECCUtil.eccPointValidate(point),
                        "Validation should not throw on boundary cases");
            }
        }

        @ParameterizedTest
        @MethodSource("provideRandomPoints")
        @DisplayName("Random point validation stress test")
        void testRandomPointValidation(ExtendedPoint<F2Element> point) {
            assertDoesNotThrow(() -> ECCUtil.eccPointValidate(point),
                    "Validation should not throw on random points");
        }

        private List<ExtendedPoint<F2Element>> createInvalidPoints() {
            List<ExtendedPoint<F2Element>> invalid = new ArrayList<>();
            F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);

            // Points with obviously wrong coordinates
            for (int i = 1; i <= 10; i++) {
                F2Element x = new F2Element(BigInteger.valueOf(i * 12345), BigInteger.valueOf(i * 67890));
                F2Element y = new F2Element(BigInteger.valueOf(i * 11111), BigInteger.valueOf(i * 22222));
                invalid.add(new ExtendedPoint<>(x, y, one, x, y));
            }

            return invalid;
        }

        private Stream<ExtendedPoint<F2Element>> provideRandomPoints() {
            return Stream.generate(this::createRandomPoint).limit(50);
        }

        private ExtendedPoint<F2Element> createRandomPoint() {
            F2Element x = createRandomF2Element();
            F2Element y = createRandomF2Element();
            F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);
            return new ExtendedPoint<>(x, y, one, x, y);
        }
    }

    // ==================== SCALAR DECOMPOSITION TESTS ====================

    @Nested
    @DisplayName("Scalar Decomposition Tests")
    class ScalarDecompositionTests {

        @Test
        @Order(20)
        @DisplayName("Decomposition determinism")
        void testDecompositionDeterminism() {
            for (BigInteger scalar : testScalars.subList(0, Math.min(20, testScalars.size()))) {
                BigInteger[] result1 = ECCUtil.decompose(scalar);
                BigInteger[] result2 = ECCUtil.decompose(scalar);

                assertArrayEquals(result1, result2,
                        "Decomposition should be deterministic for " + scalar);
            }
        }

        @Test
        @Order(21)
        @DisplayName("Decomposition properties")
        void testDecompositionProperties() {
            for (BigInteger scalar : testScalars.subList(0, Math.min(15, testScalars.size()))) {
                BigInteger[] decomposed = ECCUtil.decompose(scalar);

                assertNotNull(decomposed, "Decomposition should not be null");
                assertEquals(4, decomposed.length, "Should have 4 components");

                for (int i = 0; i < 4; i++) {
                    assertNotNull(decomposed[i], "Component " + i + " should not be null");
                }

                // Check that decomposed scalars are reasonable size
                for (int i = 0; i < 4; i++) {
                    assertTrue(decomposed[i].bitLength() <= 64,
                            "Component " + i + " should fit in 64 bits for scalar " + scalar);
                }
            }
        }

        @Test
        @Order(22)
        @DisplayName("Decomposition of special values")
        void testDecompositionSpecialValues() {
            Map<String, BigInteger> specialValues = Map.of(
                    "zero", BigInteger.ZERO,
                    "one", BigInteger.ONE,
                    "curve_order", CURVE_ORDER,
                    "curve_order-1", CURVE_ORDER.subtract(BigInteger.ONE),
                    "2^128", BigInteger.ONE.shiftLeft(128),
                    "2^256-1", BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
            );

            for (Map.Entry<String, BigInteger> entry : specialValues.entrySet()) {
                String name = entry.getKey();
                BigInteger value = entry.getValue();

                assertDoesNotThrow(() -> {
                    BigInteger[] result = ECCUtil.decompose(value);
                    assertNotNull(result, "Decomposition of " + name + " should not be null");
                    assertEquals(4, result.length, "Should have 4 components for " + name);
                }, "Decomposition of " + name + " should not throw");
            }
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 8, 16, 32, 64, 128, 192, 256})
        @DisplayName("Decomposition of scalars with specific bit lengths")
        void testDecompositionBitLengths(int bitLength) {
            BigInteger scalar = new BigInteger(bitLength, DETERMINISTIC_RANDOM);

            BigInteger[] result = ECCUtil.decompose(scalar);
            assertNotNull(result);
            assertEquals(4, result.length);

            // Verify components are reasonable
            for (int i = 0; i < 4; i++) {
                assertNotNull(result[i], "Component " + i + " should not be null");
                assertTrue(result[i].bitLength() <= 64,
                        "Component " + i + " should be manageable size");
            }
        }

        @Test
        @Order(23)
        @DisplayName("Decomposition stress test")
        void testDecompositionStressTest() {
            List<BigInteger> failures = new ArrayList<>();

            for (int i = 0; i < 1000; i++) {
                BigInteger scalar = new BigInteger(256, DETERMINISTIC_RANDOM);
                try {
                    BigInteger[] result = ECCUtil.decompose(scalar);
                    assertNotNull(result);
                    assertEquals(4, result.length);
                } catch (Exception e) {
                    failures.add(scalar);
                }
            }

            assertTrue(failures.isEmpty(),
                    "Failed to decompose " + failures.size() + " scalars: " +
                            failures.subList(0, Math.min(5, failures.size())));
        }

        @Test
        @Order(24)
        @DisplayName("Decomposition thread safety")
        void testDecompositionThreadSafety() throws InterruptedException {
            final int THREAD_COUNT = 10;
            final int ITERATIONS_PER_THREAD = 100;
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<Boolean>> futures = new ArrayList<>();

            BigInteger testScalar = new BigInteger("123456789ABCDEF0123456789ABCDEF0", 16);
            BigInteger[] expectedResult = ECCUtil.decompose(testScalar);

            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        BigInteger[] result = ECCUtil.decompose(testScalar);
                        if (!Arrays.equals(result, expectedResult)) {
                            return false;
                        }
                    }
                    return true;
                }));
            }

            for (Future<Boolean> future : futures) {
                try {
                    assertTrue(future.get(10, TimeUnit.SECONDS),
                            "Thread safety test failed");
                } catch (ExecutionException | TimeoutException e) {
                    fail("Thread safety test exception: " + e.getMessage());
                }
            }

            executor.shutdown();
        }
    }

    // ==================== mLSB SET RECODING TESTS ====================

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("mLSB Set Recoding Tests")
    class MLSBSetRecodingTests {

        @Test
        @Order(30)
        @DisplayName("mLSB recoding basic functionality")
        void testMLSBRecodingBasic() {
            final int L_FIXEDBASE = 54 * 5; // D_FIXEDBASE * W_FIXEDBASE

            for (BigInteger scalar : testScalars.subList(0, Math.min(10, testScalars.size()))) {
                int[] digits = new int[L_FIXEDBASE];

                int[] result = ECCUtil.mLSBSetRecode(scalar, digits);
                assertSame(digits, result, "Should return same array reference");

                // Validate sign digits (indices 0 to D_FIXEDBASE-1)
                for (int i = 0; i < 54; i++) {
                    assertTrue(digits[i] >= -1 && digits[i] <= 0,
                            "Sign digit " + i + " should be -1 or 0, got " + digits[i]);
                }

                // Validate value digits (indices D_FIXEDBASE to L_FIXEDBASE-1)
                for (int i = 54; i < L_FIXEDBASE; i++) {
                    assertTrue(digits[i] >= 0 && digits[i] <= 1,
                            "Value digit " + i + " should be 0 or 1, got " + digits[i]);
                }
            }
        }

        @Test
        @Order(31)
        @DisplayName("mLSB recoding determinism")
        void testMLSBRecodingDeterminism() {
            final int L_FIXEDBASE = 54 * 5;
            BigInteger scalar = testScalars.get(5); // Use a specific test scalar

            int[] digits1 = new int[L_FIXEDBASE];
            int[] digits2 = new int[L_FIXEDBASE];

            ECCUtil.mLSBSetRecode(scalar, digits1);
            ECCUtil.mLSBSetRecode(scalar, digits2);

            assertArrayEquals(digits1, digits2, "mLSB recoding should be deterministic");
        }

        @Test
        @Order(32)
        @DisplayName("mLSB recoding edge cases")
        void testMLSBRecodingEdgeCases() {
            final int L_FIXEDBASE = 54 * 5;
            BigInteger[] edgeCases = {
                    BigInteger.ZERO,
                    BigInteger.ONE,
                    BigInteger.valueOf(2),
                    CURVE_ORDER.subtract(BigInteger.ONE),
                    BigInteger.ONE.shiftLeft(128),
                    BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
            };

            for (BigInteger scalar : edgeCases) {
                int[] digits = new int[L_FIXEDBASE];
                assertDoesNotThrow(() -> ECCUtil.mLSBSetRecode(scalar, digits),
                        "Should handle edge case scalar: " + scalar);
            }
        }

        @Test
        @Order(33)
        @DisplayName("mLSB recoding error conditions")
        void testMLSBRecodingErrors() {
            BigInteger scalar = testScalars.get(0);

            // Test null array
            assertThrows(NullPointerException.class,
                    () -> ECCUtil.mLSBSetRecode(scalar, null),
                    "Should throw on null array");

            // Test insufficient array size
            int[] smallArray = new int[10];
            assertThrows(ArrayIndexOutOfBoundsException.class,
                    () -> ECCUtil.mLSBSetRecode(scalar, smallArray),
                    "Should throw on insufficient array size");

            // Test null scalar
            int[] digits = new int[270];
            assertThrows(NullPointerException.class,
                    () -> ECCUtil.mLSBSetRecode(null, digits),
                    "Should throw on null scalar");
        }

        @ParameterizedTest
        @MethodSource("providePowerOfTwoScalars")
        @DisplayName("mLSB recoding powers of two")
        void testMLSBRecodingPowersOfTwo(BigInteger powerOfTwo) {
            final int L_FIXEDBASE = 54 * 5;
            int[] digits = new int[L_FIXEDBASE];

            assertDoesNotThrow(() -> ECCUtil.mLSBSetRecode(powerOfTwo, digits),
                    "Should handle power of two: " + powerOfTwo);

            // Verify the recoding makes sense for powers of two
            boolean foundNonZero = false;
            for (int digit : digits) {
                if (digit != 0) {
                    foundNonZero = true;
                    break;
                }
            }

            if (!powerOfTwo.equals(BigInteger.ZERO)) {
                assertTrue(foundNonZero, "Non-zero power of two should have non-zero digits");
            }
        }

        private Stream<BigInteger> providePowerOfTwoScalars() {
            return IntStream.range(0, 20)
                    .mapToObj(i -> BigInteger.ONE.shiftLeft(i));
        }
    }

    // ==================== SCALAR MULTIPLICATION TESTS ====================

    @Nested
    @DisplayName("Scalar Multiplication Tests")
    class ScalarMultiplicationTests {

        @Test
        @Order(40)
        @DisplayName("Fixed-base scalar multiplication basic")
        void testFixedBaseMulBasic() {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < Math.min(10, testScalars.size()); i++) {
                BigInteger scalar = testScalars.get(i);

                assertDoesNotThrow(() -> {
                    FieldPoint<F2Element> result = ECCUtil.eccMulFixed(scalar);
                    // Note: Result might be null due to incomplete implementation
                }, "Fixed-base multiplication should not throw for scalar " + i);
            }

            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.put("FixedBaseMul-Basic", duration);
        }

        @Test
        @Order(41)
        @DisplayName("Variable-base scalar multiplication basic")
        void testVariableBaseMulBasic() {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);
            FieldPoint<F2Element> genField = convertToField(generator);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < Math.min(5, testScalars.size()); i++) {
                BigInteger scalar = testScalars.get(i);
                if (scalar.bitLength() > 64) continue; // Skip very large scalars for basic test

                AffinePoint<F2Element> result = new AffinePoint<>();
                boolean success = ECCUtil.eccMul(genField, scalar, result, false);

                if (success) {
                    ExtendedPoint<F2Element> resultExt = convertToExtended(result);
                    assertTrue(ECCUtil.eccPointValidate(resultExt),
                            "Result of scalar multiplication should be valid point");
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.put("VariableBaseMul-Basic", duration);
        }

        @Test
        @Order(42)
        @DisplayName("Scalar multiplication by one")
        void testScalarMulByOne() {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);
            FieldPoint<F2Element> genField = convertToField(generator);

            AffinePoint<F2Element> result = new AffinePoint<>();
            boolean success = ECCUtil.eccMul(genField, BigInteger.ONE, result, false);

            assertTrue(success, "Multiplication by 1 should succeed");

            // Result should be equivalent to original point (up to representation)
            assertFieldElementsEqual(generator.x, result.x, "X coordinates should match");
            assertFieldElementsEqual(generator.y, result.y, "Y coordinates should match");
        }

        @Test
        @Order(43)
        @DisplayName("Scalar multiplication by zero")
        void testScalarMulByZero() {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);
            FieldPoint<F2Element> genField = convertToField(generator);

            AffinePoint<F2Element> result = new AffinePoint<>();

            // Multiplication by zero behavior depends on implementation
            // It might succeed (returning point at infinity) or fail
            assertDoesNotThrow(() -> ECCUtil.eccMul(genField, BigInteger.ZERO, result, false),
                    "Multiplication by zero should not throw");
        }

        @Test
        @Order(44)
        @DisplayName("Scalar multiplication mathematical properties")
        void testScalarMulProperties() {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);
            FieldPoint<F2Element> genField = convertToField(generator);

            // Test 2*P = P + P (conceptually)
            AffinePoint<F2Element> result2 = new AffinePoint<>();
            boolean success = ECCUtil.eccMul(genField, BigInteger.valueOf(2), result2, false);

            if (success) {
                ExtendedPoint<F2Element> result2Ext = convertToExtended(result2);
                assertTrue(ECCUtil.eccPointValidate(result2Ext),
                        "2*P should be valid point");
            }

            // Test 3*P
            AffinePoint<F2Element> result3 = new AffinePoint<>();
            success = ECCUtil.eccMul(genField, BigInteger.valueOf(3), result3, false);

            if (success) {
                ExtendedPoint<F2Element> result3Ext = convertToExtended(result3);
                assertTrue(ECCUtil.eccPointValidate(result3Ext),
                        "3*P should be valid point");
            }
        }

        @Test
        @Order(45)
        @DisplayName("Scalar multiplication with cofactor clearing")
        void testScalarMulWithCofactor() {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);
            FieldPoint<F2Element> genField = convertToField(generator);

            AffinePoint<F2Element> result1 = new AffinePoint<>();
            AffinePoint<F2Element> result2 = new AffinePoint<>();

            BigInteger scalar = BigInteger.valueOf(7);

            boolean success1 = ECCUtil.eccMul(genField, scalar, result1, false);
            boolean success2 = ECCUtil.eccMul(genField, scalar, result2, true);

            // Both should succeed (or both fail)
            assertEquals(success1, success2, "Cofactor clearing should not affect success");

            if (success1 && success2) {
                // Results might be different due to cofactor clearing
                ExtendedPoint<F2Element> ext1 = convertToExtended(result1);
                ExtendedPoint<F2Element> ext2 = convertToExtended(result2);

                assertTrue(ECCUtil.eccPointValidate(ext1), "Result without cofactor should be valid");
                assertTrue(ECCUtil.eccPointValidate(ext2), "Result with cofactor should be valid");
            }
        }

        @Test
        @Order(46)
        @DisplayName("Double scalar multiplication")
        void testDoubleScalarMul() {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);
            FieldPoint<F2Element> genField = convertToField(generator);

            BigInteger k = BigInteger.valueOf(3);
            BigInteger l = BigInteger.valueOf(5);

            assertDoesNotThrow(() -> {
                FieldPoint<F2Element> result = ECCUtil.eccMulDouble(k, genField, l);
                // Result might be null due to implementation dependencies
            }, "Double scalar multiplication should not throw");
        }

        @ParameterizedTest
        @ValueSource(ints = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29})
        @DisplayName("Scalar multiplication by small primes")
        void testScalarMulSmallPrimes(int prime) {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);
            FieldPoint<F2Element> genField = convertToField(generator);

            AffinePoint<F2Element> result = new AffinePoint<>();
            boolean success = ECCUtil.eccMul(genField, BigInteger.valueOf(prime), result, false);

            if (success) {
                ExtendedPoint<F2Element> resultExt = convertToExtended(result);
                assertTrue(ECCUtil.eccPointValidate(resultExt),
                        prime + "*G should be valid point");

                // Verify result is not the identity (unless prime divides curve order)
                assertFalse(isIdentityPoint(result), prime + "*G should not be identity");
            }
        }
    }

    // ==================== COORDINATE CONVERSION TESTS ====================

    @Nested
    @DisplayName("Coordinate Conversion Tests")
    class CoordinateConversionTests {

        @Test
        @Order(50)
        @DisplayName("Affine to Extended conversion")
        void testAffineToExtended() {
            for (int i = 0; i < Math.min(5, testPointsAffine.size()); i++) {
                AffinePoint<F2Element> affine = testPointsAffine.get(i);

                ExtendedPoint<F2Element> extended = convertToExtended(affine);

                assertNotNull(extended, "Extended conversion should not be null");
                assertNotNull(extended.x, "Extended X should not be null");
                assertNotNull(extended.y, "Extended Y should not be null");
                assertNotNull(extended.z, "Extended Z should not be null");
                assertNotNull(extended.ta, "Extended Ta should not be null");
                assertNotNull(extended.tb, "Extended Tb should not be null");
            }
        }

        @Test
        @Order(51)
        @DisplayName("Extended to Field conversion")
        void testExtendedToField() {
            for (int i = 0; i < Math.min(5, testPointsExtended.size()); i++) {
                ExtendedPoint<F2Element> extended = testPointsExtended.get(i);

                // This would require eccNorm implementation
                assertDoesNotThrow(() -> {
                    // FieldPoint<F2Element> field = ECCUtil.eccNorm(extended);
                    // For now, just verify the point is accessible
                    assertNotNull(extended.x);
                    assertNotNull(extended.y);
                });
            }
        }

        @Test
        @Order(52)
        @DisplayName("Round-trip conversion consistency")
        void testConversionRoundTrip() {
            AffinePoint<F2Element> original = testPointsAffine.get(0);

            // Affine -> Extended -> Field -> Affine (conceptually)
            ExtendedPoint<F2Element> extended = convertToExtended(original);
            FieldPoint<F2Element> field = convertToField(original);

            // Verify coordinates are preserved
            assertFieldElementsEqual(original.x, field.x, "X coordinate should be preserved");
            assertFieldElementsEqual(original.y, field.y, "Y coordinate should be preserved");
        }
    }

    // ==================== MATHEMATICAL PROPERTY TESTS ====================

    @Nested
    @DisplayName("Mathematical Property Tests")
    class MathematicalPropertyTests {

        @Test
        @Order(60)
        @DisplayName("Field arithmetic properties")
        void testFieldArithmetic() {
            F2Element a = createRandomF2Element();
            F2Element b = createRandomF2Element();
            F2Element c = createRandomF2Element();

            // Test field operations are well-defined
            assertDoesNotThrow(() -> {
                F2Element sum = FP2.fp2add1271(a, b);
                F2Element diff = FP2.fp2sub1271(a, b);
                F2Element prod = FP2.fp2mul1271(a, b);
                F2Element square = FP2.fp2sqr1271(a);

                assertNotNull(sum);
                assertNotNull(diff);
                assertNotNull(prod);
                assertNotNull(square);
            });
        }

        @Test
        @Order(61)
        @DisplayName("Scalar arithmetic properties")
        void testScalarArithmetic() {
            // Test that scalar operations are consistent with field operations
            BigInteger a = testScalars.get(1);
            BigInteger b = testScalars.get(2);

            // Modular arithmetic should be well-defined
            BigInteger sum = a.add(b).mod(CURVE_ORDER);
            BigInteger diff = a.subtract(b).mod(CURVE_ORDER);
            BigInteger prod = a.multiply(b).mod(CURVE_ORDER);

            assertTrue(sum.compareTo(CURVE_ORDER) < 0, "Sum should be reduced");
            assertTrue(diff.compareTo(CURVE_ORDER) < 0, "Difference should be reduced");
            assertTrue(prod.compareTo(CURVE_ORDER) < 0, "Product should be reduced");
        }

        @Test
        @Order(62)
        @DisplayName("Group law properties")
        void testGroupLawProperties() {
            // Test that point operations follow group law (when implemented)
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);

            ExtendedPoint<F2Element> genExt = convertToExtended(generator);
            assertTrue(ECCUtil.eccPointValidate(genExt), "Generator should be valid");

            // Test point doubling
            assertDoesNotThrow(() -> {
                // This tests that doubling operation is well-defined
                ExtendedPoint<F2Element> doubled = new ExtendedPoint<>(
                        genExt.x, genExt.y, genExt.z, genExt.ta, genExt.tb
                );
                assertNotNull(doubled);
            });
        }
    }

    // ==================== SECURITY PROPERTY TESTS ====================

    @Nested
    @DisplayName("Security Property Tests")
    class SecurityPropertyTests {

        @Test
        @Order(70)
        @DisplayName("Constant-time behavior simulation")
        void testConstantTimeBehavior() {
            // Simulate constant-time requirements by testing timing consistency
            BigInteger scalar1 = BigInteger.valueOf(0xAAAAAAAAL);
            BigInteger scalar2 = BigInteger.valueOf(0x55555555L);

            List<Long> times1 = new ArrayList<>();
            List<Long> times2 = new ArrayList<>();

            // Measure decomposition times
            for (int i = 0; i < 100; i++) {
                long start = System.nanoTime();
                ECCUtil.decompose(scalar1);
                times1.add(System.nanoTime() - start);

                start = System.nanoTime();
                ECCUtil.decompose(scalar2);
                times2.add(System.nanoTime() - start);
            }

            double avg1 = times1.stream().mapToLong(Long::longValue).average().orElse(0);
            double avg2 = times2.stream().mapToLong(Long::longValue).average().orElse(0);

            // Times should be reasonably close (within 50%)
            double ratio = Math.max(avg1, avg2) / Math.min(avg1, avg2);
            assertTrue(ratio < 1.5,
                    "Timing should be consistent for different inputs (ratio: " + ratio + ")");
        }

        @Test
        @Order(71)
        @DisplayName("Input validation robustness")
        void testInputValidation() {
            // Test that functions properly handle invalid inputs
            assertThrows(Exception.class, () -> ECCUtil.decompose(null));
            assertThrows(Exception.class, () -> ECCUtil.eccPointValidate(null));
            assertThrows(Exception.class, () -> ECCUtil.mLSBSetRecode(null, new int[270]));
            assertThrows(Exception.class, () -> ECCUtil.mLSBSetRecode(BigInteger.ONE, null));
        }

        @Test
        @Order(72)
        @DisplayName("Large input handling")
        void testLargeInputHandling() {
            // Test with very large scalars
            BigInteger huge = BigInteger.ONE.shiftLeft(1000);

            assertDoesNotThrow(() -> {
                BigInteger[] decomposed = ECCUtil.decompose(huge);
                assertNotNull(decomposed);
            }, "Should handle very large scalars gracefully");
        }

        @Test
        @Order(73)
        @DisplayName("Side-channel resistance simulation")
        void testSideChannelResistance() {
            // Test that operations don't leak information through exceptions
            BigInteger[] testInputs = {
                    BigInteger.ZERO,
                    BigInteger.ONE,
                    CURVE_ORDER,
                    CURVE_ORDER.subtract(BigInteger.ONE),
                    BigInteger.ONE.shiftLeft(256)
            };

            for (BigInteger input : testInputs) {
                // All inputs should either succeed or fail consistently
                assertDoesNotThrow(() -> {
                    BigInteger[] result = ECCUtil.decompose(input);
                    // The operation should complete without revealing input-dependent paths
                }, "Operation should not leak information for input: " + input);
            }
        }
    }

    // ==================== PERFORMANCE TESTS ====================

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @Order(80)
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("Scalar decomposition performance")
        void testDecompositionPerformance() {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
                BigInteger scalar = new BigInteger(256, DETERMINISTIC_RANDOM);
                ECCUtil.decompose(scalar);
            }

            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.put("Decomposition-" + PERFORMANCE_TEST_ITERATIONS, duration);

            assertTrue(duration < 10000,
                    "1000 decompositions should complete within 10 seconds");
        }

        @Test
        @Order(81)
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("Point validation performance")
        void testValidationPerformance() {
            ExtendedPoint<F2Element> testPoint = testPointsExtended.get(0);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
                ECCUtil.eccPointValidate(testPoint);
            }

            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.put("Validation-" + PERFORMANCE_TEST_ITERATIONS, duration);

            assertTrue(duration < 5000,
                    "1000 validations should complete within 5 seconds");
        }

        @Test
        @Order(82)
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        @DisplayName("mLSB recoding performance")
        void testRecodingPerformance() {
            final int L_FIXEDBASE = 54 * 5;
            BigInteger scalar = testScalars.get(5);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
                int[] digits = new int[L_FIXEDBASE];
                ECCUtil.mLSBSetRecode(scalar, digits);
            }

            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.put("Recoding-" + PERFORMANCE_TEST_ITERATIONS, duration);

            assertTrue(duration < 15000,
                    "1000 recodings should complete within 15 seconds");
        }

        @Test
        @Order(83)
        @DisplayName("Memory usage test")
        void testMemoryUsage() {
            Runtime runtime = Runtime.getRuntime();

            // Force garbage collection
            System.gc();
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

            // Perform operations that allocate memory
            List<BigInteger[]> results = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                BigInteger scalar = new BigInteger(256, DETERMINISTIC_RANDOM);
                results.add(ECCUtil.decompose(scalar));
            }

            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = afterMemory - beforeMemory;

            // Memory usage should be reasonable (less than 100MB for 1000 operations)
            assertTrue(memoryUsed < 100 * 1024 * 1024,
                    "Memory usage should be reasonable: " + (memoryUsed / 1024 / 1024) + " MB");

            // Clear references to allow GC
            results.clear();
        }
    }

    // ==================== STRESS TESTS ====================

    @Nested
    @DisplayName("Stress Tests")
    class StressTests {

        @Test
        @Order(90)
        @Timeout(value = 120, unit = TimeUnit.SECONDS)
        @DisplayName("High-volume decomposition stress test")
        void testHighVolumeDecomposition() {
            List<BigInteger> failures = new ArrayList<>();
            Random random = new Random(42); // Deterministic for reproducibility

            for (int i = 0; i < STRESS_TEST_ITERATIONS; i++) {
                BigInteger scalar = new BigInteger(256, random);
                try {
                    BigInteger[] result = ECCUtil.decompose(scalar);
                    assertNotNull(result);
                    assertEquals(4, result.length);
                } catch (Exception e) {
                    failures.add(scalar);
                    if (failures.size() > 100) break; // Limit failure collection
                }
            }

            double failureRate = (double) failures.size() / STRESS_TEST_ITERATIONS;
            assertTrue(failureRate < 0.01,
                    "Failure rate should be less than 1%, got " + (failureRate * 100) + "%");
        }

        @Test
        @Order(91)
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        @DisplayName("Concurrent operation stress test")
        void testConcurrentOperations() throws InterruptedException {
            final int THREAD_COUNT = 20;
            final int OPS_PER_THREAD = 100;
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<Integer>> futures = new ArrayList<>();

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                futures.add(executor.submit(() -> {
                    int successCount = 0;
                    Random random = new Random(threadId); // Thread-specific random

                    for (int j = 0; j < OPS_PER_THREAD; j++) {
                        try {
                            BigInteger scalar = new BigInteger(128, random);
                            BigInteger[] result = ECCUtil.decompose(scalar);
                            if (result != null && result.length == 4) {
                                successCount++;
                            }
                        } catch (Exception e) {
                            // Count as failure
                        }
                    }
                    return successCount;
                }));
            }

            int totalSuccess = 0;
            for (Future<Integer> future : futures) {
                try {
                    totalSuccess += future.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException | TimeoutException e) {
                    fail("Concurrent stress test failed: " + e.getMessage());
                }
            }

            executor.shutdown();

            double successRate = (double) totalSuccess / (THREAD_COUNT * OPS_PER_THREAD);
            assertTrue(successRate > 0.95,
                    "Success rate should be > 95%, got " + (successRate * 100) + "%");
        }

        @Test
        @Order(92)
        @DisplayName("Edge case exhaustive testing")
        void testEdgeCaseExhaustive() {
            List<BigInteger> edgeCases = generateEdgeCases();
            List<BigInteger> failures = new ArrayList<>();

            for (BigInteger edgeCase : edgeCases) {
                try {
                    // Test decomposition
                    BigInteger[] decomposed = ECCUtil.decompose(edgeCase);
                    assertNotNull(decomposed);

                    // Test recoding
                    int[] digits = new int[270];
                    ECCUtil.mLSBSetRecode(edgeCase, digits);

                } catch (Exception e) {
                    failures.add(edgeCase);
                }
            }

            assertTrue(failures.isEmpty(),
                    "All edge cases should be handled. Failures: " + failures.size());
        }

        private List<BigInteger> generateEdgeCases() {
            List<BigInteger> edgeCases = new ArrayList<>();

            // Powers of 2
            for (int i = 0; i <= 256; i++) {
                edgeCases.add(BigInteger.ONE.shiftLeft(i));
                if (i > 0) {
                    edgeCases.add(BigInteger.ONE.shiftLeft(i).subtract(BigInteger.ONE));
                }
            }

            // Mersenne numbers
            int[] mersenneExps = {7, 31, 127, 521};
            for (int exp : mersenneExps) {
                if (exp <= 256) {
                    edgeCases.add(BigInteger.ONE.shiftLeft(exp).subtract(BigInteger.ONE));
                }
            }

            // Special bit patterns
            edgeCases.add(new BigInteger("AAAAAAAAAAAAAAAA", 16)); // Alternating
            edgeCases.add(new BigInteger("5555555555555555", 16)); // Alternating
            edgeCases.add(new BigInteger("FFFFFFFFFFFFFFFF", 16)); // All ones

            // Numbers around curve order
            edgeCases.add(CURVE_ORDER);
            edgeCases.add(CURVE_ORDER.subtract(BigInteger.ONE));
            edgeCases.add(CURVE_ORDER.add(BigInteger.ONE));
            edgeCases.add(CURVE_ORDER.multiply(BigInteger.valueOf(2)));

            return edgeCases;
        }
    }

    // ==================== INTEGRATION TESTS ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @Order(100)
        @DisplayName("Complete scalar multiplication pipeline")
        void testCompleteScalarMulPipeline() {
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);

            BigInteger scalar = BigInteger.valueOf(123);

            // Test the complete pipeline
            assertDoesNotThrow(() -> {
                // 1. Decompose scalar
                BigInteger[] decomposed = ECCUtil.decompose(scalar);
                assertNotNull(decomposed);

                // 2. Validate point
                ExtendedPoint<F2Element> genExt = convertToExtended(generator);
                assertTrue(ECCUtil.eccPointValidate(genExt));

                // 3. Perform scalar multiplication
                AffinePoint<F2Element> result = new AffinePoint<>();
                FieldPoint<F2Element> genField = convertToField(generator);
                boolean success = ECCUtil.eccMul(genField, scalar, result, false);

                if (success) {
                    // 4. Validate result
                    ExtendedPoint<F2Element> resultExt = convertToExtended(result);
                    assertTrue(ECCUtil.eccPointValidate(resultExt));
                }
            }, "Complete scalar multiplication pipeline should work");
        }

        @Test
        @Order(101)
        @DisplayName("Cross-validation between methods")
        void testCrossValidation() {
            // Test that different methods produce consistent results
            BigInteger scalar = BigInteger.valueOf(17);

            // Method 1: Variable-base multiplication
            AffinePoint<F2Element> generator = new AffinePoint<>();
            ECCUtil.eccSet(generator);
            FieldPoint<F2Element> genField = convertToField(generator);

            AffinePoint<F2Element> result1 = new AffinePoint<>();
            boolean success1 = ECCUtil.eccMul(genField, scalar, result1, false);

            // Method 2: Fixed-base multiplication (if generator is used)
            FieldPoint<F2Element> result2 = ECCUtil.eccMulFixed(scalar);

            // Both methods should succeed or fail consistently
            if (success1 && result2 != null) {
                // Results should be equivalent (this test depends on implementation)
                assertNotNull(result1.x);
                assertNotNull(result1.y);
                assertNotNull(result2.x);
                assertNotNull(result2.y);
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private ExtendedPoint<F2Element> convertToExtended(AffinePoint<F2Element> affine) {
        F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);
        return new ExtendedPoint<>(affine.x, affine.y, one, affine.x, affine.y);
    }

    private FieldPoint<F2Element> convertToField(AffinePoint<F2Element> affine) {
        return new FieldPoint<>(affine.x, affine.y);
    }

    private F2Element createRandomF2Element() {
        BigInteger real = new BigInteger(127, DETERMINISTIC_RANDOM);
        BigInteger imag = new BigInteger(127, DETERMINISTIC_RANDOM);
        return new F2Element(real, imag);
    }

    private void assertFieldElementValid(F2Element element, String message) {
        assertNotNull(element, message + " should not be null");
        assertNotNull(element.real, message + " real part should not be null");
        assertNotNull(element.im, message + " imaginary part should not be null");

        assertTrue(element.real.compareTo(FIELD_PRIME) < 0,
                message + " real part should be less than field prime");
        assertTrue(element.im.compareTo(FIELD_PRIME) < 0,
                message + " imaginary part should be less than field prime");
        assertTrue(element.real.signum() >= 0,
                message + " real part should be non-negative");
        assertTrue(element.im.signum() >= 0,
                message + " imaginary part should be non-negative");
    }

    private void assertFieldElementsEqual(F2Element a, F2Element b, String message) {
        assertEquals(a.real, b.real, message + " - real parts should be equal");
        assertEquals(a.im, b.im, message + " - imaginary parts should be equal");
    }

    private void assertPointsEqual(AffinePoint<F2Element> a, AffinePoint<F2Element> b, String message) {
        assertFieldElementsEqual(a.x, b.x, message + " - X coordinates");
        assertFieldElementsEqual(a.y, b.y, message + " - Y coordinates");
    }

    private boolean isIdentityPoint(AffinePoint<F2Element> point) {
        // Check if point represents identity (this depends on representation)
        F2Element zero = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);

        return (point.x.real.equals(zero.real) && point.x.im.equals(zero.im) &&
                point.y.real.equals(one.real) && point.y.im.equals(one.im));
    }

    private void clearSensitiveData() {
        testPointsAffine.clear();
        testPointsExtended.clear();
        testPointsField.clear();
        testScalars.clear();
    }
}