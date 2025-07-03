package crypto;

import constants.Params;
import constants.PregeneratedTables;
import operations.FP2;
import types.F2Element;
import types.PreComputedExtendedPoint;

import java.math.BigInteger;
import java.util.Arrays;

import static operations.FP2.fp2Copy1271;

public class DeprecatedTable {
    public static PreComputedExtendedPoint<F2Element> tableLookupFixedBase(
            PreComputedExtendedPoint<F2Element>[] table,
            int digit,
            int sign
    ) {
        PreComputedExtendedPoint<F2Element> tempPoint;
        PreComputedExtendedPoint<F2Element> point = table[0].dup();
        BigInteger mask;

        int shiftAmount = 8 * Integer.SIZE - 1;

        for (int i = 1; i < Params.VPOINTS_FIXEDBASE; i++) {
            digit--;
            // While digit>=0, mask = 0xFF...F else mask = 0x00...0
            mask = BigInteger.valueOf((digit >> shiftAmount) - 1);
            tempPoint = table[i].dup();

            // Constant-time selection
            for (int j = 0; j < Params.NWORDS_FIELD; j++) {
                // Get byte arrays for point coordinates
                byte[] pointXYReal = point.xy.real.toByteArray();
                byte[] pointXYIm = point.xy.im.toByteArray();
                byte[] pointYXReal = point.yx.real.toByteArray();
                byte[] pointYXIm = point.yx.im.toByteArray();
                byte[] pointTReal = point.t.real.toByteArray();
                byte[] pointTIm = point.t.im.toByteArray();

                // Get byte arrays for temp point coordinates
                byte[] tempXYReal = tempPoint.xy.real.toByteArray();
                byte[] tempXYIm = tempPoint.xy.im.toByteArray();
                byte[] tempYXReal = tempPoint.yx.real.toByteArray();
                byte[] tempYXIm = tempPoint.yx.im.toByteArray();
                byte[] tempTReal = tempPoint.t.real.toByteArray();
                byte[] tempTIm = tempPoint.t.im.toByteArray();

                // Ensure arrays are large enough
                if (j < pointXYReal.length && j < tempXYReal.length) {
                    pointXYReal[j] = (byte) ((mask.intValue() & (pointXYReal[j] ^ tempXYReal[j])) ^ pointXYReal[j]);
                }
                if (j < pointXYIm.length && j < tempXYIm.length) {
                    pointXYIm[j] = (byte) ((mask.intValue() & (pointXYIm[j] ^ tempXYIm[j])) ^ pointXYIm[j]);
                }
                if (j < pointYXReal.length && j < tempYXReal.length) {
                    pointYXReal[j] = (byte) ((mask.intValue() & (pointYXReal[j] ^ tempYXReal[j])) ^ pointYXReal[j]);
                }
                if (j < pointYXIm.length && j < tempYXIm.length) {
                    pointYXIm[j] = (byte) ((mask.intValue() & (pointYXIm[j] ^ tempYXIm[j])) ^ pointYXIm[j]);
                }
                if (j < pointTReal.length && j < tempTReal.length) {
                    pointTReal[j] = (byte) ((mask.intValue() & (pointTReal[j] ^ tempTReal[j])) ^ pointTReal[j]);
                }
                if (j < pointTIm.length && j < tempTIm.length) {
                    pointTIm[j] = (byte) ((mask.intValue() & (pointTIm[j] ^ tempTIm[j])) ^ pointTIm[j]);
                }

                // Update point with modified coordinates
                point = new PreComputedExtendedPoint<>(
                        new F2Element(new BigInteger(pointXYReal), new BigInteger(pointXYIm)),
                        new F2Element(new BigInteger(pointYXReal), new BigInteger(pointYXIm)),
                        point.z,
                        new F2Element(new BigInteger(pointTReal), new BigInteger(pointTIm))
                );
            }
        }

        // Handle sign: create negative point
        tempPoint = new PreComputedExtendedPoint<>(
                FP2.fp2Copy1271(point.yx),  // swap x+y and y-x
                FP2.fp2Copy1271(point.xy),
                point.z,
                FP2.fp2Neg1271(FP2.fp2Copy1271(point.t))  // negate t coordinate
        );

        // Constant-time selection based on sign
        for (int j = 0; j < Params.NWORDS_FIELD; j++) {
            byte[] pointXYReal = point.xy.real.toByteArray();
            byte[] pointXYIm = point.xy.im.toByteArray();
            byte[] pointYXReal = point.yx.real.toByteArray();
            byte[] pointYXIm = point.yx.im.toByteArray();
            byte[] pointTReal = point.t.real.toByteArray();
            byte[] pointTIm = point.t.im.toByteArray();

            byte[] tempXYReal = tempPoint.xy.real.toByteArray();
            byte[] tempXYIm = tempPoint.xy.im.toByteArray();
            byte[] tempYXReal = tempPoint.yx.real.toByteArray();
            byte[] tempYXIm = tempPoint.yx.im.toByteArray();
            byte[] tempTReal = tempPoint.t.real.toByteArray();
            byte[] tempTIm = tempPoint.t.im.toByteArray();

            if (j < pointXYReal.length && j < tempXYReal.length) {
                pointXYReal[j] = (byte) ((sign & (pointXYReal[j] ^ tempXYReal[j])) ^ pointXYReal[j]);
            }
            if (j < pointXYIm.length && j < tempXYIm.length) {
                pointXYIm[j] = (byte) ((sign & (pointXYIm[j] ^ tempXYIm[j])) ^ pointXYIm[j]);
            }
            if (j < pointYXReal.length && j < tempYXReal.length) {
                pointYXReal[j] = (byte) ((sign & (pointYXReal[j] ^ tempYXReal[j])) ^ pointYXReal[j]);
            }
            if (j < pointYXIm.length && j < tempYXIm.length) {
                pointYXIm[j] = (byte) ((sign & (pointYXIm[j] ^ tempYXIm[j])) ^ pointYXIm[j]);
            }
            if (j < pointTReal.length && j < tempTReal.length) {
                pointTReal[j] = (byte) ((sign & (pointTReal[j] ^ tempTReal[j])) ^ pointTReal[j]);
            }
            if (j < pointTIm.length && j < tempTIm.length) {
                pointTIm[j] = (byte) ((sign & (pointTIm[j] ^ tempTIm[j])) ^ pointTIm[j]);
            }

            point = new PreComputedExtendedPoint<>(
                    new F2Element(new BigInteger(pointXYReal), new BigInteger(pointXYIm)),
                    new F2Element(new BigInteger(pointYXReal), new BigInteger(pointYXIm)),
                    point.z,
                    new F2Element(new BigInteger(pointTReal), new BigInteger(pointTIm))
            );
        }

        return point.dup();
    }

