package mlvisualizer;

import java.util.ArrayList;
import java.util.Collections;

/** Reveals Euclidean distances, neighbors, and a majority vote in separate steps. */
public class KNN extends MLAlgorithm {
    private final int k;
    private final double[] distances;
    private final ArrayList<Integer> sortedIndices = new ArrayList<>();
    private double testX = 50;
    private double testY = 50;
    private int revealedNeighbors;
    private int prediction = -1;
    private boolean distancesReady;
    private boolean hasTestPoint;

    public KNN(ArrayList<DataPoint> points, int k) {
        super(points);
        if (points == null || points.isEmpty()) {
            throw new InvalidParameterException("Generate at least one training point first.");
        }
        if (k < 1 || k > points.size()) {
            throw new InvalidParameterException("K must be between 1 and the number of points (" + points.size() + ").");
        }
        for (DataPoint point : points) {
            if (point == null) throw new InvalidParameterException("Training points cannot be null.");
        }
        this.k = k;
        distances = new double[points.size()];
        reset();
    }

    @Override public String getName() { return "KNN"; }
    @Override public double[][] getCentroids() { return new double[0][2]; }
    public int getK() { return k; }
    public double getTestX() { return testX; }
    public double getTestY() { return testY; }
    public int getRevealedNeighbors() { return revealedNeighbors; }
    public int getPrediction() { return prediction; }
    public boolean areDistancesReady() { return distancesReady; }
    public boolean isComplete() { return prediction >= 0; }
    public boolean hasTestPoint() { return hasTestPoint; }
    public double getDistance(int index) { return distances[index]; }
    public int getNeighborIndex(int rank) { return sortedIndices.get(rank); }

    public void setTestPoint(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x > 100 || y < 0 || y > 100) {
            throw new InvalidParameterException("Test coordinates must be between 0 and 100.");
        }
        testX = x;
        testY = y;
        hasTestPoint = true;
        reset();
    }

    @Override public void reset() {
        iteration = 0;
        distancesReady = false;
        revealedNeighbors = 0;
        prediction = -1;
        sortedIndices.clear();
        explanation = hasTestPoint ? "Test point selected. Next Step calculates Euclidean distances to all training points."
                : "Click the plot or enter X and Y, then select a test point.";
    }

    @Override public void nextStep() {
        if (!hasTestPoint) throw new InvalidParameterException("Select a KNN test point before stepping or playing.");
        if (isComplete()) {
            explanation = "Classification is complete. Select another test point or Reset.";
            return;
        }
        iteration++;
        if (!distancesReady) {
            for (int i = 0; i < points.size(); i++) {
                DataPoint point = points.get(i);
                double dx = testX - point.getX();
                double dy = testY - point.getY();
                distances[i] = Math.sqrt(dx * dx + dy * dy);
                sortedIndices.add(i);
            }
            // Equal distances use the original point order for a repeatable result.
            Collections.sort(sortedIndices, (a, b) -> {
                int comparison = Double.compare(distances[a], distances[b]);
                return comparison != 0 ? comparison : Integer.compare(a, b);
            });
            distancesReady = true;
            explanation = "Calculated distances to all " + points.size() + " points. Next Step reveals the nearest neighbors.";
        } else if (revealedNeighbors < k) {
            revealedNeighbors++;
            int index = sortedIndices.get(revealedNeighbors - 1);
            explanation = String.format("Neighbor %d of %d: point %d, class %d, distance %.2f.",
                    revealedNeighbors, k, index + 1, points.get(index).getLabel(), distances[index]);
        } else {
            int ones = 0;
            for (int i = 0; i < k; i++) {
                if (points.get(sortedIndices.get(i)).getLabel() == 1) ones++;
            }
            int zeros = k - ones;
            prediction = ones > zeros ? 1 : 0; // A tie always predicts class 0.
            explanation = "Majority vote: class 0 = " + zeros + ", class 1 = " + ones
                    + ". Predicted class " + prediction + (ones == zeros ? " (tie goes to class 0)." : ".");
        }
    }
}
