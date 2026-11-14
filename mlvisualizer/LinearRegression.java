package mlvisualizer;

import java.util.ArrayList;

/** Fits y = slope*x + intercept with one batch gradient descent update per step. */
public class LinearRegression extends MLAlgorithm {
    private final double learningRate;
    private final int maxIterations;
    private double slope;
    private double intercept;

    public LinearRegression(ArrayList<DataPoint> points, double learningRate, int maxIterations) {
        super(points);
        if (points == null || points.isEmpty()) {
            throw new InvalidParameterException("Generate at least one point before training.");
        }
        if (!Double.isFinite(learningRate) || learningRate <= 0 || learningRate > 1) {
            throw new InvalidParameterException("Learning rate must be greater than 0 and at most 1.");
        }
        if (maxIterations < 1) {
            throw new InvalidParameterException("Maximum iterations must be positive.");
        }
        for (DataPoint point : points) {
            if (point == null || !Double.isFinite(point.getX()) || !Double.isFinite(point.getY())) {
                throw new InvalidParameterException("All points must have finite coordinates.");
            }
        }
        this.learningRate = learningRate;
        this.maxIterations = maxIterations;
        reset();
    }

    @Override public String getName() { return "Linear Regression"; }
    @Override public double[][] getCentroids() { return new double[0][2]; }
    public double getSlope() { return slope; }
    /** Intercept in the graph's original 0–100 units. */
    public double getIntercept() { return intercept * 100; }
    public boolean isComplete() { return iteration >= maxIterations; }
    public double predict(double x) { return slope * x + getIntercept(); }

    public double getMSE() {
        double sum = 0;
        for (DataPoint point : points) {
            double error = predict(point.getX()) - point.getY();
            sum += error * error;
        }
        return sum / points.size();
    }

    @Override public void reset() {
        slope = 0;
        intercept = 0;
        iteration = 0;
        explanation = "Starting line: y = 0. Click Next Step for one gradient descent update.";
    }

    @Override public void nextStep() {
        if (isComplete()) {
            explanation = "Maximum iteration count reached. Reset or change the limit to train again.";
            return;
        }
        double slopeGradient = 0;
        double interceptGradient = 0;
        for (DataPoint point : points) {
            // Both axes are scaled to 0–1 while training. This keeps updates stable.
            double x = point.getX() / 100.0;
            double y = point.getY() / 100.0;
            double error = slope * x + intercept - y;
            slopeGradient += 2 * error * x;
            interceptGradient += 2 * error;
        }
        slope -= learningRate * slopeGradient / points.size();
        intercept -= learningRate * interceptGradient / points.size();
        iteration++;
        explanation = "Gradient descent moved slope and intercept opposite the MSE gradient. Iteration "
                + iteration + " of " + maxIterations + ".";
    }
}
