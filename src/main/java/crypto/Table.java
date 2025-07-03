package crypto;

import constants.PregeneratedTables;
import operations.FP2;
import types.F2Element;
import types.Point;
import types.PreComputedExtendedPoint;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

public class Table {
    public static <T extends Point> T tableLookup(
            int tableLocation,
            int digit,
            int signMask
    ) {

        // Bounds checks
        final int tableLength = ;
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

            point.filterMaskForEach(tempPoint, mask, Optional.of(Boolean.TRUE));
        }

        tempPoint.t = point.t.dup();
        tempPoint.yx = point.xy;
        tempPoint.xy = point.yx;
        tempPoint.t = FP2.fp2Neg1271(tempPoint.t);

        BigInteger bigMask = BigInteger.valueOf(signMask);     // TODO: Potential conversion problem here
        point.filterMaskForEach(tempPoint, bigMask, Optional.empty());

        return point;
    }
}
