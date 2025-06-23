import types.Key;

public class FP {
    static Key moduloOrder(Key key) {
        Key temp = montgomeryMultiplyModOrder(key, Constants.MONTGOMERY_R_PRIME);
        return montgomeryMultiplyModOrder(temp, Constants.ONE);
    }

    static Key montgomeryMultiplyModOrder(Key key, Key order) {}

    static Key subtractModOrder(Key a, Key b) {}

    static Key conversionToOdd(Key key) {}
}
