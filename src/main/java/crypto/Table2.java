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

    public static PreComputedExtendedPoint<F2Element> tableLookup1x8(
            int tableLocation,
            int digit,
            int signMask
    ) {

        // Bounds checks
        if (tableLocation >= PregeneratedTables.FIXED_BASE_TABLE_POINTS.length) {
            throw new IndexOutOfBoundsException("Table offset out of bounds: " + tableLocation);
        }

        // Create subset of table starting from offset
        PreComputedExtendedPoint<F2Element>[] table =
                Arrays.copyOfRange(PregeneratedTables.FIXED_BASE_TABLE_POINTS,
                        tableLocation,
                        Math.min(tableLocation + Params.VPOINTS_FIXEDBASE, PregeneratedTables.FIXED_BASE_TABLE_POINTS.length));

        PreComputedExtendedPoint<F2Element> tempPoint = null;
        PreComputedExtendedPoint<F2Element> point = table[0];
        final int shiftAmount = Integer.SIZE - 1;

        for (int i = 1; i < TABLE_LOOKUP_SIZE; i++) {
            digit--;
            BigInteger mask = BigInteger.valueOf((digit >> shiftAmount) - 1);   // TODO: Could be wrong
            tempPoint = table[i];

            point.xy.real = mask.and(point.xy.real.xor(tempPoint.xy.real)).xor(point.xy.real);
            point.xy.im = mask.and(point.xy.im.xor(tempPoint.xy.im)).xor(point.xy.im);
            point.yx.real = mask.and(point.yx.real.xor(tempPoint.yx.real)).xor(point.yx.real);
            point.yx.im = mask.and(point.yx.im.xor(tempPoint.yx.im)).xor(point.yx.im);
            point.z.real = mask.and(point.z.real.xor(tempPoint.z.real)).xor(point.z.real);
            point.z.im = mask.and(point.z.im.xor(tempPoint.z.im)).xor(point.z.im);
            point.t.real = mask.and(point.t.real.xor(tempPoint.t.real)).xor(point.t.real);
            point.t.im = mask.and(point.t.im.xor(tempPoint.t.im)).xor(point.t.im);
        }
        tempPoint.t = point.t.dup();
        tempPoint.yx = point.xy;
        tempPoint.xy = point.yx;
        tempPoint.t = FP2.fp2Neg1271(tempPoint.t);

        BigInteger bigMask = BigInteger.valueOf(signMask);     // TODO: Potential conversion problem here
        point.xy.real = bigMask.and(point.xy.real.xor(tempPoint.xy.real)).xor(tempPoint.xy.real);   // TODO: Reduce code duplication in this function
        point.xy.im = bigMask.and(point.xy.im.xor(tempPoint.xy.im)).xor(tempPoint.xy.im);
        point.yx.real = bigMask.and(point.yx.real.xor(tempPoint.yx.real)).xor(tempPoint.yx.real);
        point.yx.im = bigMask.and(point.yx.im.xor(tempPoint.yx.im)).xor(tempPoint.yx.im);
        point.t.real = bigMask.and(point.t.real.xor(tempPoint.t.real)).xor(tempPoint.t.real);
        point.t.im = bigMask.and(point.t.im.xor(tempPoint.t.im)).xor(tempPoint.t.im);
        return point;

    }

    public static PreComputedExtendedPoint<F2Element> tableLookupFixedBase(
            int tableLocation,
            int digit,
            int sign
    ) {}
}
