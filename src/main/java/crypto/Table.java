package crypto;

import java.math.BigInteger;
import java.util.Arrays;

import constants.PregeneratedTables;
import exceptions.TableLookupException;
import field.operations.FP2;
import types.data.F2Element;
import types.point.PreComputedExtendedPoint;
import types.point.TablePoint;


public class Table {
    /*
    public static <T extends TablePoint> T tableLookup(
            T[] table,
            int digit,
            int signMask
    ) throws TableLookupException, NullPointerException {
        TablePoint tempPoint = null;
        T point = table[0];
        final int shiftAmount = Integer.SIZE - 1;

        for (int i = 1; i < point.getTableLength(); i++, digit--) {
            BigInteger mask = BigInteger.valueOf((digit >> shiftAmount) - 1);   // TODO: Could be wrong
            tempPoint = table[i];

            point.filterMaskForEach(tempPoint, mask, true);
        }

        if (tempPoint == null) {
            throw new TableLookupException("""
                    TableLookup expected table to provide non-null value.
                    Likely cause: generated table is faulty.
                    """);
        }

        tempPoint.setT(point.getT().dup());
        tempPoint.setY(point.getX().dup());
        tempPoint.setX(point.getY().dup());
        tempPoint.setT(FP2.fp2Neg1271(tempPoint.getT()));

        BigInteger bigMask = BigInteger.valueOf(signMask);     // TODO: Potential conversion problem here
        point.filterMaskForEach(tempPoint, bigMask, false);
        return point;
    }*/

    public static <T extends TablePoint> T tableLookup(
            T[] table,
            int digit,
            int signMask
    ) throws TableLookupException, NullPointerException {
        if (table[digit] == null) {
            throw new TableLookupException("""
                    TableLookup expected table to provide non-null value.
                    Likely cause: generated table is faulty.
                    """);
        }
        T point = (T) table[digit].dup();
        if (signMask == -1) {
            return point;
        }
        F2Element tempY = point.getX().dup();
        point.setX(point.getY().dup());
        point.setY(tempY);
        point.setT(FP2.fp2Neg1271(point.getT()));
        return point;
    }

    public static PreComputedExtendedPoint tableLookup(
            int tableLocation,
            int digit,
            int signMask,
            TablePoint point
    ) throws TableLookupException {
        if (tableLocation + point.getTableLength() >= PregeneratedTables.FIXED_BASE_TABLE_POINTS.length) {
            throw new IndexOutOfBoundsException("Table location out of bounds: " + tableLocation);
        }

        PreComputedExtendedPoint[] table = Arrays.copyOfRange(
                PregeneratedTables.FIXED_BASE_TABLE_POINTS,
                tableLocation,
                tableLocation + point.getTableLength()
        );

        return tableLookup(table, digit, signMask);
    }
}