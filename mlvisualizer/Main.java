package mlvisualizer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Random;
import javafx.application.Application;
import javafx.animation.Transition;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

/** JavaFX dashboard: the model is stepped manually through its abstract interface. */
public class Main extends Application {
    private final Random random = new Random();
    private final ComboBox<String> choice = new ComboBox<>();
    private final Spinner<Integer> k = new Spinner<>(1, 10, 3);
    private final Spinner<Integer> depth = new Spinner<>(1, 15, 5);
    private final Spinner<Integer> minLeaf = new Spinner<>(1, 20, 2);
    private final Spinner<Integer> count = new Spinner<>(0, 100, 45);
    private final Spinner<Integer> knnK = new Spinner<>(1, 10, 3);
    private final Spinner<Double> learningRate = new Spinner<>(0.001, 1.0, 0.1, 0.01);
    private final Spinner<Integer> maxIterations = new Spinner<>(1, 1000, 100);
    private final TextField testX = new TextField("50"), testY = new TextField("50");
    private final VBox treeOptions = new VBox(8), kOptions = new VBox(8);
    private final VBox regressionOptions = new VBox(8), knnOptions = new VBox(8);
    private final VBox testOptions = new VBox(8);
    private final Button classifyButton = new Button("Classify test point");
    private final Slider speed = new Slider(100, 1500, 600);
    private final Label speedLabel = new Label("600 ms/step");
    private final Canvas plot = new Canvas(430, 430);
    private final StackPane plotBox = new StackPane(plot);
    private final Pane treePane = new Pane();
    private final Group treeGroup = new Group(treePane);
    private final ScrollPane treeScroll = new ScrollPane(treeGroup);
    private final SplitPane views = new SplitPane();
    private HBox treeTools;
    private final Label status = new Label();
    private final TextArea explanation = new TextArea();
    private ArrayList<DataPoint> points = new ArrayList<>();
    private ArrayList<TreeNode> path = new ArrayList<>();
    private MLAlgorithm algorithm;
    private AnimationWorker worker;
    private volatile int delayMillis = 600;
    private Transition plotTransition;
    private double[][] displayedCentroids;
    private Double displayedSlope, displayedIntercept;
    private double zoom = 1, dragX, dragY, dragH, dragV;
    private int leafIndex;

