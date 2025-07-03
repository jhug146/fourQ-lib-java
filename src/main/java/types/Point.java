package types;

import exceptions.TableLookupException;

import java.math.BigInteger;
import java.util.Iterator;

public interface Point {
    int getTableLength() throws TableLookupException;

    F2Element getX();
    F2Element getY();
    F2Element getZ();
    F2Element getT();

    void setX(F2Element x);
    void setY(F2Element y);
    void setZ(F2Element z);
    void setT(F2Element t);

    void filterMaskForEach(
            PreComputedExtendedPoint tempPoint,
            BigInteger mask,
            boolean modifyZ
    );
}
