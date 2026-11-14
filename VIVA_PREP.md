# ML Algorithm Visualizer - Viva Preparation

These answers refer to the submitted source code. Keep them short in a viva, then point to the relevant class if asked for detail.

## Architecture and Java OOP

**1. What is the aim of the project?**  
To show the intermediate steps of four simple machine learning algorithms in a JavaFX desktop application.

**2. What is the role of `Main`?**  
`Main` extends JavaFX `Application`. It builds the controls and graphs, generates data, selects an algorithm, and displays explanations.

**3. Why is `MLAlgorithm` abstract?**  
All algorithms have a name, a next step, a reset operation, and a completion state, but the implementations differ. The abstract class defines that common contract.

**4. Which classes inherit from `MLAlgorithm`?**  
`KMeans`, `DecisionTree`, `LinearRegression`, and `KNN`.

**5. Where is polymorphism used?**  
`Main` keeps an `MLAlgorithm algorithm` reference and calls `algorithm.nextStep()`. Java chooses the current subclass's overridden method at runtime.

**6. What is method overriding here?**  
Each algorithm provides its own `nextStep()`, `reset()`, `isComplete()`, and `getName()` implementation declared by `MLAlgorithm`.

**7. What is encapsulation in this project?**  
`DataPoint` stores private coordinates, class label, and cluster index. Other classes use getters and `setCluster()` rather than editing fields directly. `TreeNode` also keeps its fields private.

**8. What is the difference between inheritance and composition in this source?**  
`DecisionTree` inherits from `MLAlgorithm`. It also contains `TreeNode` objects as its root and children; that relationship is composition.

**9. Where are arrays used?**  
K-Means uses `double[][]` for centroid coordinates. KNN uses `double[]` for distances. The tree uses local arrays or collections during calculations.

**10. Why use `ArrayList`?**  
The number of data points and samples in tree branches varies. `ArrayList<DataPoint>` makes adding and dividing those points straightforward. KNN also uses an `ArrayList<Integer>` for sorted indices.

**11. Why keep cluster index and class label separate in `DataPoint`?**  
The class label is the known answer for supervised classification. The cluster index is assigned by unsupervised K-Means and can change during its steps.

## K-Means

**12. What does K-Means do?**  
It groups points into K clusters by repeatedly assigning each point to the nearest centroid and moving centroids to the means of their assigned points.

**13. How is distance calculated?**  
With Euclidean distance: `sqrt((x1-x2)^2 + (y1-y2)^2)`.

**14. What are the two visible K-Means steps?**  
The first assigns points to nearest centroids. The second updates centroid coordinates using cluster averages.

**15. What happens if a cluster becomes empty?**  
`KMeans.updateCentroids()` keeps that cluster's previous centroid rather than dividing by zero.

**16. When does K-Means stop?**  
When centroid movement is below a small tolerance, or after 100 completed iterations.

## Decision Tree and Gini

**17. What is Gini impurity?**  
It measures how mixed a node's class labels are. For the fraction `p` of class 1, `Gini = 1 - p^2 - (1-p)^2`. A pure node has Gini 0.

**18. How is the best split found?**  
`DecisionTree` tries X and Y thresholds between adjacent distinct sample values, calculates the weighted Gini of the resulting children, and chooses the best improvement.

**19. Why is Gini weighted after a split?**  
The two child nodes may have different sample counts. Each child's impurity contributes in proportion to its size.

**20. What does one tree step do?**  
It finds one pending node recursively and either splits it into Yes/No children or turns it into a leaf.

**21. When does tree splitting stop?**  
At maximum depth, when labels are pure, when there are too few points for two valid leaves, when no split improves Gini, or when the 127-node safety limit would be exceeded.

**22. How is a test point classified?**  
After the tree is complete, it follows each `<= threshold` Yes branch or No branch until a leaf, then returns that leaf's predicted class.

## Linear Regression

**23. What model is fitted?**  
A straight line `y = slope*x + intercept`.

**24. What is gradient descent?**  
It changes parameters in the direction opposite the error gradient. `LinearRegression.nextStep()` makes exactly one batch update using all data points.

**25. What is MSE?**  
Mean squared error is the average of `(predictedY - actualY)^2`. Lower MSE means the current line fits that dataset better.

**26. Why divide coordinates by 100 during training?**  
The graph uses 0-100 coordinates. Dividing both axes by 100 keeps the gradient scale moderate and helps numerical stability. The displayed intercept is converted back to graph units.

**27. What does the learning rate control?**  
The size of each parameter update. An excessively large value can make training unstable; the GUI restricts it to a positive value at most 1.

## KNN

**28. Is KNN trained like regression?**  
No. It keeps labeled points and compares a selected test point to them when classifying.

**29. What does each KNN step reveal?**  
First all distances, then one nearest neighbor per click, then the final class vote.

**30. How does KNN break ties?**  
Equal distances use original point order. Equal class votes predict class 0, so the result is repeatable.

**31. Why must a test point be selected first?**  
Distances depend on its X and Y coordinates. `KNN.nextStep()` throws `InvalidParameterException` if none has been selected.

## Exceptions and multithreading

**32. Why create `InvalidParameterException`?**  
It gives parameter and input errors a clear type and message. `Main` catches them and shows a JavaFX Alert instead of a stack trace.

**33. Give examples of invalid input handling.**  
Invalid K, empty data, tree depth outside 1-15, invalid learning rate, nonnumeric test coordinates, nonfinite points, and a missing KNN test point are handled.

**34. What thread performs automatic timing?**  
One daemon `AnimationWorker` thread. It waits between steps and is paused or resumed through synchronized methods and a lock.

**35. Why use `Platform.runLater`?**  
JavaFX nodes must be updated on the JavaFX Application Thread. The worker only schedules a step; the model and scene are updated in that callback.

**36. How do you prevent multiple workers or a backlog of steps?**  
`Main` creates one worker. `AnimationWorker` waits for a scheduled step to finish with a `CountDownLatch` before sleeping and scheduling another.

**37. How do Pause, Next Step, and Reset interact?**  
Pause stops automatic advancement without resetting the model. Next Step pauses first and advances once. Reset pauses and calls the current algorithm's overridden `reset()`.

## JavaFX, Maven, and limitations

**38. Why use Canvas for the graph and nodes for the tree?**  
Canvas efficiently draws many points and lines. Separate JavaFX nodes make tree cards, labels, highlighting, zooming, and scrolling easier.

**39. What does CSS do?**  
`style.css` defines the dashboard colors, spacing, rounded controls, tree node styles, and active path colors separately from the algorithm math.

**40. What does Maven manage?**  
`pom.xml` sets Java 17 compilation and downloads JavaFX Controls and Graphics. `mvn clean package` builds; `mvn javafx:run` launches the app.

**41. What are the main project limitations?**  
Only synthetic in-memory data, two numeric features, binary classes, no saved model, at most 100 generated points, and safety caps on tree nodes and K-Means iterations.

**42. How did you verify correctness?**  
`tests/AlgorithmReview.java` checks known distances, means, Gini, predictions, MSE reduction, resets, and invalid inputs. A separate JavaFX smoke check exercised controls, switching, zoom values, resizing, playback, and scene snapshots.
