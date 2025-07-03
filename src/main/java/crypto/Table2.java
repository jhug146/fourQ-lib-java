package crypto;

import constants.PregeneratedTables;
import exceptions.TableLookupException;
import operations.FP2;
import types.F2Element;
import types.Point;
import types.PreComputedExtendedPoint;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

public class Table2 {
    private static LookupMode lookupMode;
    public static <T extends Point> T tableLookup(
            int tableLocation,
            int digit,
            int signMask,
            T point
    ) throws TableLookupException {
        // Bounds checks
        if (tableLocation + point.getTableLength() >= PregeneratedTables.FIXED_BASE_TABLE_POINTS.length) {
            throw new IndexOutOfBoundsException("Table offset out of bounds: " + tableLocation);
        }

        if (point.getTableLength() <= 0) {
            throw new IndexOutOfBoundsException("Table length is too short: " + point.getTableLength());
        }

        // Changes function behavior in terms of lookUp mode
        lookupMode = point.getTableLength() == 8 ? LookupMode.ONE_X_EIGHT : LookupMode.FIXED;

        // Create subset of table starting from offset
        PreComputedExtendedPoint<F2Element>[] table =
                Arrays.copyOfRange(PregeneratedTables.FIXED_BASE_TABLE_POINTS,
                        tableLocation,
                        tableLocation + point.getTableLength()
                );

        PreComputedExtendedPoint<F2Element> tempPoint = null;
        //PreComputedExtendedPoint<F2Element> point = table[0];
        final int shiftAmount = Integer.SIZE - 1;

        for (int i = 1; i < point.getTableLength(); i++) {
            digit--;
            BigInteger mask = BigInteger.valueOf((digit >> shiftAmount) - 1);   // TODO: Could be wrong
            tempPoint = table[i];

            point.filterMaskForEach(tempPoint, mask,true);
        }

        tempPoint.t = point.getT().dup();
        tempPoint.yx = point.getX();
        tempPoint.xy = point.getY();
        tempPoint.t = FP2.fp2Neg1271(tempPoint.t);

        BigInteger bigMask = BigInteger.valueOf(signMask);     // TODO: Potential conversion problem here
        point.filterMaskForEach(tempPoint, bigMask, false);

        return point;
    }

    private enum LookupMode {
        ONE_X_EIGHT,
        FIXED
    }
}

