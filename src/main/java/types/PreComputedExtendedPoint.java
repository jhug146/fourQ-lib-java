package types;

import constants.Params;
import operations.FP2;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Optional;

public class PreComputedExtendedPoint<Field> implements Point<Field>{
    public Field xy;
    public Field yx;
    public Field z;
    public Field t;
    public PreComputedExtendedPoint(Field _xy, Field _yx, Field _z, Field _t) {
        xy = _xy;
        yx = _yx;
        z = _z;
        t = _t;
    }

    @Override
    public int getTableLength() {
        return Params.PRE_COMPUTE_TABLE_LENGTH;
    }

    public PreComputedExtendedPoint<Field> dup() {
        return new PreComputedExtendedPoint<Field>(
              this.xy,
              this.yx,
              this.z,
              this.t
        );
    }

    /**
     * Simplified version assuming z = 1 (normalized coordinates)
     *  Often the case for precomputed table values
     */
    public AffinePoint<Field> toAffinePoint() {
        if (!(xy instanceof F2Element)) {
            throw new UnsupportedOperationException("Only F2Element supported");
        }

        F2Element xy_f2 = (F2Element) xy;
        F2Element yx_f2 = (F2Element) yx;

        // Assuming z = 1, we can directly compute affine coordinates
        F2Element two = new F2Element(BigInteger.valueOf(2), BigInteger.ZERO);
        F2Element twoInverse = FP2.fp2Inv1271(two);

        // x = (xy - yx) / 2, y = (xy + yx) / 2
        F2Element twoX = FP2.fp2Sub1271(xy_f2, yx_f2);
        F2Element twoY = FP2.fp2Add1271(xy_f2, yx_f2);

        F2Element x = FP2.fp2Mul1271(twoX, twoInverse);
        F2Element y = FP2.fp2Mul1271(twoY, twoInverse);

        AffinePoint<Field> ret = new AffinePoint<>();
        ret.x = (Field) x;
        ret.y = (Field) y;

        return ret;
    }

    public void filterMaskForEach(
            PreComputedExtendedPoint<F2Element> tempPoint,
            BigInteger mask,
            boolean modifyZ
    ) {
        xy = (Field) ((F2Element) xy).applyMasks(tempPoint.xy, mask);
        yx = (Field) ((F2Element) yx).applyMasks(tempPoint.yx, mask);
        z = (Field) (!modifyZ ? (F2Element) z : ((F2Element) z).applyMasks(tempPoint.t, mask));
        t = t;
    }

    @Override
    public F2Element getX() {
        return (F2Element) xy;
    }

    public void setX(F2Element x) {
        this.xy = (Field) x;
    }

    @Override
    public F2Element getY() {
        return (F2Element) yx;
    }

    @Override
    public F2Element getZ() {
        return null;
    }

    public void setY(F2Element y) {
        this.yx = (Field) y;
    }

    @Override
    public void setZ(F2Element z) {

    }

    @Override
    public F2Element getT() {
        return (F2Element) t;
    }

    public void setT(F2Element t) {
        this.t = (Field) t;
    }
}
