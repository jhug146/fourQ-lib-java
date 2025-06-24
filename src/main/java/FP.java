import types.Key;
import types.Pair;

import java.math.BigInteger;

public class FP {
    private static final int NWORDS_ORDER = 8;
    static Key moduloOrder(Key key) {
        Key temp = montgomeryMultiplyModOrder(key, Constants.MONTGOMERY_R_PRIME);
        return montgomeryMultiplyModOrder(temp, Constants.ONE);
    }

    static Key montgomeryMultiplyModOrder(Key a, Key b) {
        Key p = multiply(a, b);
        Key q = multiply(p, Constants.MONTGOMERY_R_PRIME);
        Key returnEnd = multiply(q, Constants.CURVE_ORDER);

        Pair<Key, Integer> result = mpAdd(p, returnEnd);
        returnEnd = result.first;
        int cout = result.second;

        Key returnVal = new Key(returnEnd.key.shiftRight(NWORDS_ORDER), 2 * NWORDS_ORDER);
        Pair<Key, Integer> result2 = mpSubtract(returnVal, Constants.CURVE_ORDER);
        returnVal = new Key(returnVal.key.add(result2.first.key), 2 * NWORDS_ORDER);
        Integer bout = result2.second;
        int mask = cout - bout;

        returnEnd = new Key(
                returnEnd.key.add(Constants.CURVE_ORDER.key.and(BigInteger.valueOf(mask))),
                2 * NWORDS_ORDER
        );
        return new Key(returnVal.key.add(returnEnd.key), 2 * NWORDS_ORDER);
    }

    static Key subtractModOrder(Key a, Key b) {}

    static Key conversionToOdd(Key key) {}

    static Key multiply(Key a, Key b) {}

    static Pair<Key, Integer> mpAdd(Key a, Key b) {}

    static Pair<Key, Integer> mpSubtract(Key a, Key b) {}
}
