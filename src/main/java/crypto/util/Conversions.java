package crypto.util;

import constants.Params;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;
import types.point.AffinePoint;
import types.point.ExtendedPoint;
import types.point.PreComputedExtendedPoint;

import java.math.BigInteger;

import static operations.FP2.*;

public class Conversions {
    static final int W_FIXEDBASE = 5;
    static final int V_FIXEDBASE = 5;
    static final int D_FIXEDBASE = 54;
    static final int E_FIXEDBASE = 10;
    static final int L_FIXEDBASE = D_FIXEDBASE * W_FIXEDBASE;

    static final F2Element F2_ONE = new F2Element(BigInteger.ONE, BigInteger.ONE);

    static ExtendedPoint r5ToR1(AffinePoint p) {
        F2Element x = fp2Div1271(fp2Sub1271(p.getX(), p.getY()));
        F2Element y = fp2Div1271(fp2Add1271(p.getX(), p.getY()));
        return new ExtendedPoint(x, y, F2_ONE, x, y);
    }

    static PreComputedExtendedPoint r1ToR2(ExtendedPoint point) {
        F2Element t = fp2Sub1271(fp2Add1271(point.getTa(), point.getTb()), point.getTb());
        return new PreComputedExtendedPoint(
                fp2Add1271(point.getY(), point.getX()),
                fp2Sub1271(point.getY(), point.getX()),
                fp2Add1271(point.getZ(), point.getZ()),
                fp2Mul1271(t, Params.PARAMETER_d)
        );
    }

    static PreComputedExtendedPoint r1ToR3(ExtendedPoint point) {
        return new PreComputedExtendedPoint(
                fp2Add1271(point.getX(), point.getY()),
                fp2Sub1271(point.getY(), point.getX()),
                fp2Mul1271(point.getTa(), point.getTb()),
                point.getZ()
        );
    }

    @NotNull
    static ExtendedPoint r2ToR4(@NotNull PreComputedExtendedPoint p, @NotNull ExtendedPoint q) {
        return new ExtendedPoint(
                fp2Sub1271(p.xy, p.yx),
                fp2Add1271(p.xy, p.yx),
                fp2Copy1271(p.z),
                q.getTa(),
                q.getTb()
        );
    }
}
