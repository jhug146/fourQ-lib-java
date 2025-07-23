package crypto.primitives;

import fieldoperations.FP2;
import types.data.F2Element;
import types.point.TablePoint;

public class Table {
    public static <T extends TablePoint> T tableLookup(
            T[] table,
            int digit,
            int signMask
    ) {
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
}