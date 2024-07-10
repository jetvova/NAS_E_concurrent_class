class FizzBuzz {
private:
    int n; // The maximum number to check
    int currentNumber = 1; // Current number being processed

    mutex mtx; // Mutex for thread synchronization
    condition_variable cv; // Condition variable for thread synchronization

public:
    // Constructor to initialize 'n'
    FizzBuzz(int n) {
        this->n = n;
    }

    // Function for handling "fizz" printing
    void fizz(function<void()> printFizz) {
        wrapper(printFizz, [this](){return currentNumber%3 == 0 && currentNumber%5 != 0;});
    }

    // Function for handling "buzz" printing
    void buzz(function<void()> printBuzz) {
        wrapper(printBuzz, [this](){return currentNumber%3 != 0 && currentNumber%5 == 0;});
    }

    // Function for handling "fizzbuzz" printing
    void fizzbuzz(function<void()> printFizzBuzz) {
        wrapper(printFizzBuzz, [this](){return currentNumber%3 == 0 && currentNumber%5 == 0;});
    }

    // Function for handling number printing
    void number(function<void(int)> printNumber) {
        wrapper([printNumber, this](){ return printNumber(currentNumber); },
                [this](){return currentNumber%3 != 0 && currentNumber%5 != 0;});
    }

private:
    // Helper function to run the task with proper synchronization
    void wrapper(function<void()> task, function<bool()> condition) {
        while (true) {
            {
                // Lock the mutex
                unique_lock<mutex> lock(mtx);

                // Wait until the condition is satisfied or the number exceeds 'n'
                cv.wait(lock, [condition, this]() { return (condition() || (currentNumber > n)); });

                // If the number exceeds 'n', notify all threads and exit the loop
                if (currentNumber > n) {
                    cv.notify_all();
                    return;
                }
            }

            // Execute the print function
            task();

            {
                // Lock the mutex to update the current number
                unique_lock<mutex> lock(mtx);
                currentNumber += 1;
            }

            // Notify all threads about the state update
            cv.notify_all();
        }
    }
};
