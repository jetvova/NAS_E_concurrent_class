class FooBar {
private:
    int n;
    mutex m;
    int state = 0;
    condition_variable cv;

    void execute(function<void()> print, int whichTurn) {
        unique_lock<mutex> lck(m);
        for (int i = 0; i < n; i++) {
            cv.wait(lck, [this, whichTurn] { return state == whichTurn; });

            // Run the passed function (printFoo or printBar)
            print();

            // Switch state between foo and bar
            state = 1 - state;
            cv.notify_all();
        }
    }

public:
    FooBar(int n) {
        this->n = n;
    }

    void foo(function<void()> printFoo) {
        execute(printFoo, 0);
    }

    void bar(function<void()> printBar) {
        execute(printBar, 1);
    }
};
