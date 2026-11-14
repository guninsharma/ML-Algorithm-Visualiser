package mlvisualizer;

import java.util.ArrayList;

/** One tree node; children are created only when this node is expanded. */
public class TreeNode {
    private final ArrayList<DataPoint> samples;
    private final int depth;
    private final double gini;
    private final int prediction;
    private boolean expanded;
    private boolean leaf;
    private int feature;
    private double threshold;
    private TreeNode yesChild;
    private TreeNode noChild;

    public TreeNode(ArrayList<DataPoint> samples, int depth) {
        if (samples == null || samples.isEmpty()) {
            throw new InvalidParameterException("A tree node needs at least one sample.");
        }
        this.samples = samples;
        this.depth = depth;
        int classOne = 0;
        for (DataPoint point : samples) if (point.getLabel() == 1) classOne++;
        double one = (double) classOne / samples.size();
        this.gini = 1 - one * one - (1 - one) * (1 - one);
        this.prediction = classOne > samples.size() - classOne ? 1 : 0;
    }

    public ArrayList<DataPoint> getSamples() { return samples; }
    public int getDepth() { return depth; }
    public double getGini() { return gini; }
    public int getPrediction() { return prediction; }
    public boolean isExpanded() { return expanded; }
    public boolean isLeaf() { return leaf; }
    public int getFeature() { return feature; }
    public double getThreshold() { return threshold; }
    public TreeNode getYesChild() { return yesChild; }
    public TreeNode getNoChild() { return noChild; }

    void makeLeaf() { expanded = true; leaf = true; }
    void split(int feature, double threshold, TreeNode yes, TreeNode no) {
        this.feature = feature;
        this.threshold = threshold;
        this.yesChild = yes;
        this.noChild = no;
        expanded = true;
    }
}
