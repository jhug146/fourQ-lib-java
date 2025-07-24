package fourqj.types.point;

import fourqj.types.data.F2Element;


public interface TablePoint extends Point {
    F2Element getT();

    void setT(F2Element t);

    TablePoint dup();
}