    @Override public void start(Stage stage) {
        choice.getItems().addAll("K-Means", "Decision Tree", "Linear Regression", "KNN");
        choice.setValue("Decision Tree");
        kOptions.getChildren().add(field("Clusters (K)", k));
        treeOptions.getChildren().addAll(field("Maximum depth", depth), field("Minimum samples per leaf", minLeaf));
        regressionOptions.getChildren().addAll(field("Learning rate", learningRate), field("Maximum iterations", maxIterations));
        knnOptions.getChildren().add(field("Neighbors (K)", knnK));
        classifyButton.setOnAction(e -> classifyPoint());
        testOptions.getChildren().addAll(heading("TEST POINT"), field("X", testX), field("Y", testY), classifyButton);
        VBox sidebar = new VBox(12, heading("ML VISUALIZER"), new Label("Explore algorithms step by step"),
                field("Algorithm", choice), field("Data points", count), kOptions, treeOptions,
                regressionOptions, knnOptions, testOptions);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(245);
        worker = new AnimationWorker(this::advanceOneStep, () -> delayMillis);
        worker.start();
        Button play = button("Play", () -> play());
        Button pause = button("Pause", () -> pause());
        Button next = button("Next Step", () -> { pause(); advanceOneStep(); });
        next.getStyleClass().add("primary");
        FlowPane toolbar = new FlowPane(9, 8, button("Generate Data", () -> generateData()), play, pause, next,
                button("Reset", () -> { pause(); stopTransition(); if (algorithm != null) { algorithm.reset(); path.clear(); refresh(); } }));
        toolbar.getStyleClass().add("toolbar");
        speed.setPrefWidth(135);
        speed.valueProperty().addListener((observable, oldValue, newValue) -> {
            delayMillis = newValue.intValue();
            speedLabel.setText(delayMillis + " ms/step");
        });
        toolbar.getChildren().addAll(new Label("Speed"), speed, speedLabel);
        plotBox.getStyleClass().add("card");
        plotBox.setMinWidth(300);
        plot.widthProperty().bind(plotBox.widthProperty().subtract(20));
        plot.heightProperty().bind(plotBox.heightProperty().subtract(20));
        plot.widthProperty().addListener((o,a,b) -> drawPlot());
        plot.heightProperty().addListener((o,a,b) -> drawPlot());
        treeScroll.getStyleClass().add("card");
        treeScroll.setPannable(true);
        views.getItems().addAll(plotBox, treeScroll);
        views.setDividerPositions(0.46);
        VBox.setVgrow(views, Priority.ALWAYS);
        treeTools = new HBox(8, new Label("SCATTER PLOT  •  DECISION TREE"),
                button("−", () -> setZoom(zoom / 1.2)), button("100%", () -> setZoom(1)),
                button("+", () -> setZoom(zoom * 1.2)));
        treeTools.getStyleClass().add("toolbar");
        explanation.setWrapText(true);
        explanation.setEditable(false);
        explanation.setPrefRowCount(4);
        VBox info = new VBox(5, status, explanation);
        info.getStyleClass().add("info");
        VBox center = new VBox(8, toolbar, treeTools, views, info);
        center.setPadding(new Insets(14));
        BorderPane root = new BorderPane(center);
        root.setLeft(sidebar);
        Scene scene = new Scene(root, 1200, 760);
        scene.getStylesheets().add(getClass().getResource("/mlvisualizer/style.css").toExternalForm());
        stage.setTitle("ML Algorithm Visualizer"); stage.setMinWidth(850); stage.setMinHeight(600); stage.setScene(scene);
        stage.setOnCloseRequest(e -> worker.shutdown());
        choice.setOnAction(e -> generateData());
        k.valueProperty().addListener((o,a,b) -> createAlgorithm());
        depth.valueProperty().addListener((o,a,b) -> createAlgorithm());
        minLeaf.valueProperty().addListener((o,a,b) -> createAlgorithm());
        knnK.valueProperty().addListener((o,a,b) -> createAlgorithm());
        learningRate.valueProperty().addListener((o,a,b) -> createAlgorithm());
        maxIterations.valueProperty().addListener((o,a,b) -> createAlgorithm());
        plot.setOnMouseClicked(e -> {
            testX.setText(String.format(Locale.US, "%.1f", clamp((e.getX()-35)*100/Math.max(1,plot.getWidth()-55))));
            testY.setText(String.format(Locale.US, "%.1f", clamp((plot.getHeight()-35-e.getY())*100/Math.max(1,plot.getHeight()-55))));
            if (algorithm instanceof KNN knn) {
                knn.setTestPoint(Double.parseDouble(testX.getText()), Double.parseDouble(testY.getText()));
                refresh();
            } else if (algorithm instanceof DecisionTree tree && tree.isComplete()) classifyPoint();
        });
        treeScroll.setOnMousePressed(e -> { dragX=e.getX(); dragY=e.getY(); dragH=treeScroll.getHvalue(); dragV=treeScroll.getVvalue(); });
        treeScroll.setOnMouseDragged(e -> {
            treeScroll.setHvalue(dragH-(e.getX()-dragX)/Math.max(1,treePane.getWidth()*zoom));
            treeScroll.setVvalue(dragV-(e.getY()-dragY)/Math.max(1,treePane.getHeight()*zoom));
        });
        treeScroll.setOnScroll(e -> { if(e.isControlDown()) { setZoom(zoom*(e.getDeltaY()>0?1.1:1/1.1)); e.consume(); } });
        generateData(); stage.show();
    }

