import java.util.ArrayList;
import mlvisualizer.DataPoint;
import mlvisualizer.DecisionTree;
import mlvisualizer.InvalidParameterException;
import mlvisualizer.KMeans;
import mlvisualizer.KNN;
import mlvisualizer.LinearRegression;

/** Plain Java checks so the project needs no test framework dependency. */
public class AlgorithmReview {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected InvalidParameterException");
        } catch (InvalidParameterException expected) {
            // The input guard worked.
        }
    }

    public static void main(String[] args) {
        ArrayList<DataPoint> clusters = new ArrayList<>();
        clusters.add(new DataPoint(10, 10));
        clusters.add(new DataPoint(20, 20));
        clusters.add(new DataPoint(80, 80));
        clusters.add(new DataPoint(90, 90));
        KMeans means = new KMeans(clusters, 2);
        double[][] initial = new double[2][2];
        for (int i = 0; i < 2; i++) {
            initial[i][0] = means.getCentroids()[i][0];
            initial[i][1] = means.getCentroids()[i][1];
        }
        means.nextStep();
        for (DataPoint point : clusters) {
            double first = Math.hypot(point.getX() - initial[0][0], point.getY() - initial[0][1]);
            double second = Math.hypot(point.getX() - initial[1][0], point.getY() - initial[1][1]);
            check(point.getCluster() == (first <= second ? 0 : 1), "K-Means nearest centroid");
        }
        means.nextStep();
        for (int i = 0; i < 2; i++) {
            double sumX = 0, sumY = 0;
            int count = 0;
            for (DataPoint point : clusters) {
                if (point.getCluster() == i) {
                    sumX += point.getX();
                    sumY += point.getY();
                    count++;
                }
            }
            if (count > 0) {
                check(Math.abs(means.getCentroids()[i][0] - sumX / count) < 1e-9, "K-Means X mean");
                check(Math.abs(means.getCentroids()[i][1] - sumY / count) < 1e-9, "K-Means Y mean");
            }
        }
        means.reset();
        check(means.getIteration() == 0 && clusters.stream().allMatch(p -> p.getCluster() == -1), "K-Means reset");

        ArrayList<DataPoint> classes = new ArrayList<>();
        classes.add(new DataPoint(10, 10, 0));
        classes.add(new DataPoint(20, 20, 0));
        classes.add(new DataPoint(80, 80, 1));
        classes.add(new DataPoint(90, 90, 1));
        DecisionTree tree = new DecisionTree(classes, 1, 2);
        check(Math.abs(tree.getRoot().getGini() - 0.5) < 1e-9, "Root Gini");
        tree.nextStep();
        check(tree.getNodeCount() == 3, "Tree root split");
        tree.nextStep();
        tree.nextStep();
        check(tree.isComplete(), "Depth-one tree completion");
        check(tree.predict(15, 15) == 0 && tree.predict(85, 85) == 1, "Tree predictions");
        tree.reset();
        check(tree.getNodeCount() == 1 && !tree.isComplete(), "Tree reset");

        ArrayList<DataPoint> line = new ArrayList<>();
        for (int x = 10; x <= 90; x += 10) line.add(new DataPoint(x, 10 + 0.7 * x));
        LinearRegression regression = new LinearRegression(line, 0.1, 100);
        double initialMse = regression.getMSE();
        for (int i = 0; i < 100; i++) regression.nextStep();
        double finalMse = regression.getMSE();
        check(finalMse < initialMse * 0.1 && regression.isComplete(), "Regression MSE reduction");
        regression.reset();
        check(regression.getIteration() == 0, "Regression reset");

        KNN knn = new KNN(classes, 3);
        expectInvalid(knn::nextStep);
        knn.setTestPoint(15, 15);
        knn.nextStep();
        check(Math.abs(knn.getDistance(0) - Math.sqrt(50)) < 1e-9, "KNN Euclidean distance");
        for (int i = 0; i < 4; i++) knn.nextStep();
        check(knn.getPrediction() == 0, "KNN class 0");
        knn.setTestPoint(85, 85);
        for (int i = 0; i < 5; i++) knn.nextStep();
        check(knn.getPrediction() == 1, "KNN class 1");
        KNN tie = new KNN(classes, 4);
        tie.setTestPoint(50, 50);
        for (int i = 0; i < 6; i++) tie.nextStep();
        check(tie.getPrediction() == 0, "KNN tie goes to class 0");

        expectInvalid(() -> new KMeans(clusters, 0));
        expectInvalid(() -> new DecisionTree(classes, 16, 2));
        expectInvalid(() -> new LinearRegression(line, 0, 10));
        expectInvalid(() -> new KNN(classes, 5));
        expectInvalid(() -> new DataPoint(Double.NaN, 5));
        System.out.printf("All algorithm checks passed. Regression MSE %.2f -> %.2f%n", initialMse, finalMse);
    }
}