    public static class FixedBaseTableLookup {
        // General method that handles any offset and sign index
        public static PreComputedExtendedPoint<F2Element> performTableLookupWithOffset(
                int offset, int digit, int[] digits, int signIndex) {

            // Bounds checks
            if (offset >= PregeneratedTables.FIXED_BASE_TABLE_POINTS.length) {
                throw new IndexOutOfBoundsException("Table offset out of bounds: " + offset);
            }
            if (signIndex < 0 || signIndex >= digits.length) {
                throw new IndexOutOfBoundsException("Sign index out of bounds: " + signIndex);
            }

            // Create subset of table starting from offset
            PreComputedExtendedPoint<F2Element>[] tableSubset =
                    Arrays.copyOfRange(PregeneratedTables.FIXED_BASE_TABLE_POINTS,
                            offset,
                            Math.min(offset + Params.VPOINTS_FIXEDBASE, PregeneratedTables.FIXED_BASE_TABLE_POINTS.length));

            // Call the table lookup method with the sign from digits array
            return tableLookupFixedBase(tableSubset, digit, digits[signIndex]);
        }

        // Specific methods for each case
        public static PreComputedExtendedPoint<F2Element> performTableLookupInitial(
                int v, int w, int digit, int[] digits, int d) {
            int offset = (v - 1) * (1 << (w - 1));
            int signIndex = d - 1;
            return performTableLookupWithOffset(offset, digit, digits, signIndex);
        }

        public static PreComputedExtendedPoint<F2Element> performTableLookupFirstLoop(
                int v, int w, int j, int e, int d, int digit, int[] digits) {
            int offset = (v - j - 2) * (1 << (w - 1));
            int signIndex = d - (j + 1) * e - 1;
            return performTableLookupWithOffset(offset, digit, digits, signIndex);
        }

