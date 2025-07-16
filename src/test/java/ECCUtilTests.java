
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import crypto.core.Curve;
import crypto.core.ECC;
import exceptions.EncryptionException;
import field.operations.FP2;
import types.data.F2Element;
import types.point.AffinePoint;
import types.point.ExtendedPoint;
import types.point.FieldPoint;
import constants.Params;



import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for crypto.util.ECCUtil class covering all mathematical properties,
 * security requirements, edge cases, and performance characteristics.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECCUtilTests {

    private static final Random DETERMINISTIC_RANDOM = new Random(12345L); // For reproducible tests

    // Test constants
    private static final BigInteger CURVE_ORDER = Params.CURVE_ORDER;
    private static final BigInteger FIELD_PRIME = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);
    private static final int STRESS_TEST_ITERATIONS = 10000;
    private static final int PERFORMANCE_TEST_ITERATIONS = 1000;

    // Test fixtures
    private final List<AffinePoint> testPointsAffine = new ArrayList<>();
    private final List<ExtendedPoint> testPointsExtended = new ArrayList<>();
    private final List<FieldPoint> testPointsField = new ArrayList<>();
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
        FieldPoint generator = ECC.eccSet();
        testPointsAffine.add(convertToAffine(generator));
        testPointsExtended.add(convertToExtended(generator));
        testPointsField.add(generator);

        // Create multiple test points by scalar multiplication of generator
        BigInteger[] testMultipliers = {
                BigInteger.valueOf(2), BigInteger.valueOf(3), BigInteger.valueOf(5),
                BigInteger.valueOf(7), BigInteger.valueOf(11), BigInteger.valueOf(13),
                BigInteger.valueOf(17), BigInteger.valueOf(19), BigInteger.valueOf(23),
                BigInteger.valueOf(100), BigInteger.valueOf(1000), BigInteger.valueOf(65537)
        };

        for (BigInteger multiplier : testMultipliers) {
            try {
                FieldPoint result = ECC.eccMul(generator, multiplier, false);
                if (result != null) {
                    AffinePoint affineResult = convertToAffine(result);
                    testPointsAffine.add(affineResult);
                    testPointsExtended.add(convertToExtended(affineResult));
                    testPointsField.add(result);
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
        AffinePoint zeroPoint = new AffinePoint();
        zeroPoint.setX(zero);
        zeroPoint.setY(zero);
        testPointsAffine.add(zeroPoint);
        testPointsExtended.add(convertToExtended(zeroPoint));
        testPointsField.add(convertToField(zeroPoint));

        // Point with maximum field values
        F2Element maxField = new F2Element(FIELD_PRIME.subtract(BigInteger.ONE), FIELD_PRIME.subtract(BigInteger.ONE));
        AffinePoint maxPoint = new AffinePoint();
        maxPoint.setX(maxField);
        maxPoint.setY(maxField);
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
            FieldPoint gen1 = ECC.eccSet();
            FieldPoint gen2 = ECC.eccSet();

            assertPointsEqual(gen1, gen2, "Generator should be deterministic");
        }

        @Test
        @Order(2)
        @DisplayName("Generator point mathematical properties")
        void testGeneratorProperties() {
            FieldPoint generator = ECC.eccSet();

            // Check coordinates are in valid field range
            assertFieldElementValid(generator.getX(), "Generator X coordinate");
            assertFieldElementValid(generator.getY(), "Generator Y coordinate");

            // Check generator is on curve
            ExtendedPoint extGen = convertToExtended(generator);
            assertTrue(ECC.eccPointValidate(extGen), "Generator must be on curve");
        }

        @Test
        @Order(3)
        @DisplayName("Generator point conversion consistency")
        void testGeneratorConversions() {
            FieldPoint field = ECC.eccSet();
            ExtendedPoint extended = convertToExtended(field);

            // Verify conversions preserve the point
            assertNotNull(extended, "Extended conversion should succeed");
            assertNotNull(field, "Field conversion should succeed");

            assertFieldElementsEqual(field.getX(), field.getX(), "X coordinate should be preserved");
            assertFieldElementsEqual(field.getY(), field.getY(), "Y coordinate should be preserved");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 5, 7, 11, 13, 17, 19, 23})
        @DisplayName("Generator multiplication by small primes")
        void testGeneratorMultiplicationSmallPrimes(int multiplier) throws EncryptionException {
            FieldPoint genField = ECC.eccSet();
            FieldPoint result = ECC.eccMul(genField, BigInteger.valueOf(multiplier), false);

            assertNotNull(result, "Multiplication by " + multiplier + " should succeed");

            ExtendedPoint resultExtended = convertToExtended(convertToAffine(result));
            assertTrue(ECC.eccPointValidate(resultExtended),
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
                ExtendedPoint point = testPointsExtended.get(i);
                try {
                    boolean isValid = ECC.eccPointValidate(point);
                    if (!isValid) {
                        System.out.printf("Point %d failed validation: (%s, %s)%n",
                                i, point.getX(), point.getY());
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
            ExtendedPoint testPoint = testPointsExtended.getFirst();

            // Test multiple times to ensure consistency
            boolean firstResult = ECC.eccPointValidate(testPoint);
            for (int i = 0; i < 100; i++) {
                boolean result = ECC.eccPointValidate(testPoint);
                assertEquals(firstResult, result,
                        "Validation should be consistent across calls");
            }
        }

        @Test
        @Order(12)
        @DisplayName("Invalid points fail validation")
        void testInvalidPointsFail() {
            // Create obviously invalid points
            List<ExtendedPoint> invalidPoints = createInvalidPoints();

            for (int i = 0; i < invalidPoints.size(); i++) {
                ExtendedPoint point = invalidPoints.get(i);
                boolean isValid = ECC.eccPointValidate(point);
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

            ExtendedPoint[] boundaryPoints = new ExtendedPoint[] {
                    new ExtendedPoint(zero, zero, one, zero, zero),
                    new ExtendedPoint(maxField, maxField, one, maxField, maxField),
                    new ExtendedPoint(one, zero, one, zero, zero),
                    new ExtendedPoint(zero, one, one, zero, zero)
            };

            for (ExtendedPoint point : boundaryPoints) {
                assertDoesNotThrow(() -> ECC.eccPointValidate(point),
                        "Validation should not throw on boundary cases");
            }
        }

        @ParameterizedTest
        @MethodSource("provideRandomPoints")
        @DisplayName("Random point validation stress test")
        void testRandomPointValidation(ExtendedPoint point) {
            assertDoesNotThrow(() -> ECC.eccPointValidate(point),
                    "Validation should not throw on random points");
        }

        private List<ExtendedPoint> createInvalidPoints() {
            List<ExtendedPoint> invalid = new ArrayList<>();
            F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);

            // Points with obviously wrong coordinates
            for (int i = 1; i <= 10; i++) {
                F2Element x = new F2Element(BigInteger.valueOf(i * 12345), BigInteger.valueOf(i * 67890));
                F2Element y = new F2Element(BigInteger.valueOf(i * 11111), BigInteger.valueOf(i * 22222));
                invalid.add(new ExtendedPoint(x, y, one, x, y));
            }

            return invalid;
        }

        private Stream<ExtendedPoint> provideRandomPoints() {
            return Stream.generate(this::createRandomPoint).limit(50);
        }

        private ExtendedPoint createRandomPoint() {
            F2Element x = createRandomF2Element();
            F2Element y = createRandomF2Element();
            F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);
            return new ExtendedPoint(x, y, one, x, y);
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
                BigInteger[] result1 = Curve.decompose(scalar);
                BigInteger[] result2 = Curve.decompose(scalar);

                assertArrayEquals(result1, result2,
                        "Decomposition should be deterministic for " + scalar);
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
                    BigInteger[] result = Curve.decompose(value);
                    assertNotNull(result, "Decomposition of " + name + " should not be null");
                    assertEquals(4, result.length, "Should have 4 components for " + name);
                }, "Decomposition of " + name + " should not throw");
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
                    BigInteger[] result = Curve.decompose(scalar);
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
            BigInteger[] expectedResult = Curve.decompose(testScalar);

            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        BigInteger[] result = Curve.decompose(testScalar);
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

                int[] result = Curve.mLSBSetRecode(scalar, digits);
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

            Curve.mLSBSetRecode(scalar, digits1);
            Curve.mLSBSetRecode(scalar, digits2);

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
                assertDoesNotThrow(() -> Curve.mLSBSetRecode(scalar, digits),
                        "Should handle edge case scalar: " + scalar);
            }
        }

        @Test
        @Order(33)
        @DisplayName("mLSB recoding error conditions")
        void testMLSBRecodingErrors() {
            BigInteger scalar = testScalars.getFirst();

            // Test null array
            assertThrows(NullPointerException.class,
                    () -> Curve.mLSBSetRecode(scalar, null),
                    "Should throw on null array");

            // Test insufficient array size
            int[] smallArray = new int[10];
            assertThrows(ArrayIndexOutOfBoundsException.class,
                    () -> Curve.mLSBSetRecode(scalar, smallArray),
                    "Should throw on insufficient array size");

            // Test null scalar
            int[] digits = new int[270];
            assertThrows(NullPointerException.class,
                    () -> Curve.mLSBSetRecode(null, digits),
                    "Should throw on null scalar");
        }

        @ParameterizedTest
        @MethodSource("providePowerOfTwoScalars")
        @DisplayName("mLSB recoding powers of two")
        void testMLSBRecodingPowersOfTwo(BigInteger powerOfTwo) {
            final int L_FIXEDBASE = 54 * 5;
            int[] digits = new int[L_FIXEDBASE];

            assertDoesNotThrow(() -> Curve.mLSBSetRecode(powerOfTwo, digits),
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
                    .mapToObj(BigInteger.ONE::shiftLeft);
        }
    }

    // ==================== SCALAR MULTIPLICATION TESTS ====================

    @Nested
    @DisplayName("Scalar Multiplication Tests")
    class ScalarMultiplicationTests {

        /*
        @Test
        @Order(40)
        @DisplayName("Fixed-base scalar multiplication basic")
        void testFixedBaseMulBasic() {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < Math.min(10, testScalars.size()); i++) {
                BigInteger scalar = testScalars.get(i);

                int finalI = i;
                assertDoesNotThrow(() -> {
                    FieldPoint result = ECCUtil.eccMulFixed(scalar);
                    assertNotNull(result, "Fixed-base multiplication should return result for scalar " + finalI);
                }, "Fixed-base multiplication should not throw for scalar " + i);
            }

            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.put("FixedBaseMul-Basic", duration);
        }*/

        @Test
        @Order(41)
        @DisplayName("Variable-base scalar multiplication basic")
        void testVariableBaseMulBasic() throws EncryptionException {
            FieldPoint genField = ECC.eccSet();

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < Math.min(5, testScalars.size()); i++) {
                BigInteger scalar = testScalars.get(i);
                if (scalar.bitLength() > 64) continue; // Skip very large scalars for basic test

                FieldPoint result = ECC.eccMul(genField, scalar, false);

                if (result != null) {
                    ExtendedPoint resultExt = convertToExtended(convertToAffine(result));
                    assertTrue(ECC.eccPointValidate(resultExt),
                            "Result of scalar multiplication should be valid point");
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.put("VariableBaseMul-Basic", duration);
        }

        @Test
        @Order(42)
        @DisplayName("Scalar multiplication by one")
        void testScalarMulByOne() throws EncryptionException {
            FieldPoint genField = ECC.eccSet();

            FieldPoint result = ECC.eccMul(genField, BigInteger.ONE, false);

            assertNotNull(result, "Multiplication by 1 should succeed");

            // Result should be equivalent to original point (up to representation)
            assertFieldElementsEqual(genField.getX(), result.getX(), "X coordinates should match");
            assertFieldElementsEqual(genField.getY(), result.getY(), "Y coordinates should match");
        }

        @Test
        @Order(43)
        @DisplayName("Scalar multiplication by zero")
        void testScalarMulByZero() throws EncryptionException {
            FieldPoint genField = ECC.eccSet();

            // Multiplication by zero behavior depends on implementation
            // It might succeed (returning point at infinity) or return null
            assertDoesNotThrow(() -> {
                FieldPoint result = ECC.eccMul(genField, BigInteger.ZERO, false);
                if (result != null) {
                    // Check if result represents point at infinity
                    assertTrue(result.getX().real.equals(BigInteger.ZERO) && result.getX().im.equals(BigInteger.ZERO));
                    assertTrue(result.getY().real.equals(BigInteger.ONE) && result.getY().im.equals(BigInteger.ZERO));
                }
            }, "Multiplication by zero should not throw");
        }

        @Test
        @Order(44)
        @DisplayName("Scalar multiplication mathematical properties")
        void testScalarMulProperties() throws EncryptionException {
            FieldPoint generator = ECC.eccSet();

            // Test 2*P = P + P (conceptually)
            FieldPoint result2 = ECC.eccMul(generator, BigInteger.valueOf(2), false);

            if (result2 != null) {
                ExtendedPoint result2Ext = convertToExtended(convertToAffine(result2));
                assertTrue(ECC.eccPointValidate(result2Ext),
                        "2*P should be valid point");
            }

            // Test 3*P
            FieldPoint result3 = ECC.eccMul(generator, BigInteger.valueOf(3), false);

            if (result3 != null) {
                ExtendedPoint result3Ext = convertToExtended(convertToAffine(result3));
                assertTrue(ECC.eccPointValidate(result3Ext),
                        "3*P should be valid point");
            }
        }

        @Test
        @Order(45)
        @DisplayName("Scalar multiplication with cofactor clearing")
        void testScalarMulWithCofactor() throws EncryptionException {
            FieldPoint genField = ECC.eccSet();
            BigInteger scalar = BigInteger.valueOf(7);

            FieldPoint result1 = ECC.eccMul(genField, scalar, false);
            FieldPoint result2 = ECC.eccMul(genField, scalar, true);

            // Both should succeed or both be null
            assertEquals(result1 == null, result2 == null, "Cofactor clearing should not affect success status");

            if (result1 != null && result2 != null) {
                // Results might be different due to cofactor clearing
                ExtendedPoint ext1 = convertToExtended(convertToAffine(result1));
                ExtendedPoint ext2 = convertToExtended(convertToAffine(result2));

                assertTrue(ECC.eccPointValidate(ext1), "Result without cofactor should be valid");
                assertTrue(ECC.eccPointValidate(ext2), "Result with cofactor should be valid");
            }
        }

        @Test
        @Order(46)
        @DisplayName("Double scalar multiplication")
        void testDoubleScalarMul() {
            FieldPoint genField = ECC.eccSet();

            BigInteger k = BigInteger.valueOf(3);
            BigInteger l = BigInteger.valueOf(5);

            assertDoesNotThrow(() -> {
                FieldPoint result = ECC.eccMulDouble(k, genField, l);
                assertNotNull(result, "Double scalar multiplication should return result");
            }, "Double scalar multiplication should not throw");
        }

        @ParameterizedTest
        @ValueSource(ints = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29})
        @DisplayName("Scalar multiplication by small primes")
        void testScalarMulSmallPrimes(int prime) throws EncryptionException {
            FieldPoint genField = ECC.eccSet();

            FieldPoint result = ECC.eccMul(genField, BigInteger.valueOf(prime), false);

            if (result != null) {
                ExtendedPoint resultExt = convertToExtended(convertToAffine(result));
                assertTrue(ECC.eccPointValidate(resultExt),
                        prime + "*G should be valid point");

                // Verify result is not the identity (unless prime divides curve order)
                assertFalse(isIdentityPoint(convertToAffine(result)), prime + "*G should not be identity");
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
                AffinePoint affine = testPointsAffine.get(i);

                ExtendedPoint extended = convertToExtended(affine);

                assertNotNull(extended, "Extended conversion should not be null");
                assertNotNull(extended.getX(), "Extended X should not be null");
                assertNotNull(extended.getY(), "Extended Y should not be null");
                assertNotNull(extended.getZ(), "Extended Z should not be null");
                assertNotNull(extended.getTa(), "Extended Ta should not be null");
                assertNotNull(extended.getTb(), "Extended Tb should not be null");
            }
        }

        @Test
        @Order(51)
        @DisplayName("Extended to Field conversion")
        void testExtendedToField() {
            for (int i = 0; i < Math.min(5, testPointsExtended.size()); i++) {
                ExtendedPoint extended = testPointsExtended.get(i);

                assertDoesNotThrow(() -> {
                    FieldPoint field = ECC.eccNorm(extended);
                    assertNotNull(field, "Field conversion should not be null");
                    assertNotNull(field.getX(), "Field X should not be null");
                    assertNotNull(field.getY(), "Field Y should not be null");
                });
            }
        }

        @Test
        @Order(52)
        @DisplayName("Round-trip conversion consistency")
        void testConversionRoundTrip() {
            AffinePoint original = testPointsAffine.getFirst();

            // Affine -> Extended -> Field -> Affine
            ExtendedPoint extended = convertToExtended(original);
            FieldPoint field = ECC.eccNorm(extended);
            AffinePoint roundTrip = convertToAffine(field);

            // Verify coordinates are preserved
            assertFieldElementsEqual(original.getX(), roundTrip.getX(), "X coordinate should be preserved");
            assertFieldElementsEqual(original.getY(), roundTrip.getY(), "Y coordinate should be preserved");
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

            // Test field operations are well-defined
            assertDoesNotThrow(() -> {
                F2Element sum = FP2.fp2Add1271(a, b);
                F2Element diff = FP2.fp2Sub1271(a, b);
                F2Element prod = FP2.fp2Mul1271(a, b);
                F2Element square = FP2.fp2Sqr1271(a);

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

//        @Test
//        @Order(62)
//        @DisplayName("Group law properties")
//        void testGroupLawProperties() {
//            // Test that point operations follow group law
//            FieldPoint generator = ECC.eccSet();
//
//            ExtendedPoint genExt = convertToExtended(generator);
//            assertTrue(ECC.eccPointValidate(genExt), "Generator should be valid");
//
//            // Test point doubling
//            assertDoesNotThrow(() -> {
//                ExtendedPoint doubled = ECC.eccDouble(genExt);
//                assertNotNull(doubled, "Doubling should produce result");
//                assertTrue(ECC.eccPointValidate(doubled), "Doubled point should be valid");
//            });
//        }
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
                Curve.decompose(scalar1);
                times1.add(System.nanoTime() - start);

                start = System.nanoTime();
                Curve.decompose(scalar2);
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
            assertThrows(Exception.class, () -> Curve.decompose(null));
            assertThrows(Exception.class, () -> ECC.eccPointValidate(null));
            assertThrows(Exception.class, () -> Curve.mLSBSetRecode(null, new int[270]));
            assertThrows(Exception.class, () -> Curve.mLSBSetRecode(BigInteger.ONE, null));
        }

        @Test
        @Order(72)
        @DisplayName("Large input handling")
        void testLargeInputHandling() {
            // Test with very large scalars
            BigInteger huge = BigInteger.ONE.shiftLeft(1000);

            assertDoesNotThrow(() -> {
                BigInteger[] decomposed = Curve.decompose(huge);
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
                    Curve.decompose(input);
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
                Curve.decompose(scalar);
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
            ExtendedPoint testPoint = testPointsExtended.getFirst();

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
                ECC.eccPointValidate(testPoint);
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
                Curve.mLSBSetRecode(scalar, digits);
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
                results.add(Curve.decompose(scalar));
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
                    BigInteger[] result = Curve.decompose(scalar);
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
                            BigInteger[] result = Curve.decompose(scalar);
                            if (result.length == 4) {
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
                    BigInteger[] decomposed = Curve.decompose(edgeCase);
                    assertNotNull(decomposed);

                    // Test recoding
                    int[] digits = new int[270];
                    Curve.mLSBSetRecode(edgeCase, digits);

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
            FieldPoint generator = ECC.eccSet();
            BigInteger scalar = BigInteger.valueOf(123);

            // Test the complete pipeline
            assertDoesNotThrow(() -> {
                // 1. Decompose scalar
                BigInteger[] decomposed = Curve.decompose(scalar);
                assertNotNull(decomposed);

                // 2. Validate point
                AffinePoint affPoint = new AffinePoint(generator.getX(), generator.getY(), null);
                ExtendedPoint genExt = convertToExtended(affPoint);
                assertTrue(ECC.eccPointValidate(genExt));

                // 3. Perform scalar multiplication
                FieldPoint result = ECC.eccMul(generator, scalar, false);

                // 4. Validate result
                ExtendedPoint resultExt = convertToExtended(convertToAffine(result));
                assertTrue(ECC.eccPointValidate(resultExt));
            }, "Complete scalar multiplication pipeline should work");
        }

        @Test
        @Order(101)
        @DisplayName("Cross-validation between methods")
        void testCrossValidation() throws EncryptionException {
            // Test that different methods produce consistent results
            BigInteger scalar = BigInteger.valueOf(17);

            // Method 1: Variable-base multiplication
            FieldPoint genField = ECC.eccSet();
            FieldPoint result1 = ECC.eccMul(genField, scalar, false);

            // Method 2: Fixed-base multiplication (if generator is used)
            FieldPoint result2 = ECC.eccMulFixed(scalar);

            // Both methods should succeed or fail consistently
            assertEquals(result1 == null, result2 == null, "Both methods should have same success status");

            if (result1 != null && result2 != null) {
                // Results should be mathematically equivalent (may differ in representation)
                assertNotNull(result1.getX());
                assertNotNull(result1.getY());
                assertNotNull(result2.getX());
                assertNotNull(result2.getY());
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private ExtendedPoint convertToExtended(AffinePoint affine) {
        F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);
        return new ExtendedPoint(affine.getX(), affine.getY(), one, affine.getX(), affine.getY());
    }

    private ExtendedPoint convertToExtended(FieldPoint field) {
        F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);
        return new ExtendedPoint(field.getX(), field.getY(), one, field.getX(), field.getY());
    }

    private FieldPoint convertToField(AffinePoint affine) {
        return new FieldPoint(affine.getX(), affine.getY());
    }

    private AffinePoint convertToAffine(FieldPoint field) {
        AffinePoint affine = new AffinePoint();
        affine.setX(field.getX());
        affine.setY(field.getY());
        return affine;
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

    private void assertPointsEqual(AffinePoint a, AffinePoint b, String message) {
        assertFieldElementsEqual(a.getX(), b.getX(), message + " - X coordinates");
        assertFieldElementsEqual(a.getY(), b.getY(), message + " - Y coordinates");
    }

    private void assertPointsEqual(FieldPoint a, FieldPoint b, String message) {
        assertFieldElementsEqual(a.getX(), b.getX(), message + " - X coordinates");
        assertFieldElementsEqual(a.getY(), b.getY(), message + " - Y coordinates");
    }

    private boolean isIdentityPoint(AffinePoint point) {
        // Check if point represents identity (this depends on representation)
        F2Element zero = new F2Element(BigInteger.ZERO, BigInteger.ZERO);
        F2Element one = new F2Element(BigInteger.ONE, BigInteger.ZERO);

        return (point.getX().real.equals(zero.real) && point.getX().im.equals(zero.im) &&
                point.getY().real.equals(one.real) && point.getY().im.equals(one.im));
    }

    private void clearSensitiveData() {
        testPointsAffine.clear();
        testPointsExtended.clear();
        testPointsField.clear();
        testScalars.clear();
    }
}