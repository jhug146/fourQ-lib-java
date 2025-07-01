package crypto;

import constants.Params;
import exceptions.TableLookupException;
import org.jetbrains.annotations.NotNull;
import types.F2Element;
import types.PreComputedExtendedPoint;

import java.math.BigInteger;

import static operations.FP.PUtil.fpNeg1271;
import static operations.FP2.fp2Copy1271;

public class Table {
    /**
     * Constant-time table lookup to extract a point represented as (X+Y,Y-X,2Z,2dT) corresponding to extended twisted Edwards coordinates (X:Y:Z:T)
     * @param table containing 8 points
     * @param digits
     * @param sign_masks
     * @return = sign*table[digit], where sign=1 if sign_mask=0xFF...FF and sign=-1 if sign_mask=0
     */
    @NotNull
    public static PreComputedExtendedPoint<F2Element> tableLookup1x8(
            PreComputedExtendedPoint<F2Element>[] table,
            int digits,
            int sign_masks
    ) throws TableLookupException {
        throw new TableLookupException("");
    }

    // Constant-time table lookup to extract a point represented as (x+y,y-x,2t) corresponding to extended twisted Edwards coordinates (X:Y:Z:T) with Z=1
    // Inputs: sign, digit, table containing VPOINTS_FIXEDBASE = 2^(W_FIXEDBASE-1) points
    // Output: if sign=0 then P = table[digit], else if (sign=-1) then P = -table[digit]
    @NotNull
    public static PreComputedExtendedPoint<F2Element> tableLookupFixedBase(
            PreComputedExtendedPoint<F2Element>[] table,
            int digit,
            int sign
    ) {
        PreComputedExtendedPoint<F2Element> tempPoint = null;
        PreComputedExtendedPoint<F2Element> point = table[0].dup();               // point = table[0]
        BigInteger mask;

        int shiftAmount = 8*Integer.SIZE-1;

        for (int i = 1; i < Params.VPOINTS_FIXEDBASE; i++) {
            digit--;
            // While digit>=0, mask = 0xFF...F else sign = 0x00...0
            mask = BigInteger.valueOf((digit >> shiftAmount) - 1);
            tempPoint = table[i].dup();                    // temp_point = table[i]

            // If mask = 0x00...0 then point = point, else if mask = 0xFF...F then point = temp_point
            for (int j = 0; j < Params.NWORDS_FIELD; j++) {
                byte[] xyRealArr = point.xy.real.toByteArray();
                byte[] xyImArr = point.xy.real.toByteArray();

                xyRealArr[j] = (byte) ((mask.intValueExact() & (xyRealArr[j] ^ tempPoint.xy.real.toByteArray()[j])) ^ xyRealArr[j]);
                xyImArr[j] = (byte) ((mask.intValueExact() & (xyImArr[j] ^ tempPoint.xy.im.toByteArray()[j])) ^ xyImArr[j]);

                byte[] yxRealArr = point.yx.real.toByteArray();
                byte[] yxImArr = point.yx.real.toByteArray();

                xyRealArr[j] = (byte) ((mask.intValueExact() & (yxRealArr[j] ^ tempPoint.xy.real.toByteArray()[j])) ^ yxRealArr[j]);
                xyImArr[j] = (byte) ((mask.intValueExact() & (yxImArr[j] ^ tempPoint.xy.im.toByteArray()[j])) ^ yxImArr[j]);

                byte[] tRealArr = point.t.real.toByteArray();
                byte[] tImArr = point.t.real.toByteArray();

                xyRealArr[j] = (byte) ((mask.intValueExact() & (tRealArr[j] ^ tempPoint.xy.real.toByteArray()[j])) ^ tRealArr[j]);
                xyImArr[j] = (byte) ((mask.intValueExact() & (tImArr[j] ^ tempPoint.xy.im.toByteArray()[j])) ^ tImArr[j]);

                point = new PreComputedExtendedPoint<>(
                        new F2Element(new BigInteger(xyRealArr), new BigInteger(xyImArr)),
                        new F2Element(new BigInteger(yxRealArr), new BigInteger(yxImArr)),
                        point.z,
                        new F2Element(new BigInteger(tRealArr), new BigInteger(tImArr))
                );
            }
        }

        tempPoint.t = fp2Copy1271(point.t);
        tempPoint.yx = fp2Copy1271(point.xy);                                  // point: x+y,y-x,2dt coordinate, temp_point: y-x,x+y,-2dt coordinate
        tempPoint.xy = fp2Copy1271(point.yx);
        fpNeg1271(tempPoint.t.real);                                            // Negate 2dt coordinate
        fpNeg1271(tempPoint.t.im);

        for (int j = 0; j < Params.NWORDS_FIELD; j++) {                                     // If sign = 0xFF...F then choose negative of the point
            byte[] xyRealArr = point.xy.real.toByteArray();
            byte[] xyImArr = point.xy.real.toByteArray();

            xyRealArr[j] = (byte) ((sign & (xyRealArr[j] ^ tempPoint.xy.real.toByteArray()[j])) ^ xyRealArr[j]);
            xyRealArr[j] = (byte) ((sign & (xyImArr[j] ^ tempPoint.xy.im.toByteArray()[j])) ^ xyImArr[j]);

            byte[] yxRealArr = point.yx.real.toByteArray();
            byte[] yxImArr = point.yx.real.toByteArray();

            xyRealArr[j] = (byte) ((sign & (yxRealArr[j] ^ tempPoint.yx.real.toByteArray()[j])) ^ yxRealArr[j]);
            xyRealArr[j] = (byte) ((sign & (yxImArr[j] ^ tempPoint.yx.im.toByteArray()[j])) ^ yxImArr[j]);

            byte[] tRealArr = point.t.real.toByteArray();
            byte[] tImArr = point.t.real.toByteArray();

            xyRealArr[j] = (byte) ((sign & (tRealArr[j] ^ tempPoint.t.real.toByteArray()[j])) ^ tRealArr[j]);
            xyRealArr[j] = (byte) ((sign & (tImArr[j] ^ tempPoint.t.im.toByteArray()[j])) ^ tImArr[j]);

            point = new PreComputedExtendedPoint<>(
                    new F2Element(new BigInteger(xyRealArr), new BigInteger(xyImArr)),
                    new F2Element(new BigInteger(yxRealArr), new BigInteger(yxImArr)),
                    point.z,
                    new F2Element(new BigInteger(tRealArr), new BigInteger(tImArr))
            );
        }

        return point.dup();
    }
}