    private Button button(String text, Runnable action) { Button b=new Button(text); b.setOnAction(e -> action.run()); return b; }
    private Label heading(String text) { Label l=new Label(text); l.getStyleClass().add("heading"); return l; }
    private VBox field(String title, javafx.scene.Node input) {
        Label label=new Label(title); label.getStyleClass().add("field-label");
        if(input instanceof Control c) c.setMaxWidth(Double.MAX_VALUE);
        return new VBox(5,label,input);
    }
    private double clamp(double value) { return Math.max(0,Math.min(100,value)); }
    private void setZoom(double value) { zoom=Math.max(0.45,Math.min(2.5,value)); treeGroup.getTransforms().setAll(new Scale(zoom,zoom)); }

    private void generateData() {
        pause();
        stopTransition();
        if (count.getValue() < 1 || count.getValue() > 100) {
            showError("Dataset size must be between 1 and 100.");
            return;
        }
        points=new ArrayList<>();
        String selected = choice.getValue();
        for(int i=0;i<count.getValue();i++) {
            double x, y;
            int label;
            if ("Linear Regression".equals(selected)) {
                x = 5 + random.nextDouble() * 90;
                y = clamp(12 + 0.7 * x + random.nextGaussian() * 6);
                label = 0;
            } else if ("KNN".equals(selected)) {
                label = i % 2;
                x = clamp((label == 0 ? 27 : 73) + random.nextGaussian() * 12);
                y = clamp((label == 0 ? 30 : 70) + random.nextGaussian() * 12);
            } else {
                x = 5 + random.nextDouble() * 90;
                y = 5 + random.nextDouble() * 90;
                label = x + y > 100 ? 1 : 0;
            }
            points.add(new DataPoint(x, y, label));
        }
        createAlgorithm();
    }
    private void createAlgorithm() {
        pause();
        stopTransition();
        String selected = choice.getValue();
        boolean tree="Decision Tree".equals(selected);
        treeOptions.setVisible(tree); treeOptions.setManaged(tree);
        treeTools.setVisible(tree); treeTools.setManaged(tree);
        if (tree) {
            views.getItems().setAll(plotBox, treeScroll);
            views.setDividerPositions(0.46);
        } else {
            views.getItems().setAll(plotBox);
        }
        boolean clustering = "K-Means".equals(selected);
        kOptions.setVisible(clustering); kOptions.setManaged(clustering);
        boolean regression = "Linear Regression".equals(selected);
        regressionOptions.setVisible(regression); regressionOptions.setManaged(regression);
        boolean nearest = "KNN".equals(selected);
        knnOptions.setVisible(nearest); knnOptions.setManaged(nearest);
        testOptions.setVisible(tree || nearest); testOptions.setManaged(tree || nearest);
        classifyButton.setText(nearest ? "Select test point" : "Classify test point");
        path.clear();
        try {
            switch (selected) {
                case "Decision Tree" -> algorithm = new DecisionTree(points, depth.getValue(), minLeaf.getValue());
                case "Linear Regression" -> algorithm = new LinearRegression(points, learningRate.getValue(), maxIterations.getValue());
                case "KNN" -> {
                    algorithm = new KNN(points, knnK.getValue());
                }
                default -> algorithm = new KMeans(points, k.getValue());
            }
            refresh();
        } catch(IllegalArgumentException e) {
            algorithm=null; status.setText("Choose valid parameters"); explanation.setText(e.getMessage());
            treePane.getChildren().clear(); drawPlot();
            showError(e.getMessage());
        }
    }
    private void play() {
        if (algorithm == null) { showError("Generate data with valid parameters first."); return; }
        if (algorithm instanceof KNN model && !model.hasTestPoint()) {
            showError("Select a KNN test point before playing."); return;
        }
        if (algorithm.isComplete()) { explanation.setText("This run is complete. Reset to start again."); return; }
        worker.play();
        status.setText(algorithm.getName() + "  •  Playing");
    }
    private void pause() { if (worker != null) worker.pausePlayback(); }
    private void advanceOneStep() {
        if (algorithm == null) {
            showError("Generate data with valid parameters first.");
            return;
        }
        if (algorithm.isComplete()) { pause(); return; }
        stopTransition();
        double[][] beforeCenters = null;
        double beforeSlope = 0, beforeIntercept = 0;
        if (algorithm instanceof KMeans) {
            double[][] current = algorithm.getCentroids();
            beforeCenters = new double[current.length][2];
            for (int i = 0; i < current.length; i++) {
                beforeCenters[i][0] = current[i][0];
                beforeCenters[i][1] = current[i][1];
            }
        } else if (algorithm instanceof LinearRegression regression) {
            beforeSlope = regression.getSlope();
            beforeIntercept = regression.getIntercept();
        }
        try {
            algorithm.nextStep(); // Runtime polymorphism: each subclass supplies its own step.
            path.clear();
            refresh();
            animatePlot(beforeCenters, beforeSlope, beforeIntercept);
            if (algorithm.isComplete()) pause();
        } catch (InvalidParameterException exception) {
            pause();
            showError(exception.getMessage());
        }
    }
    private void stopTransition() {
        if (plotTransition != null) plotTransition.stop();
        plotTransition = null;
        displayedCentroids = null;
        displayedSlope = null;
        displayedIntercept = null;
    }
    private void animatePlot(double[][] beforeCenters, double beforeSlope, double beforeIntercept) {
        if (algorithm instanceof KMeans && beforeCenters != null) {
            double[][] after = algorithm.getCentroids();
            displayedCentroids = new double[after.length][2];
            plotTransition = new Transition() {
                { setCycleDuration(Duration.millis(220)); }
                @Override protected void interpolate(double fraction) {
                    for (int i = 0; i < after.length; i++) {
                        displayedCentroids[i][0] = beforeCenters[i][0] + fraction * (after[i][0] - beforeCenters[i][0]);
                        displayedCentroids[i][1] = beforeCenters[i][1] + fraction * (after[i][1] - beforeCenters[i][1]);
                    }
                    drawPlot();
                }
            };
        } else if (algorithm instanceof LinearRegression regression) {
            double afterSlope = regression.getSlope(), afterIntercept = regression.getIntercept();
            plotTransition = new Transition() {
                { setCycleDuration(Duration.millis(220)); }
                @Override protected void interpolate(double fraction) {
                    displayedSlope = beforeSlope + fraction * (afterSlope - beforeSlope);
                    displayedIntercept = beforeIntercept + fraction * (afterIntercept - beforeIntercept);
                    drawPlot();
                }
            };
        }
        if (plotTransition != null) {
            plotTransition.setOnFinished(e -> { displayedCentroids = null; displayedSlope = null; displayedIntercept = null; drawPlot(); });
            plotTransition.playFromStart();
        }
    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid input");
        alert.setHeaderText("Please check your settings");
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void refresh() {
        status.setText(algorithm.getName()+"  •  "+points.size()+" samples  •  "+algorithm.getIteration()+" step(s)");
        String text=algorithm.getExplanation();
        if(algorithm instanceof KMeans) {
            double[][] centers=algorithm.getCentroids();
            for(int i=0;i<centers.length;i++) text+=String.format(Locale.US,"   C%d (%.1f, %.1f)",i+1,centers[i][0],centers[i][1]);
        }
        if (algorithm instanceof LinearRegression model) {
            text += String.format(Locale.US, "  Slope %.4f  •  Intercept %.2f  •  MSE %.3f",
                    model.getSlope(), model.getIntercept(), model.getMSE());
        }
        if (algorithm instanceof KNN model && model.areDistancesReady()) {
            text += "\nDistances to training points:\n";
            for (int i = 0; i < points.size(); i++) {
                text += String.format(Locale.US, "P%d class %d: %.2f%s", i + 1,
                        points.get(i).getLabel(), model.getDistance(i), i == points.size() - 1 ? "" : "   ");
            }
        }
        explanation.setText(text); drawPlot(); drawTree();
    }
    private void classifyPoint() {
        if (algorithm instanceof KNN model) {
            try {
                model.setTestPoint(Double.parseDouble(testX.getText()), Double.parseDouble(testY.getText()));
                refresh();
            } catch (IllegalArgumentException error) {
                explanation.setText("Enter X and Y as numbers between 0 and 100.");
                showError("Enter X and Y as numbers between 0 and 100.");
            }
            return;
        }
        if(!(algorithm instanceof DecisionTree tree)) return;
        try {
            double x=Double.parseDouble(testX.getText()), y=Double.parseDouble(testY.getText());
            if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>100||y<0||y>100) throw new NumberFormatException();
            int prediction=tree.predict(x,y); path=tree.pathFor(x,y);
            explanation.setText(String.format(Locale.US,"Test point (%.1f, %.1f) → class %d. Orange nodes show its path.",x,y,prediction));
            drawPlot(); drawTree();
        } catch(NumberFormatException e) {
            explanation.setText("Enter X and Y as numbers between 0 and 100.");
            showError("Enter X and Y as numbers between 0 and 100.");
        } catch(IllegalStateException e) {
            explanation.setText(e.getMessage());
            showError(e.getMessage());
        }
    }

