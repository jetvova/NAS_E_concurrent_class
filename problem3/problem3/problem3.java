public class FizzBuzz {
    private final int n; // The maximum number to check
    private int currentNumber = 1; // Current number being processed

    // Constructor to initialize `n`
    public FizzBuzz(int n) {
        this.n = n;
    }

    // Function for handling "fizz" printing
    public void fizz(Runnable printFizz) throws InterruptedException {
        wrapper(printFizz, () -> currentNumber % 3 == 0 && currentNumber % 5 != 0);
    }

    // Function for handling "buzz" printing
    public void buzz(Runnable printBuzz) throws InterruptedException {
        wrapper(printBuzz, () -> currentNumber % 3 != 0 && currentNumber % 5 == 0);
    }

    // Function for handling "fizzbuzz" printing
    public void fizzbuzz(Runnable printFizzBuzz) throws InterruptedException {
        wrapper(printFizzBuzz, () -> currentNumber % 3 == 0 && currentNumber % 5 == 0);
    }

    // Function for handling number printing
    public void number(IntConsumer printNumber) throws InterruptedException {
        wrapper(() -> printNumber.accept(currentNumber), () -> currentNumber % 3 != 0 && currentNumber % 5 != 0);
    }

    // Helper function to run the task with proper synchronization
    private void wrapper(Runnable task, Condition condition) throws InterruptedException {
        while (true) {
            // Acquire the lock to wait and check the condition
            synchronized (this) {
                while (!(condition.test() || currentNumber > n)) {
                    this.wait();
                }

                // If the number exceeds `n`, notify all threads and exit the loop
                if (currentNumber > n) {
                    notifyAll();
                    return;
                }
            }

            // Execute the print function without holding the lock
            task.run();

            // Acquire the lock again to update the current number and notify
            synchronized (this) {
                currentNumber += 1;
                // Notify all threads about the state update
                this.notifyAll();
            }
        }
    }

    // Interface for the condition check
    private interface Condition {
        boolean test();
    }
}
