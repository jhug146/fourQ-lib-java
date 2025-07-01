import types.AffinePoint;
import types.F2Element;
import types.PreComputedExtendedPoint;

public class Table {
    public static AffinePoint<F2Element> tableLookupFixedBase(int digit, int sign) {
        return null;
    }

    /**
     * Constant-time table lookup to extract a point represented as (X+Y,Y-X,2Z,2dT) corresponding to extended twisted Edwards coordinates (X:Y:Z:T)
     * @param table containing 8 points
     * @param digits
     * @param sign_masks
     * @return = sign*table[digit], where sign=1 if sign_mask=0xFF...FF and sign=-1 if sign_mask=0
     */
    public static PreComputedExtendedPoint<F2Element> tableLookup1x8(
            PreComputedExtendedPoint<F2Element>[] table,
            int digits,
            int sign_masks
    ) {
        return null;
    }
}
