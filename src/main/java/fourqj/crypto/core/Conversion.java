package fourqj.crypto.core;

import fourqj.types.data.F2Element;
import fourqj.types.point.ExtendedPoint;
import fourqj.types.point.PreComputedExtendedPoint;
import fourqj.constants.Params;

import static fourqj.fieldoperations.FP2.*;

public class Conversion {
    public static PreComputedExtendedPoint r1ToR2(ExtendedPoint point) {
        F2Element t = fp2Add1271(point.getTa(), point.getTa());
        t = fp2Mul1271(t, point.getTb());

        return new PreComputedExtendedPoint(
                fp2Add1271(point.getX(), point.getY()),
                fp2Sub1271(point.getY(), point.getX()),
                fp2Add1271(point.getZ(), point.getZ()),
                fp2Mul1271(t, Params.PARAMETER_D)
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

    static ExtendedPoint r2ToR4(PreComputedExtendedPoint p, ExtendedPoint q) {
        return new ExtendedPoint(
                fp2Sub1271(p.getX(), p.getY()),
                fp2Add1271(p.getX(), p.getY()),
                fp2Copy1271(p.getZ()),
                q.getTa(),
                q.getTb()
        );
    }
}
