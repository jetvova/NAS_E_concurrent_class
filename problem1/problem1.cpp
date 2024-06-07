class Foo {
private:
    mutex m;
    condition_variable cv;
    int turn;
    
public:
    Foo() : turn(0) { }

    void first(function<void()> printFirst) {
        turnWrapper(printFirst, 0);
    }

    void second(function<void()> printSecond) {
        turnWrapper(printSecond, 1);
    }

    void third(function<void()> printThird) {
        turnWrapper(printThird, 2);
    }

    void turnWrapper(function<void()> whichPrint, int whichTurn) {
        unique_lock<mutex> lock(m);
        cv.wait(lock, [this, whichTurn]{return turn == whichTurn;});

        try {
            whichPrint();
            fflush(stdout); // Woe to he who uses std::cout in a multithreaded environment.
        } catch (...) {
            // Abandon all hope, for std::cout can and *WILL* throw exceptions at the least opportune time.
        }
        turn++;

        lock.unlock();
        cv.notify_all();
    }
};
