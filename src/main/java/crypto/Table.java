package crypto;

import java.math.BigInteger;
import java.util.Arrays;

import constants.PregeneratedTables;
import exceptions.TableLookupException;
import operations.FP2;
import types.TablePoint;


public class Table {
    public static TablePoint tableLookup(
            TablePoint[] table,
            int digit,
            int signMask
    ) throws TableLookupException {
        TablePoint tempPoint = null;
        TablePoint point = table[0];
        final int shiftAmount = Integer.SIZE - 1;

        for (int i = 1; i < point.getTableLength(); i++) {
            digit--;
            BigInteger mask = BigInteger.valueOf((digit >> shiftAmount) - 1);   // TODO: Could be wrong
            tempPoint = table[i];

            point.filterMaskForEach(tempPoint, mask,true);
        }

        assert tempPoint != null;
        tempPoint.setT(point.getT().dup());
        tempPoint.setY(point.getX().dup());
        tempPoint.setX(point.getY().dup());
        tempPoint.setT(FP2.fp2Neg1271(tempPoint.getT()));

        BigInteger bigMask = BigInteger.valueOf(signMask);     // TODO: Potential conversion problem here
        point.filterMaskForEach(tempPoint, bigMask, false);
        return point;
    }

    public static TablePoint tableLookup(
            int tableLocation,
            int digit,
            int signMask,
            TablePoint point
    ) throws TableLookupException {
        if (tableLocation + point.getTableLength() >= PregeneratedTables.FIXED_BASE_TABLE_POINTS.length) {
            throw new IndexOutOfBoundsException("Table location out of bounds: " + tableLocation);
        }

        // Create subset of table starting from offset
        TablePoint[] table =
                Arrays.copyOfRange(PregeneratedTables.FIXED_BASE_TABLE_POINTS,
                        tableLocation,
                        tableLocation + point.getTableLength()
                );

        return tableLookup(
              table,
                digit,
                signMask
        );
    }
}