package crypto.primitives;

import field.operations.FP2;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;
import types.point.TablePoint;


public class Table {
    @NotNull
    public static <T extends TablePoint> T tableLookup(
            @NotNull T[] table,
            int digit,
            int signMask
    ) throws NullPointerException {
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