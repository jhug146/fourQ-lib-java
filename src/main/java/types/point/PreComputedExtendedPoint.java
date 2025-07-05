package types.point;

import constants.Params;
import field.operations.FP2;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;

import java.math.BigInteger;

public class PreComputedExtendedPoint implements TablePoint{
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

    public PreComputedExtendedPoint() {
        this.xy = Params.F2_ZERO.dup();
        this.yx = Params.F2_ZERO.dup();
        this.z = Params.F2_ZERO.dup();
        this.t = Params.F2_ZERO.dup();
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

    @Override
    public AffinePoint toAffinePoint() {
        F2Element xy_f2 = xy;
        F2Element yx_f2 = yx;

        // TODO: Assuming z = 1, we can directly compute affine coordinates
        F2Element two = new F2Element(BigInteger.valueOf(2), BigInteger.ZERO);
        F2Element twoInverse = FP2.fp2Inv1271(two);

        // x = (xy - yx) / 2, y = (xy + yx) / 2
        F2Element twoX = FP2.fp2Sub1271(xy_f2, yx_f2);
        F2Element twoY = FP2.fp2Add1271(xy_f2, yx_f2);

        F2Element x = FP2.fp2Mul1271(twoX, twoInverse);
        F2Element y = FP2.fp2Mul1271(twoY, twoInverse);

        AffinePoint ret = new AffinePoint();
        ret.setX(x);
        ret.setY(y);
        ret.setT(new F2Element(BigInteger.ONE, BigInteger.ZERO));

        return ret;
    }

    @Override
    public void filterMaskForEach(
            @NotNull TablePoint tempPoint,
            @NotNull BigInteger mask,
            boolean modZ
    ) {
        xy = xy.applyMasks(tempPoint.getX(), mask);
        yx = yx.applyMasks(tempPoint.getY(), mask);
        t = t.applyMasks(tempPoint.getT(), mask);
        if (modZ) {
            z = z.applyMasks(tempPoint.getZ(), mask);
        }
    }

    @Override
    public PreComputedExtendedPoint toPreComputedExtendedPoint() {
        return this;
    }

    @Override
    public F2Element getX() {
        return xy;
    }

    @Override
    public F2Element getY() {
        return yx;
    }

    @Override
    public F2Element getT() {
        return t;
    }

    @Override
    public F2Element getZ() {
        return z;
    }

    @Override
    public void setX(F2Element x) {
        this.xy = x;
    }

    @Override
    public void setY(F2Element y) {
        this.yx = y;
    }

    @Override
    public void setT(F2Element t) {
        this.t = t;
    }

    @Override
    public void setZ(F2Element z) { this.z = z; }
}
