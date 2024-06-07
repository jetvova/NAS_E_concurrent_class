class Foo {
    private Object lock;
    private int turn;

    public Foo() {
        lock = new Object();
        turn = 0;
    }

    public void first(Runnable printFirst) throws InterruptedException {
        turnWrapper(printFirst, 0);
    }

    public void second(Runnable printSecond) throws InterruptedException {
        turnWrapper(printSecond, 1);
    }

    public void third(Runnable printThird) throws InterruptedException {
        turnWrapper(printThird, 2);
    }

    private void turnWrapper(Runnable whichPrint, int whichTurn) throws InterruptedException {
        synchronized (lock) {
            while (turn != whichTurn) {
                lock.wait();
            }

            try {
                whichPrint.run();
                System.out.flush(); // Woe to he who uses std::cout in a multithreaded environment.
            } catch (Exception e) {
                // Abandon all hope, for std::cout can and *WILL* throw exceptions at the least opportune time.
            }
            turn++;

            lock.notifyAll();
        }
    }
}
