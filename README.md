# ML Algorithm Visualizer

A JavaFX desktop application for exploring four machine learning algorithms one step at a time. The calculations are written in Java without an ML library. The project is designed to demonstrate core university Java OOP concepts through a visual, interactive example.

## Features

- **K-Means:** assign points to the nearest centroid, then move centroids to cluster means. Points and crosses are color coded.
- **Decision Tree:** expand one node per step using Gini impurity. Inspect split conditions, samples, predictions, and a test point's path. Zoom and pan the tree.
- **Linear Regression:** update a line by batch gradient descent. View slope, intercept, and mean squared error (MSE).
- **K-Nearest Neighbors (KNN):** calculate distances, reveal the K nearest points, then show the majority vote.
- Manual **Next Step**, **Reset**, **Generate Data**, and automatic **Play/Pause**. A slider sets the delay between steps.
- Input validation with readable JavaFX error dialogs. Changing algorithms or datasets pauses playback.

## Technologies and requirements

| Item | Version or use |
|---|---|
| Java | Source and target compatibility: 17 |
| JavaFX | 21.0.6 Controls, Graphics, Canvas, nodes, CSS |
| Maven | Builds the project and downloads JavaFX; Maven 3.9.x recommended |
| Other dependencies | None; no ML library or database |

Install a JDK 17 or newer and Maven. Ensure `java`, `javac`, and `mvn` are available on PATH. An internet connection is needed on the first Maven run to download JavaFX.

## Build, run, and test

From the folder containing `pom.xml`:

```powershell
mvn clean package
mvn javafx:run
```

Run the dependency-free algorithm review after building:

```powershell
javac --release 17 -cp target/classes -d target/test-classes tests/AlgorithmReview.java
java -cp 'target/classes;target/test-classes' AlgorithmReview
```

On macOS or Linux, use `:` in the last command's classpath instead of `;`. The app is a desktop GUI and needs a graphical environment.

## Using the application

1. Select an algorithm, choose parameters, and click **Generate Data**. The data is synthetic and differs for each algorithm.
2. Use **Next Step** for one operation, or **Play** for automatic steps. **Pause** preserves progress. **Reset** restarts the current algorithm on the same data.
3. For a decision tree, set maximum depth from 1 to 15 (default 5) and minimum samples per leaf (default 2). After construction, enter X/Y and classify, or click the scatter plot. Use `+`, `-`, `100%`, and scrolling/dragging to explore the tree.
4. For KNN, select a test point by clicking the graph or entering X/Y and pressing **Select test point**. The first step computes distances; the next K steps reveal neighbors; the final step votes.
5. For regression, adjust the learning rate and maximum iterations. Each step makes one gradient descent update.

## Project files

```text
pom.xml                         Maven configuration
mlvisualizer/
  Main.java                     JavaFX entry point and dashboard
  MLAlgorithm.java              Abstract algorithm parent
  DataPoint.java                2D sample and class/cluster data
  KMeans.java                   Clustering math and steps
  DecisionTree.java             Gini split search and construction
  TreeNode.java                 Decision or leaf node
  LinearRegression.java         Gradient descent and MSE
  KNN.java                      Distance ranking and vote
  AnimationWorker.java          One background timing thread
  InvalidParameterException.java Input validation exception
src/main/resources/mlvisualizer/style.css  JavaFX stylesheet
tests/AlgorithmReview.java       Plain Java mathematical checks
screenshots/                    Actual JavaFX scene snapshots
PROJECT_REPORT.md               Design, formulas, and test results
VIVA_PREP.md                    Questions and short answers
```

## Algorithms and class hierarchy

```text
MLAlgorithm (abstract)
  +-- KMeans
  +-- DecisionTree
  +-- LinearRegression
  +-- KNN

Main (JavaFX Application) --> MLAlgorithm
DecisionTree --> TreeNode
KMeans / DecisionTree / KNN / LinearRegression --> DataPoint
Main --> AnimationWorker
```

`Main` stores the selected model as an `MLAlgorithm` and calls its overridden `nextStep()`, `reset()`, and `isComplete()` methods. Each subclass provides its own behavior.

## OOP concepts demonstrated

- **Class hierarchy, inheritance, and polymorphism:** four subclasses extend `MLAlgorithm`; `Main` uses the parent reference at runtime.
- **Encapsulation:** `DataPoint` and `TreeNode` keep fields private and expose methods to read or update them.
- **Arrays and ArrayList:** K-Means stores centroid coordinates in `double[][]`; KNN stores distances in `double[]`; point collections and tree samples use `ArrayList<DataPoint>`.
- **Exceptions:** `InvalidParameterException` reports bad parameters and inputs; the GUI catches errors and shows alerts.
- **Multithreading:** `AnimationWorker` sleeps between steps on a daemon thread. It schedules model changes and drawing on the JavaFX Application Thread.

## Known limitations

- Datasets are randomly generated in memory; there is no import, export, or saved model.
- The plot uses two features on a 0-100 coordinate plane; classification is binary.
- K-Means stops when centroids stabilize or after 100 iterations. The tree is limited to depth 15 and 127 nodes. Data generation allows 1-100 points.
- KNN classifies one selected test point at a time. A tied vote predicts class 0.
- JavaFX requires a graphical desktop. The included automated UI check exercised controls and resizing, but physical mouse drag behavior was not independently verified.
