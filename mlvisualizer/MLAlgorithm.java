package mlvisualizer;

import java.util.ArrayList;

/** Common parent for algorithms that can advance one visible step at a time. */
public abstract class MLAlgorithm {
    protected final ArrayList<DataPoint> points;
    protected int iteration;
    protected String explanation;

    protected MLAlgorithm(ArrayList<DataPoint> points) {
        this.points = points;
        this.iteration = 0;
    }

    public int getIteration() {
        return iteration;
    }

    public String getExplanation() {
        return explanation;
    }

    public abstract String getName();

    public abstract void nextStep();

    public abstract void reset();

    public abstract boolean isComplete();

    public abstract double[][] getCentroids();
}
