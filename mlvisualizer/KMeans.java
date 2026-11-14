package mlvisualizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/** K-Means alternates between assigning points and moving centroids to cluster means. */
public class KMeans extends MLAlgorithm {
    private final int k;
    private final double[][] centroids;
    private final double[][] initialCentroids;
    private boolean assignNext;
    private boolean complete;

    public KMeans(ArrayList<DataPoint> points, int k) {
        super(points);
        if (points == null || points.isEmpty()) {
            throw new InvalidParameterException("Generate at least one data point first.");
        }
        if (k < 1 || k > points.size()) {
            throw new InvalidParameterException("K must be between 1 and the number of points (" + points.size() + ").");
        }
        for (DataPoint point : points) {
            if (point == null) throw new InvalidParameterException("Data cannot contain null points.");
        }

        this.k = k;
        centroids = new double[k][2];
        initialCentroids = new double[k][2];

        // Choose distinct existing points as starting centroids.
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, new Random());
        for (int i = 0; i < k; i++) {
            DataPoint point = points.get(indices.get(i));
            initialCentroids[i][0] = point.getX();
            initialCentroids[i][1] = point.getY();
        }
        reset();
    }

    @Override
    public String getName() {
        return "K-Means";
    }

    @Override
    public void nextStep() {
        if (complete) {
            explanation = "K-Means has converged or reached 100 iterations. Reset to start again.";
            return;
        }
        if (assignNext) {
            assignPoints();
            assignNext = false;
            explanation = "Assignment: each point joins the nearest centroid using Euclidean distance. Next, update the centroids.";
        } else {
            double[][] previous = new double[k][2];
            for (int i = 0; i < k; i++) {
                previous[i][0] = centroids[i][0];
                previous[i][1] = centroids[i][1];
            }
            int emptyClusters = updateCentroids();
            iteration++;
            assignNext = true;
            boolean moved = false;
            for (int i = 0; i < k; i++) {
                if (Math.abs(previous[i][0] - centroids[i][0]) > 0.000001
                        || Math.abs(previous[i][1] - centroids[i][1]) > 0.000001) moved = true;
            }
            complete = !moved || iteration >= 100;
            explanation = "Update: each centroid moves to the mean of its assigned points. Iteration " + iteration + " is complete.";
            if (emptyClusters > 0) {
                explanation += " " + emptyClusters + " empty cluster(s) kept their previous centroid.";
            }
            if (complete) explanation += " K-Means stopped because centroids stabilized or the 100-iteration limit was reached.";
        }
    }

    private void assignPoints() {
        for (DataPoint point : points) {
            int nearest = 0;
            double shortestDistance = Double.MAX_VALUE;
            for (int i = 0; i < k; i++) {
                double dx = point.getX() - centroids[i][0];
                double dy = point.getY() - centroids[i][1];
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    nearest = i;
                }
            }
            point.setCluster(nearest);
        }
    }

    private int updateCentroids() {
        double[][] sums = new double[k][2];
        int[] counts = new int[k];
        for (DataPoint point : points) {
            int cluster = point.getCluster();
            sums[cluster][0] += point.getX();
            sums[cluster][1] += point.getY();
            counts[cluster]++;
        }
        int emptyClusters = 0;
        for (int i = 0; i < k; i++) {
            if (counts[i] == 0) {
                emptyClusters++;
            } else {
                centroids[i][0] = sums[i][0] / counts[i];
                centroids[i][1] = sums[i][1] / counts[i];
            }
        }
        return emptyClusters;
    }

    @Override
    public void reset() {
        iteration = 0;
        assignNext = true;
        complete = false;
        for (DataPoint point : points) {
            point.setCluster(-1);
        }
        for (int i = 0; i < k; i++) {
            centroids[i][0] = initialCentroids[i][0];
            centroids[i][1] = initialCentroids[i][1];
        }
        explanation = "Starting centroids are selected from the data. Click Next Step to assign points to their nearest centroid.";
    }

    @Override
    public double[][] getCentroids() {
        return centroids;
    }

    @Override public boolean isComplete() { return complete; }
}
