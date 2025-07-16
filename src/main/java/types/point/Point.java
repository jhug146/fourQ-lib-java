package types.point;

import org.jetbrains.annotations.NotNull;

import types.data.F2Element;


public interface Point {
    @NotNull
    F2Element getX();

    void setX(@NotNull F2Element x);

    @NotNull
    F2Element getY();

    void setY(@NotNull F2Element y);
}
