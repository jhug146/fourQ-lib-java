package types.point;

import types.data.F2Element;

public interface Point {
    F2Element getX();
    F2Element getY();

    void setX(F2Element x);
    void setY(F2Element y);
}
