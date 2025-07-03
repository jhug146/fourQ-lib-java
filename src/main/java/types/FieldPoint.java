package types;


public class FieldPoint implements Point {
    private F2Element x;
    private F2Element y;
    public FieldPoint(F2Element x, F2Element y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public F2Element getX() { return x; }

    @Override
    public F2Element getY() { return y; }

    @Override
    public void setX(F2Element x) { this.x = x; }

    @Override
    public void setY(F2Element y) { this.y = y; }
}
