import types.AffinePoint;
import types.ExtendedPoint;
import types.F2Elem;
import types.FieldPoint;

import java.math.BigInteger;


public class ECCUtil {
    private static final int W_FIXEDBASE = 5;
    private static final int V_FIXEDBASE = 5;
    private static final int D_FIXEDBASE = 54;
    private static final int E_FIXEDBASE = 10;

    private static final Table TABLE = new Table();

    static FieldPoint<F2Elem> eccMulFixed(BigInteger val) {
        BigInteger temp = FP.moduloOrder(val);
        temp = FP.conversionToOdd(temp);
        int[] digits = mLSBSetRecode(temp);  // TODO: No idea how this works
        int digit = digits[W_FIXEDBASE * D_FIXEDBASE - 1];
        int startI = (W_FIXEDBASE - 1) * D_FIXEDBASE - 1;
        for (int i = startI; i >= 2 * D_FIXEDBASE - 1; i -= D_FIXEDBASE) {
            digit = 2 * digit + digits[i];
        }

        // TODO: Both instances of TABLE in this function might need updating
        AffinePoint<F2Elem> point = TableLookup.tableLookupFixedBase(TABLE, digit, digits[D_FIXEDBASE - 1]);
        ExtendedPoint<F2Elem> exPoint = R5_To_R1(point);

        for (int j = 0; j < V_FIXEDBASE - 1; j++) {
            digit = digits[W_FIXEDBASE * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1];
            int iStart = (W_FIXEDBASE - 1) * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            int iMin = 2 * D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            for (int i = iStart; i >= iMin; i -= D_FIXEDBASE) {
                digit = 2 * digit + digits[i];
            }
            // Extract point in (x+y,y-x,2dt) representation
            int signDigit = D_FIXEDBASE - (j + 1) * E_FIXEDBASE - 1;
            point = TableLookup.tableLookupFixedBase(TABLE, digit, digits[signDigit]);
            exPoint = eccMixedAdd(point, exPoint);
        }

        for (int i = E_FIXEDBASE - 2; i >= 0; i--) {
            exPoint = eccDouble(exPoint);
            for (int j = 0; j < V_FIXEDBASE; j++) {
                digit = digits[W_FIXEDBASE * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE];
                int kStart = (W_FIXEDBASE - 1) * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                int kMin = 2 * D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                for (int k = kStart; k >= kMin; k -= D_FIXEDBASE) {
                    digit = 2 * digit + digits[k];
                }
                int signDigit = D_FIXEDBASE - j * E_FIXEDBASE + i - E_FIXEDBASE;
                point = TableLookup.tableLookupFixedBase(TABLE, digit, signDigit);
                exPoint = eccMixedAdd(point, exPoint);
            }
        }
        return eccNorm(exPoint);
    }

    static BigInteger encode(FieldPoint<F2Elem> point) {
        if (point.x.isZero()) {
            int temp = ;
        } else {
            int temp =;
        }

    }

    static int[] mLSBSetRecode(BigInteger scalar) {}

    static ExtendedPoint<F2Elem> R5_To_R1(AffinePoint<F2Elem> P) {}

    static ExtendedPoint<F2Elem> eccMixedAdd(AffinePoint<F2Elem> Q, ExtendedPoint<F2Elem> P) {}

    static ExtendedPoint<F2Elem> eccDouble(ExtendedPoint<F2Elem> P) {}

    static FieldPoint<F2Elem> eccNorm(ExtendedPoint<F2Elem> P) {}
}
