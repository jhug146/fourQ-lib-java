package types;

import java.math.BigInteger;

import exceptions.TableLookupException;


public interface TablePoint extends Point {
    F2Element getT();
    F2Element getZ();

    void setT(F2Element t);
    void setZ(F2Element z);

    int getTableLength() throws TableLookupException;

    void filterMaskForEach(TablePoint tempPoint, BigInteger mask, boolean modZ);

    PreComputedExtendedPoint toPreComputedExtendedPoint();
    AffinePoint toAffinePoint();
}
