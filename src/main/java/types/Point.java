package types;

import exceptions.TableLookupException;

public interface Point {
    int getTableLength() throws TableLookupException;
}
