package mlvisualizer;

import java.util.concurrent.CountDownLatch;
import java.util.function.IntSupplier;
import javafx.application.Platform;

/** One daemon thread waits between steps; all algorithm and UI work stays on the FX thread. */
public class AnimationWorker extends Thread {
    private final Object lock = new Object();
    private final Runnable stepAction;
    private final IntSupplier delayMillis;
    private volatile boolean paused = true;
    private volatile boolean stopped;

    public AnimationWorker(Runnable stepAction, IntSupplier delayMillis) {
        super("ML animation worker");
        this.stepAction = stepAction;
        this.delayMillis = delayMillis;
        setDaemon(true);
    }

    public void play() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
    }

    public void pausePlayback() {
        paused = true;
    }

    public boolean isPlaying() {
        return !paused && !stopped;
    }

    public void shutdown() {
        stopped = true;
        synchronized (lock) { lock.notifyAll(); }
        interrupt();
    }

    @Override public void run() {
        while (!stopped) {
            try {
                synchronized (lock) {
                    while (paused && !stopped) lock.wait();
                }
                if (stopped) break;
                CountDownLatch finished = new CountDownLatch(1);
                Platform.runLater(() -> {
                    try {
                        if (!paused && !stopped) stepAction.run();
                    } finally {
                        finished.countDown();
                    }
                });
                finished.await(); // Never queue a second step before the first is drawn.
                Thread.sleep(delayMillis.getAsInt());
            } catch (InterruptedException exception) {
                if (stopped) break;
            }
        }
    }
}
