package crypto;

import constants.Params;
import constants.PregeneratedTables;
import operations.FP2;
import types.F2Element;
import types.PreComputedExtendedPoint;

import java.math.BigInteger;
import java.util.Arrays;

public class Table2 {
    private static final int TABLE_LOOKUP_SIZE = 8;

    public static PreComputedExtendedPoint<F2Element> tableLookup(
            int tableLocation,
            int tableLength,
            int digit,
            int signMask
    ) {

        // Bounds checks
        if (tableLocation + tableLength >= PregeneratedTables.FIXED_BASE_TABLE_POINTS.length) {
            throw new IndexOutOfBoundsException("Table offset out of bounds: " + tableLocation);
        }

        if (tableLength <= 0) {
            throw new IndexOutOfBoundsException("Table length is too short: " + tableLength);
        }

        // Create subset of table starting from offset
        PreComputedExtendedPoint<F2Element>[] table =
                Arrays.copyOfRange(PregeneratedTables.FIXED_BASE_TABLE_POINTS,
                        tableLocation,
                        tableLocation + tableLength
                );

        PreComputedExtendedPoint<F2Element> tempPoint = null;
        PreComputedExtendedPoint<F2Element> point = table[0];
        final int shiftAmount = Integer.SIZE - 1;

        for (int i = 1; i < tableLength; i++) {
            digit--;
            BigInteger mask = BigInteger.valueOf((digit >> shiftAmount) - 1);   // TODO: Could be wrong
            tempPoint = table[i];

            point.xy = applyMasks(point.xy, tempPoint.xy, mask);
            point.yx = applyMasks(point.yx, tempPoint.yx, mask);
            point.z = applyMasks(point.z, tempPoint.z, mask);
            point.t = applyMasks(point.t, tempPoint.t, mask);
        }
        tempPoint.t = point.t.dup();
        tempPoint.yx = point.xy;
        tempPoint.xy = point.yx;
        tempPoint.t = FP2.fp2Neg1271(tempPoint.t);

        BigInteger bigMask = BigInteger.valueOf(signMask);     // TODO: Potential conversion problem here
        point.xy = applyMasks(point.xy, tempPoint.xy, bigMask);
        point.yx = applyMasks(point.yx, tempPoint.yx, bigMask);
        point.t = applyMasks(point.t, tempPoint.t, bigMask);
        return point;

    }

    private static F2Element applyMasks(F2Element point1, F2Element point2, BigInteger mask) {
        return new F2Element(
                mask.and(point1.real.xor(point2.real)).xor(point1.real),
                mask.and(point1.im.xor(point2.im)).xor(point1.im)
        );
    }
}
