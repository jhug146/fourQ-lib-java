package types.point;

import org.jetbrains.annotations.NotNull;

import types.data.F2Element;


public interface TablePoint extends Point {
    @NotNull
    F2Element getT();

    void setT(@NotNull F2Element t);

    @NotNull
    TablePoint dup();
}
