package mlvisualizer;

/** One point on the graph. A cluster of -1 means it has not been assigned yet. */
public class DataPoint {
    private final double x;
    private final double y;
    private int cluster;
    private final int label;

    public DataPoint(double x, double y) {
        this(x, y, 0);
    }

    public DataPoint(double x, double y, int label) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new InvalidParameterException("Point coordinates must be finite numbers.");
        }
        if (label != 0 && label != 1) {
            throw new InvalidParameterException("Class label must be 0 or 1.");
        }
        this.x = x;
        this.y = y;
        this.cluster = -1;
        this.label = label;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    public int getLabel() {
        return label;
    }
}
