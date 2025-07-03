package types;

import constants.Params;
import operations.FP2;

import java.math.BigInteger;

public class PreComputedExtendedPoint implements Point{
    public F2Element xy;
    public F2Element yx;
    public F2Element z;
    public F2Element t;
    public PreComputedExtendedPoint(F2Element _xy, F2Element _yx, F2Element _z, F2Element _t) {
        xy = _xy;
        yx = _yx;
        z = _z;
        t = _t;
    }

    @Override
    public int getTableLength() {
        return Params.PRE_COMPUTE_TABLE_LENGTH;
    }

    public PreComputedExtendedPoint dup() {
        return new PreComputedExtendedPoint(
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
    public AffinePoint toAffinePoint() {
        F2Element xy_f2 = xy;
        F2Element yx_f2 = yx;

        // Assuming z = 1, we can directly compute affine coordinates
        F2Element two = new F2Element(BigInteger.valueOf(2), BigInteger.ZERO);
        F2Element twoInverse = FP2.fp2Inv1271(two);

        // x = (xy - yx) / 2, y = (xy + yx) / 2
        F2Element twoX = FP2.fp2Sub1271(xy_f2, yx_f2);
        F2Element twoY = FP2.fp2Add1271(xy_f2, yx_f2);

        F2Element x = FP2.fp2Mul1271(twoX, twoInverse);
        F2Element y = FP2.fp2Mul1271(twoY, twoInverse);

        AffinePoint ret = new AffinePoint();
        ret.x = x;
        ret.y = y;

        return ret;
    }

    public void filterMaskForEach(
            PreComputedExtendedPoint tempPoint,
            BigInteger mask,
            boolean modifyZ
    ) {
        xy = xy.applyMasks(tempPoint.xy, mask);
        yx = yx.applyMasks(tempPoint.yx, mask);
        z = !modifyZ ? z : z.applyMasks(tempPoint.t, mask);
    }

    @Override
    public F2Element getX() {
        return (F2Element) xy;
    }

    public void setX(F2Element x) {
        this.xy = (F2Element) x;
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
        this.yx = (F2Element) y;
    }

    @Override
    public void setZ(F2Element z) {

    }

    @Override
    public F2Element getT() {
        return t;
    }

    public void setT(F2Element t) {
        this.t = t;
    }
}
