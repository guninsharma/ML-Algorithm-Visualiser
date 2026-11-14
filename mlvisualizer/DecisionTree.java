package mlvisualizer;

import java.util.ArrayList;
import java.util.Collections;

/** Binary decision tree built one node per step using weighted Gini impurity. */
public class DecisionTree extends MLAlgorithm {
    public static final int MAX_NODES = 127;
    private final int maxDepth;
    private final int minLeaf;
    private TreeNode root;
    private TreeNode activeNode;
    private int nodeCount;

    public DecisionTree(ArrayList<DataPoint> points, int maxDepth, int minLeaf) {
        super(points);
        if (points == null || points.isEmpty()) throw new InvalidParameterException("Generate data first.");
        if (maxDepth < 1 || maxDepth > 15) throw new InvalidParameterException("Depth must be 1 to 15.");
        if (minLeaf < 1) throw new InvalidParameterException("Minimum leaf size must be positive.");
        for (DataPoint point : points) if (point == null) throw new InvalidParameterException("Data cannot contain null points.");
        this.maxDepth = maxDepth;
        this.minLeaf = minLeaf;
        reset();
    }

    @Override public String getName() { return "Decision Tree"; }
    @Override public double[][] getCentroids() { return new double[0][2]; }
    public TreeNode getRoot() { return root; }
    public TreeNode getActiveNode() { return activeNode; }
    public int getNodeCount() { return nodeCount; }
    public boolean isComplete() { return nextPending(root) == null; }

    @Override public void reset() {
        root = new TreeNode(new ArrayList<>(points), 0);
        activeNode = null;
        nodeCount = 1;
        iteration = 0;
        explanation = "Root has " + points.size() + " samples. Click Next Step to choose its best split.";
    }

    @Override public void nextStep() {
        TreeNode node = nextPending(root);
        if (node == null) { explanation = "Tree is complete. Click the plot or enter a test point to classify it."; return; }
        activeNode = node;
        iteration++;
        expand(node);
    }

    // Recursive depth-first search lets each click expand exactly one pending node.
    private TreeNode nextPending(TreeNode node) {
        if (!node.isExpanded()) return node;
        if (node.isLeaf()) return null;
        TreeNode pending = nextPending(node.getYesChild());
        return pending != null ? pending : nextPending(node.getNoChild());
    }

    private void expand(TreeNode node) {
        ArrayList<DataPoint> samples = node.getSamples();
        if (node.getDepth() >= maxDepth || node.getGini() < 0.0000001 || samples.size() < 2 * minLeaf) {
            node.makeLeaf();
            explanation = "Leaf at depth " + node.getDepth() + ": stopping rule reached. Predict class " + node.getPrediction() + ".";
            return;
        }
        double bestGini = node.getGini();
        int bestFeature = -1;
        double bestThreshold = 0;
        for (int feature = 0; feature < 2; feature++) {
            ArrayList<Double> values = new ArrayList<>();
            for (DataPoint point : samples) values.add(feature == 0 ? point.getX() : point.getY());
            Collections.sort(values);
            for (int i = 1; i < values.size(); i++) {
                if (values.get(i).equals(values.get(i - 1))) continue;
                double threshold = (values.get(i) + values.get(i - 1)) / 2;
                int leftCount = 0, leftOne = 0, rightOne = 0;
                for (DataPoint point : samples) {
                    double value = feature == 0 ? point.getX() : point.getY();
                    if (value <= threshold) {
                        leftCount++;
                        if (point.getLabel() == 1) leftOne++;
                    } else if (point.getLabel() == 1) rightOne++;
                }
                int rightCount = samples.size() - leftCount;
                if (leftCount < minLeaf || rightCount < minLeaf) continue;
                double weighted = (leftCount * gini(leftOne, leftCount) + rightCount * gini(rightOne, rightCount)) / samples.size();
                if (weighted < bestGini - 0.0000001) {
                    bestGini = weighted;
                    bestFeature = feature;
                    bestThreshold = threshold;
                }
            }
        }
        if (bestFeature < 0) {
            node.makeLeaf();
            explanation = "No split improves Gini. This node becomes a class " + node.getPrediction() + " leaf.";
        } else if (nodeCount + 2 > MAX_NODES) {
            node.makeLeaf();
            explanation = "The 127-node safety limit was reached. This node becomes a leaf to keep the view responsive.";
        } else {
            ArrayList<DataPoint> yes = new ArrayList<>(), no = new ArrayList<>();
            for (DataPoint point : samples) {
                double value = bestFeature == 0 ? point.getX() : point.getY();
                if (value <= bestThreshold) yes.add(point); else no.add(point);
            }
            node.split(bestFeature, bestThreshold, new TreeNode(yes, node.getDepth() + 1), new TreeNode(no, node.getDepth() + 1));
            nodeCount += 2;
            explanation = String.format("Split %s ≤ %.2f: Gini %.3f → %.3f; Yes %d samples, No %d samples.",
                    bestFeature == 0 ? "X" : "Y", bestThreshold, node.getGini(), bestGini, yes.size(), no.size());
        }
    }

    private double gini(int classOne, int count) {
        double p = (double) classOne / count;
        return 1 - p * p - (1 - p) * (1 - p);
    }

    public int predict(double x, double y) {
        if (!isComplete()) throw new IllegalStateException("Finish constructing the tree before classifying a point.");
        TreeNode node = root;
        while (!node.isLeaf()) node = (node.getFeature() == 0 ? x : y) <= node.getThreshold() ? node.getYesChild() : node.getNoChild();
        return node.getPrediction();
    }

    public ArrayList<TreeNode> pathFor(double x, double y) {
        if (!isComplete()) throw new IllegalStateException("Finish constructing the tree before classifying a point.");
        ArrayList<TreeNode> path = new ArrayList<>();
        TreeNode node = root;
        while (node != null) {
            path.add(node);
            if (node.isLeaf()) break;
            node = (node.getFeature() == 0 ? x : y) <= node.getThreshold() ? node.getYesChild() : node.getNoChild();
        }
        return path;
    }
}