        public static PreComputedExtendedPoint<F2Element> performTableLookupSecondLoop(
                int v, int w, int j, int e, int d, int ii, int digit, int[] digits) {
            int offset = (v - j - 1) * (1 << (w - 1));
            int signIndex = d - j * e + ii - e;
            return performTableLookupWithOffset(offset, digit, digits, signIndex);
        }
    }

    /**
     * Constant-time table lookup to extract a point represented as (X+Y,Y-X,2Z,2dT)
     * corresponding to extended twisted Edwards coordinates (X:Y:Z:T)
     *
     * @param table array containing 8 precomputed points
     * @param digit the digit to lookup (1-8)
     * @param signMask sign mask: 0 for positive, 0xFFFFFFFF for negative
     * @return P = sign*table[digit-1], where sign=1 if signMask=0 and sign=-1 if signMask=0xFFFFFFFF
     */
    public static PreComputedExtendedPoint<F2Element> tableLookup1x8(
            PreComputedExtendedPoint<F2Element>[] table,
            int digit,
            int signMask
    ) {

        if (table.length != 8) {
            throw new IllegalArgumentException("Table must contain exactly 8 points");
        }

        // Initialize point with table[0]
        PreComputedExtendedPoint<F2Element> point = table[0].dup();

        // Constant-time table lookup
        for (int i = 1; i < 8; i++) {
            digit--;
            // While digit>=0 mask = 0xFFFFFFFF else mask = 0x00000000
            int mask = (digit >> (8 * Integer.BYTES - 1)) - 1;

            PreComputedExtendedPoint<F2Element> tempPoint = table[i].dup();

            // Constant-time selection: If mask = 0x00000000 then point = point,
            // else if mask = 0xFFFFFFFF then point = tempPoint
            point = constantTimeSelectPoint(mask, tempPoint, point);
        }

        // Handle sign: create negated version and use constant-time selection
        PreComputedExtendedPoint<F2Element> tempPoint = createNegatedPoint(point);

        // If signMask = 0 then choose negative of the point (tempPoint),
        // else choose original point
        return constantTimeSelectPoint(signMask, point, tempPoint);
    }

    /**
     * Constant-time selection between two points
     * @param mask selection mask: if 0xFFFFFFFF select pointA, if 0x00000000 select pointB
     * @param pointA first point option
     * @param pointB second point option
     * @return selected point
     */
    private static PreComputedExtendedPoint<F2Element> constantTimeSelectPoint(
            int mask,
            PreComputedExtendedPoint<F2Element> pointA,
            PreComputedExtendedPoint<F2Element> pointB) {

        F2Element xy = constantTimeSelectF2Element(mask, pointA.xy, pointB.xy);
        F2Element yx = constantTimeSelectF2Element(mask, pointA.yx, pointB.yx);
        F2Element z2 = constantTimeSelectF2Element(mask, pointA.z, pointB.z);
        F2Element t2 = constantTimeSelectF2Element(mask, pointA.t, pointB.t);

        return new PreComputedExtendedPoint<>(xy, yx, z2, t2);
    }

    /**
     * Constant-time selection between two F2Elements
     * @param mask selection mask: if 0xFFFFFFFF select a, if 0x00000000 select b
     * @param a first F2Element option
     * @param b second F2Element option
     * @return selected F2Element
     */
    private static F2Element constantTimeSelectF2Element(int mask, F2Element a, F2Element b) {
        BigInteger realResult = constantTimeSelectBigInteger(mask, a.real, b.real);
        BigInteger imResult = constantTimeSelectBigInteger(mask, a.im, b.im);
        return new F2Element(realResult, imResult);
    }

