package crypto.core;

import constants.Params;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;
import types.point.AffinePoint;
import types.point.ExtendedPoint;
import types.point.PreComputedExtendedPoint;

import static field.operations.FP2.*;

public class Conversion {
    static ExtendedPoint r5ToR1(AffinePoint p) {
        F2Element x = fp2Div1271(fp2Sub1271(p.getX(), p.getY()));
        F2Element y = fp2Div1271(fp2Add1271(p.getX(), p.getY()));
        return new ExtendedPoint(x, y, F2Element.ONE, x, y);
    }

    public static PreComputedExtendedPoint r1ToR2(ExtendedPoint point) {
        F2Element t = fp2Add1271(point.getTa(), point.getTa());
        t = fp2Mul1271(t, point.getTb());
        return new PreComputedExtendedPoint(
                fp2Add1271(point.getX(), point.getY()),
                fp2Sub1271(point.getY(), point.getX()),
                fp2Add1271(point.getZ(), point.getZ()),
                fp2Mul1271(t, Params.PARAMETER_d)
        );
    }

    public static PreComputedExtendedPoint r1ToR3(ExtendedPoint point) {
        return new PreComputedExtendedPoint(
                fp2Add1271(point.getX(), point.getY()),
                fp2Sub1271(point.getY(), point.getX()),
                point.getZ(),
                fp2Mul1271(point.getTa(), point.getTb())
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