    private Color color(int index) { return index<0?Color.web("#8998ab"):Color.hsb((index*137.5)%360,0.72,0.8); }
    private void drawPlot() {
        GraphicsContext g=plot.getGraphicsContext2D(); double w=plot.getWidth(),h=plot.getHeight(),pw=Math.max(1,w-55),ph=Math.max(1,h-55);
        g.setFill(Color.WHITE); g.fillRect(0,0,w,h);
        g.setStroke(Color.web("#e3eaf2")); g.setLineWidth(1);
        for(int n=0;n<=100;n+=20) {
            double x=35+n*pw/100,y=20+(100-n)*ph/100;
            g.strokeLine(x,20,x,20+ph); g.strokeLine(35,y,35+pw,y);
            g.setFill(Color.web("#6a7890")); g.fillText(""+n,x-6,h-8); g.fillText(""+n,4,y+4);
        }
        g.setFill(Color.web("#334b6b"));
        g.fillText("X", w - 14, h - 26);
        g.fillText("Y", 14, 13);
        for(DataPoint p:points) {
            g.setFill(algorithm instanceof DecisionTree || algorithm instanceof KNN
                    ? (p.getLabel()==0?Color.web("#3276d2"):Color.web("#e76572"))
                    : algorithm instanceof LinearRegression ? Color.web("#3276d2") : color(p.getCluster()));
            g.fillOval(35+p.getX()*pw/100-4,20+(100-p.getY())*ph/100-4,8,8);
        }
        if(algorithm instanceof KMeans) {
            double[][] c=displayedCentroids != null ? displayedCentroids : algorithm.getCentroids();
            for(int i=0;i<c.length;i++) {
                double x=35+c[i][0]*pw/100,y=20+(100-c[i][1])*ph/100;
                g.setStroke(Color.WHITE); g.setLineWidth(6); g.strokeLine(x-10,y,x+10,y); g.strokeLine(x,y-10,x,y+10);
                g.setStroke(color(i)); g.setLineWidth(3); g.strokeLine(x-10,y,x+10,y); g.strokeLine(x,y-10,x,y+10);
            }
        }
        if (algorithm instanceof LinearRegression model) {
            g.setStroke(Color.web("#e76572")); g.setLineWidth(3);
            double slope = displayedSlope != null ? displayedSlope : model.getSlope();
            double intercept = displayedIntercept != null ? displayedIntercept : model.getIntercept();
            g.strokeLine(35, 20 + (100 - intercept) * ph / 100,
                    35 + pw, 20 + (100 - (slope * 100 + intercept)) * ph / 100);
        }
        if (algorithm instanceof KNN model) {
            if (!model.hasTestPoint()) return;
            double x = 35 + model.getTestX() * pw / 100;
            double y = 20 + (100 - model.getTestY()) * ph / 100;
            g.setStroke(Color.web("#f4a623")); g.setLineWidth(3);
            g.strokeOval(x - 9, y - 9, 18, 18);
            for (int i = 0; i < model.getRevealedNeighbors(); i++) {
                DataPoint point = points.get(model.getNeighborIndex(i));
                double nx = 35 + point.getX() * pw / 100;
                double ny = 20 + (100 - point.getY()) * ph / 100;
                g.setStroke(Color.web("#f4a623")); g.setLineWidth(1.5);
                g.strokeLine(x, y, nx, ny);
                g.setLineWidth(3); g.strokeOval(nx - 8, ny - 8, 16, 16);
            }
        }
        if(!path.isEmpty()) try {
            double x=Double.parseDouble(testX.getText()),y=Double.parseDouble(testY.getText());
            g.setStroke(Color.web("#f4a623"));g.setLineWidth(3);g.strokeOval(35+x*pw/100-8,20+(100-y)*ph/100-8,16,16);
        } catch(NumberFormatException ignored) { }
    }

