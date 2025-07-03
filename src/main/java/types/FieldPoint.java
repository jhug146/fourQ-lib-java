package types;

import exceptions.TableLookupException;

public class FieldPoint<Field> implements Point {
    public Field x;
    public Field y;
    public FieldPoint(Field x, Field y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int getTableLength() throws TableLookupException {
        throw new TableLookupException("Invalid point lookup type");
    }
}
