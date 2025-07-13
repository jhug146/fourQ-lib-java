package types.point;

import java.math.BigInteger;

import exceptions.TableLookupException;
import types.data.F2Element;


public interface TablePoint extends Point {
    F2Element getT();
    F2Element getZ();

    void setT(F2Element t);
    void setZ(F2Element z);

    int getTableLength() throws TableLookupException;

    TablePoint dup();

    PreComputedExtendedPoint toPreComputedExtendedPoint();
    AffinePoint toAffinePoint();
}
