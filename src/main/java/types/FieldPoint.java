package types;

import exceptions.TableLookupException;

public class FieldPoint implements Point {
    public F2Element x;
    public F2Element y;
    public FieldPoint(F2Element x, F2Element y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int getTableLength() throws TableLookupException {
        throw new TableLookupException("Invalid point lookup type");
    }
}