    /**
     * Constant-time selection between two BigIntegers
     * Uses the pattern: result = (mask & (a ^ b)) ^ b
     * @param mask selection mask: if 0xFFFFFFFF select a, if 0x00000000 select b
     * @param a first BigInteger option
     * @param b second BigInteger option
     * @return selected BigInteger
     */
    private static BigInteger constantTimeSelectBigInteger(int mask, BigInteger a, BigInteger b) {
        // Convert to byte arrays for bitwise operations
        byte[] aBytes = toFixedSizeByteArray(a);
        byte[] bBytes = toFixedSizeByteArray(b);
        byte[] resultBytes = new byte[Math.max(aBytes.length, bBytes.length)];

        // Ensure arrays are same size
        int maxLen = Math.max(aBytes.length, bBytes.length);
        if (aBytes.length < maxLen) {
            byte[] temp = new byte[maxLen];
            System.arraycopy(aBytes, 0, temp, maxLen - aBytes.length, aBytes.length);
            aBytes = temp;
        }
        if (bBytes.length < maxLen) {
            byte[] temp = new byte[maxLen];
            System.arraycopy(bBytes, 0, temp, maxLen - bBytes.length, bBytes.length);
            bBytes = temp;
        }

        // Constant-time selection: result = (mask & (a ^ b)) ^ b
        for (int i = 0; i < maxLen; i++) {
            resultBytes[i] = (byte) ((mask & (aBytes[i] ^ bBytes[i])) ^ bBytes[i]);
        }

        return new BigInteger(1, resultBytes);
    }

    /**
     * Convert BigInteger to fixed-size byte array for constant-time operations
     */
    private static byte[] toFixedSizeByteArray(BigInteger value) {
        byte[] bytes = value.toByteArray();

        // Remove leading zero byte if present (due to sign bit)
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] temp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, temp, 0, temp.length);
            bytes = temp;
        }

        // Pad to field size if needed
        int targetSize = 32; // e.g., 32 for 256-bit field // TODO THis is a BIIG assumption
        if (bytes.length < targetSize) {
            byte[] temp = new byte[targetSize];
            System.arraycopy(bytes, 0, temp, targetSize - bytes.length, bytes.length);
            bytes = temp;
        }

        return bytes;
    }

    /**
     * Create negated point for sign handling
     * For point (X+Y, Y-X, 2Z, 2dT), negated point is (Y-X, X+Y, 2Z, -2dT)
     */
    private static PreComputedExtendedPoint<F2Element> createNegatedPoint(
            PreComputedExtendedPoint<F2Element> point) {

        // Swap X+Y and Y-X coordinates
        F2Element negatedXY = fp2Copy1271(point.yx);  // Y-X becomes new X+Y
        F2Element negatedYX = fp2Copy1271(point.xy);  // X+Y becomes new Y-X

        // Keep Z coordinate the same
        F2Element negatedZ2 = fp2Copy1271(point.z);

        // Negate the T coordinate
        F2Element negatedT2 = FP2.fp2Neg1271(fp2Copy1271(point.t));

        return new PreComputedExtendedPoint<>(negatedXY, negatedYX, negatedZ2, negatedT2);
    }

