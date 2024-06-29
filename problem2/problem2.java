class FooBar {
    private int n;
    private int state = 0;

    public FooBar(int n) {
        this.n = n;
    }

    private void execute(Runnable print, int whichTurn) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            synchronized(this) {
                while (state != whichTurn) {
                    wait();
                }
                // Run the passed function (printFoo or printBar)
                print.run();
                // Switch state between foo and bar
                state = 1 - state;
                notifyAll();
            }
        }
    }

    public void foo(Runnable printFoo) throws InterruptedException {
        execute(printFoo, 0);
    }

    public void bar(Runnable printBar) throws InterruptedException {
        execute(printBar, 1);
    }
}
