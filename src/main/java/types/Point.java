package types;

public class Point {
    public AffinePoint[] p;

    public Point() {
        p = new AffinePoint[1];
        p[0] = new AffinePoint();
    }
}