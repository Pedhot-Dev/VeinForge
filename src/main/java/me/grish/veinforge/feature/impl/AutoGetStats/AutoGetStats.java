package me.grish.veinforge.feature.impl.AutoGetStats;

import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.AbstractInventoryTask;
import me.grish.veinforge.util.helper.Clock;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * AutoGetStats handles a simple internal queue of AbstractInventoryTask,
 * running one at a time in FIFO order. Goes onto the next task (if any) when one fails.
 */
public class AutoGetStats extends AbstractFeature {

    private static AutoGetStats instance;

    private final Queue<AbstractInventoryTask<?>> taskQueue = new ArrayDeque<>();
    private final Clock delay = new Clock();
    private AbstractInventoryTask<?> currentTask;

    public static AutoGetStats getInstance() {
        if (instance == null) {
            instance = new AutoGetStats();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "AutoGetStats";
    }

    @Override
    protected void onTick() {
        if (!enabled || mc.player == null || mc.level == null)
            return;

        if (currentTask != null) {
            currentTask.onTick();

            if (currentTask.getTaskStatus().isFailure() || currentTask.getTaskStatus().isSuccessful()) {
                currentTask.end();
                currentTask = null;
                delay.schedule(1000);  // 1-second delay between each task
            }
        } else if (delay.isScheduled() && delay.passed() && !taskQueue.isEmpty()) {
            currentTask = taskQueue.poll();
            currentTask.init();
        }
    }

    /**
     * Adds a new task to the queue and starts it immediately if idle.
     */
    public void startTask(AbstractInventoryTask<?> task) {

        if (task == null) return;
        taskQueue.add(task);

        // If no current task running, start this one immediately
        if (currentTask == null) {
            currentTask = taskQueue.poll();
            currentTask.init();
            this.enabled = true;
            this.start();
        }
    }

    public boolean hasFinishedAllTasks() {
        return currentTask == null && taskQueue.isEmpty();
    }
}

