package crypto.primitives;

import java.util.Arrays;

import constants.PregeneratedTables;
import exceptions.TableLookupException;
import fieldoperations.FP2;
import types.data.F2Element;
import types.point.PreComputedExtendedPoint;
import types.point.TablePoint;


public class Table {
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
        //noinspection unchecked
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