    private void drawTree() {
        treePane.getChildren().clear();
        if(!(algorithm instanceof DecisionTree tree)) return;
        IdentityHashMap<TreeNode,Double> xs=new IdentityHashMap<>(); leafIndex=0;
        position(tree.getRoot(),xs);
        treePane.setPrefSize(Math.max(500,leafIndex*190+90),Math.max(420,(depth.getValue()+1)*125));
        branches(tree.getRoot(),xs); nodes(tree.getRoot(),xs,tree.getActiveNode());
    }
    private double position(TreeNode node,IdentityHashMap<TreeNode,Double> xs) {
        double x=node.getYesChild()==null?95+leafIndex++*190:(position(node.getYesChild(),xs)+position(node.getNoChild(),xs))/2;
        xs.put(node,x); return x;
    }
    private void branches(TreeNode node,IdentityHashMap<TreeNode,Double> xs) {
        if(node.getYesChild()==null) return;
        for(int i=0;i<2;i++) {
            TreeNode child=i==0?node.getYesChild():node.getNoChild();
            double x1=xs.get(node),x2=xs.get(child),y1=node.getDepth()*125+92,y2=child.getDepth()*125+28;
            Line line=new Line(x1,y1,x2,y2);
            boolean selected=path.contains(node)&&path.contains(child);
            line.setStroke(Color.web(selected?"#f4a623":"#a9b8ca")); line.setStrokeWidth(selected?3:2);
            Label label=new Label(i==0?"Yes":"No"); label.getStyleClass().add("branch-label");
            label.relocate((x1+x2)/2,(y1+y2)/2); treePane.getChildren().addAll(line,label);
            branches(child,xs);
        }
    }
    private void nodes(TreeNode node,IdentityHashMap<TreeNode,Double> xs,TreeNode active) {
        String title=!node.isExpanded()?"PENDING":node.isLeaf()?"CLASS "+node.getPrediction():
                String.format(Locale.US,"%s ≤ %.1f",node.getFeature()==0?"X":"Y",node.getThreshold());
        VBox card=new VBox(3,new Label(title),new Label(String.format(Locale.US,"Gini %.3f • n=%d",node.getGini(),node.getSamples().size())),
                new Label("Predict class "+node.getPrediction()));
        card.setPrefSize(152,64);
        card.getStyleClass().addAll("tree-node",!node.isExpanded()?"pending-node":node.isLeaf()?"leaf-node":"decision-node");
        if(node==active) card.getStyleClass().add("active-node");
        if(path.contains(node)) card.getStyleClass().add("path-node");
        card.relocate(xs.get(node)-76,node.getDepth()*125+28); treePane.getChildren().add(card);
        if(node.getYesChild()!=null) { nodes(node.getYesChild(),xs,active);nodes(node.getNoChild(),xs,active); }
    }
    public static void main(String[] args) { launch(args); }
}
