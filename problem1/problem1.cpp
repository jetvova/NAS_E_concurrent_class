class WriteAheadSynchronization {
private:
    vector<bool> taskCompletionLogs;
    mutex logAccessMutex;

    vector<condition_variable> priorTaskCompletedCVs;
    vector<condition_variable> currentTaskCompletedCVs;
    vector<mutex> taskExecutionMutexes;

public:
    WriteAheadSynchronization(int taskCount)
        : taskCompletionLogs(taskCount, false),
            priorTaskCompletedCVs(taskCount),
            currentTaskCompletedCVs(taskCount),
            taskExecutionMutexes(taskCount) {}

    void registerAndAwait(function<void()> task, int orderPosition) {
        // Has prior task been accomplished?
        priorTaskCompletionBarrier(orderPosition);

        // Current task is executed. Note: task must not contain bugs.
        executeCurrentTask(task, orderPosition);

        // Has current task been accomplished?
        currentTaskCompletionBarrier(orderPosition);
        
        // Succeeding task is notified of its prior's completion.
        notifySucceedingTask(orderPosition);
    }

private:
    void priorTaskCompletionBarrier(int thisOrderPosition) {
        unique_lock localMemoryFence(taskExecutionMutexes[thisOrderPosition]);
        while (!isCompleted(thisOrderPosition - 1)) {
            priorTaskCompletedCVs[thisOrderPosition].wait(localMemoryFence);
        }
    }

    void executeCurrentTask(function<void()> task, int thisOrderPosition) {
        task();
        markAsCompleted(thisOrderPosition);
        currentTaskCompletedCVs[thisOrderPosition].notify_one();
    }

    void currentTaskCompletionBarrier(int thisOrderPosition) {
        unique_lock localMemoryFence(taskExecutionMutexes[thisOrderPosition]);
        while (!isCompleted(thisOrderPosition)) {
            currentTaskCompletedCVs[thisOrderPosition].wait(localMemoryFence);
        }
    }
    
    void notifySucceedingTask(int thisOrderPosition) {
        if (thisOrderPosition+1 < priorTaskCompletedCVs.size()) {
            priorTaskCompletedCVs[thisOrderPosition+1].notify_one();
        }
    }

    bool isCompleted(int thisOrderPosition) {
        if (thisOrderPosition == -1) { return true; }

        {   scoped_lock currentlyUsingLogs(logAccessMutex);
            return taskCompletionLogs[thisOrderPosition];
        }
    }

    void markAsCompleted(int thisOrderPosition) {
        {   scoped_lock currentlyUsingLogs(logAccessMutex);
            taskCompletionLogs[thisOrderPosition] = true;
        }
    }
};

class Foo {
private:
    WriteAheadSynchronization sync;

public:
    Foo() : sync(WriteAheadSynchronization(3)) { }

    void first(function<void()> printFirst) {
        sync.registerAndAwait(printFirst, 0);
    }

    void second(function<void()> printSecond) {
        sync.registerAndAwait(printSecond, 1);
    }

    void third(function<void()> printThird) {
        sync.registerAndAwait(printThird, 2);
    }
};