//    /**
//     * Constant-time table lookup to extract a point represented as (x+y,y-x,2t) corresponding to extended twisted Edwards coordinates (X:Y:Z:T) with Z=1
//     * @param table containing VPOINTS_FIXEDBASE = 2^(W_FIXEDBASE-1) points
//     * @param digit
//     * @param sign
//     * @return if sign=0 then P = table[digit], else if (sign=-1) then P = -table[digit]
//     */
//    public static PreComputedExtendedPoint<F2Element> tableLookupFixedBase(
//            PreComputedExtendedPoint<F2Element>[] table,
//            int digit,
//            int sign
//    ) {
//        PreComputedExtendedPoint<F2Element> tempPoint = null;
//        PreComputedExtendedPoint<F2Element> point = table[0].dup();               // point = table[0]
//        BigInteger mask;
//
//        int shiftAmount = 8*Integer.SIZE-1;
//
//        for (int i = 1; i < Params.VPOINTS_FIXEDBASE; i++) {
//            digit--;
//            // While digit>=0, mask = 0xFF...F else sign = 0x00...0
//            mask = BigInteger.valueOf((digit >> shiftAmount) - 1);
//            tempPoint = table[i].dup();                    // temp_point = table[i]
//
//            // If mask = 0x00...0 then point = point, else if mask = 0xFF...F then point = temp_point
//            for (int j = 0; j < Params.NWORDS_FIELD; j++) {
//                byte[] xyRealArr = point.xy.real.toByteArray();
//                byte[] xyImArr = point.xy.real.toByteArray();
//
//                xyRealArr[j] = (byte) ((mask.intValueExact() & (xyRealArr[j] ^ tempPoint.xy.real.toByteArray()[j])) ^ xyRealArr[j]);
//                xyImArr[j] = (byte) ((mask.intValueExact() & (xyImArr[j] ^ tempPoint.xy.im.toByteArray()[j])) ^ xyImArr[j]);
//
//                byte[] yxRealArr = point.yx.real.toByteArray();
//                byte[] yxImArr = point.yx.real.toByteArray();
//
//                xyRealArr[j] = (byte) ((mask.intValueExact() & (yxRealArr[j] ^ tempPoint.xy.real.toByteArray()[j])) ^ yxRealArr[j]);
//                xyImArr[j] = (byte) ((mask.intValueExact() & (yxImArr[j] ^ tempPoint.xy.im.toByteArray()[j])) ^ yxImArr[j]);
//
//                byte[] tRealArr = point.t.real.toByteArray();
//                byte[] tImArr = point.t.real.toByteArray();
//
//                xyRealArr[j] = (byte) ((mask.intValueExact() & (tRealArr[j] ^ tempPoint.xy.real.toByteArray()[j])) ^ tRealArr[j]);
//                xyImArr[j] = (byte) ((mask.intValueExact() & (tImArr[j] ^ tempPoint.xy.im.toByteArray()[j])) ^ tImArr[j]);
//
//                point = new PreComputedExtendedPoint<F2Element>(
//                        new F2Element(new BigInteger(xyRealArr), new BigInteger(xyImArr)),
//                        new F2Element(new BigInteger(yxRealArr), new BigInteger(yxImArr)),
//                        point.z,
//                        new F2Element(new BigInteger(tRealArr), new BigInteger(tImArr))
//                );
//            }
//        }
//
//        tempPoint.t = fp2Copy1271(point.t);
//        tempPoint.yx = fp2Copy1271(point.xy);                                  // point: x+y,y-x,2dt coordinate, temp_point: y-x,x+y,-2dt coordinate
//        tempPoint.xy = fp2Copy1271(point.yx);
//        fpNeg1271(tempPoint.t.real);                                            // Negate 2dt coordinate
//        fpNeg1271(tempPoint.t.im);
//
//        for (int j = 0; j < Params.NWORDS_FIELD; j++) {                                     // If sign = 0xFF...F then choose negative of the point
//            byte[] xyRealArr = point.xy.real.toByteArray();
//            byte[] xyImArr = point.xy.real.toByteArray();
//
//            xyRealArr[j] = (byte) ((sign & (xyRealArr[j] ^ tempPoint.xy.real.toByteArray()[j])) ^ xyRealArr[j]);
//            xyRealArr[j] = (byte) ((sign & (xyImArr[j] ^ tempPoint.xy.im.toByteArray()[j])) ^ xyImArr[j]);
//
//            byte[] yxRealArr = point.yx.real.toByteArray();
//            byte[] yxImArr = point.yx.real.toByteArray();
//
//            xyRealArr[j] = (byte) ((sign & (yxRealArr[j] ^ tempPoint.yx.real.toByteArray()[j])) ^ yxRealArr[j]);
//            xyRealArr[j] = (byte) ((sign & (yxImArr[j] ^ tempPoint.yx.im.toByteArray()[j])) ^ yxImArr[j]);
//
//            byte[] tRealArr = point.t.real.toByteArray();
//            byte[] tImArr = point.t.real.toByteArray();
//
//            xyRealArr[j] = (byte) ((sign & (tRealArr[j] ^ tempPoint.t.real.toByteArray()[j])) ^ tRealArr[j]);
//            xyRealArr[j] = (byte) ((sign & (tImArr[j] ^ tempPoint.t.im.toByteArray()[j])) ^ tImArr[j]);
//
//            point = new PreComputedExtendedPoint<F2Element>(
//                    new F2Element(new BigInteger(xyRealArr), new BigInteger(xyImArr)),
//                    new F2Element(new BigInteger(yxRealArr), new BigInteger(yxImArr)),
//                    point.z,
//                    new F2Element(new BigInteger(tRealArr), new BigInteger(tImArr))
//            );
//        }
//
//        return point.dup();
//    }
}
