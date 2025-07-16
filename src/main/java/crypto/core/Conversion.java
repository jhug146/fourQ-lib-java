package crypto.core;

import org.jetbrains.annotations.NotNull;

import types.data.F2Element;
import types.point.ExtendedPoint;
import types.point.PreComputedExtendedPoint;
import constants.Params;

import static fieldoperations.FP2.*;

public class Conversion {
    @NotNull
    public static PreComputedExtendedPoint r1ToR2(@NotNull ExtendedPoint point) {
        F2Element t = fp2Add1271(point.getTa(), point.getTa());
        t = fp2Mul1271(t, point.getTb());

        return new PreComputedExtendedPoint(
                fp2Add1271(point.getX(), point.getY()),
                fp2Sub1271(point.getY(), point.getX()),
                fp2Add1271(point.getZ(), point.getZ()),
                fp2Mul1271(t, Params.PARAMETER_D)
        );
    }

    @NotNull
    public static PreComputedExtendedPoint r1ToR3(@NotNull ExtendedPoint point) {
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
                fp2Sub1271(p.getX(), p.getY()),
                fp2Add1271(p.getX(), p.getY()),
                fp2Copy1271(p.getZ()),
                q.getTa(),
                q.getTb()
        );
    }
}
