class WriteAheadSynchronization {
    private final List<Boolean> taskCompletionLogs;
    private final Object logAccessLock = new Object();

    private final List<Object> priorTaskCompletionLocks;
    private final List<Object> currentTaskCompletionLocks;

    public WriteAheadSynchronization(int taskCount) {
        taskCompletionLogs = new ArrayList<>(taskCount);
        priorTaskCompletionLocks = new ArrayList<>(taskCount);
        currentTaskCompletionLocks = new ArrayList<>(taskCount);

        for (int i = 0; i < taskCount; i++) {
            taskCompletionLogs.add(false);
            priorTaskCompletionLocks.add(new Object());
            currentTaskCompletionLocks.add(new Object());
        }
    }

    public void registerAndAwait(Runnable task, int orderPosition) {
        // Has prior task has been accomplished?
        priorTaskCompletionBarrier(orderPosition);

        // Current task is being executed. Note: task must not contain bugs.
        { task.run(); declareCompleted(orderPosition); }

        // Has current task has been accomplished?
        currentTaskCompletionBarrier(orderPosition);
        
        // Succeeding task is being notified of its prior's completion.
        notifySucceedingTask(orderPosition);
    }

    private void priorTaskCompletionBarrier(int thisOrderPosition) {
        synchronized (priorTaskCompletionLocks.get(thisOrderPosition)) {
            while (!isCompleted(thisOrderPosition-1)) {
                try {
                    priorTaskCompletionLocks.get(thisOrderPosition).wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void currentTaskCompletionBarrier(int thisOrderPosition) {
        synchronized (currentTaskCompletionLocks.get(thisOrderPosition)) {
            while (!isCompleted(thisOrderPosition)) {
                try {
                    currentTaskCompletionLocks.get(thisOrderPosition).wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private boolean isCompleted(int thisOrderPosition) {
        if (thisOrderPosition == -1) { return true; }

        synchronized (logAccessLock) {
            return taskCompletionLogs.get(thisOrderPosition);
        }
    }

    private void declareCompleted(int thisOrderPosition) {
        synchronized (logAccessLock) {
            taskCompletionLogs.set(thisOrderPosition, true);
        }

        synchronized (currentTaskCompletionLocks.get(thisOrderPosition)) {
            currentTaskCompletionLocks.get(thisOrderPosition).notify();
        }
    }

    void notifySucceedingTask(int thisOrderPosition) {
        if (thisOrderPosition+1 < priorTaskCompletionLocks.size()) {
            synchronized (priorTaskCompletionLocks.get(thisOrderPosition+1)) {
                priorTaskCompletionLocks.get(thisOrderPosition+1).notify();
            }
        }
    }
}

class Foo {
    private final WriteAheadSynchronization sync;

    public Foo() {
        this.sync = new WriteAheadSynchronization(3);
    }

    public void first(Runnable printFirst) throws InterruptedException {
        sync.registerAndAwait(printFirst, 0);
    }

    public void second(Runnable printSecond) throws InterruptedException {
        sync.registerAndAwait(printSecond, 1);
    }

    public void third(Runnable printThird) throws InterruptedException {
        sync.registerAndAwait(printThird, 2);
    }
}
