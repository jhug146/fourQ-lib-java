import types.FieldPoint;
import types.Key;
import types.Digit;

public class ECCUtil {
    private static final int W_FIXEDBASE = 5;
    private static final int V_FIXEDBASE = 5;
    private static final int D_FIXEDBASE = 5;    // TODO: Fix D and E values
    private static final int E_FIXEDBASE = 5;


    static FieldPoint<Integer> eccMulFixed(Key key) {
        Key temp = FP.moduloOrder(key);
        temp = FP.conversionToOdd(temp);
        int[] digits = mLSBSetRecode(temp);  // TODO: No idea how this works
        int digit = digits[W_FIXEDBASE * D_FIXEDBASE - 1];
        int startI = (W_FIXEDBASE - 1) * D_FIXEDBASE - 1;
        for (int i = startI; i >= 2 * D_FIXEDBASE - 1; i = i - D_FIXEDBASE) {
            digit = 2 * digit + digits[i];
        }


    }

    static Key encode(FieldPoint<Integer> point) {}

    static int[] mLSBSetRecode(Key scalar) {}
}
