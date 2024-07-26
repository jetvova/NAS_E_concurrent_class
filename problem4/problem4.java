class DiningPhilosophers {
    private final Object licenseToEat = new Object();
    private final boolean[] isTaken = new boolean[5];
    private final LinkedList<Integer> queue = new LinkedList<>();

    public DiningPhilosophers() {
        for (int i = 0; i < 5; i++) {
            isTaken[i] = false;
        }
    }

    public void wantsToEat(int philosopher,
                           Runnable pickLeftFork,
                           Runnable pickRightFork,
                           Runnable eat,
                           Runnable putLeftFork,
                           Runnable putRightFork) throws InterruptedException {
        int leftFork = philosopher;
        int rightFork = (philosopher + 1) % 5;

        synchronized (licenseToEat) {
            queue.addLast(philosopher);

            // Wait until it's this philosopher's turn, forks are available, and precedence over neighbors is ensured
            while (!canPickUpForks(philosopher, leftFork, rightFork)) {
                licenseToEat.wait();
            }

            // Pick up forks
            isTaken[leftFork] = true;
            isTaken[rightFork] = true;
            queue.removeFirstOccurrence(philosopher);
        }

        // Execute the actions outside the lock to avoid blocking other threads
        synchronized (this) {
            pickLeftFork.run();
            pickRightFork.run();
            eat.run();
            putLeftFork.run();
            putRightFork.run();
        }
        
        synchronized (licenseToEat) {
            // Release forks
            isTaken[leftFork] = false;
            isTaken[rightFork] = false;
            // Notify all waiting philosophers
            licenseToEat.notifyAll();
        }
    }

    private boolean canPickUpForks(int philosopher, int leftFork, int rightFork) {
        // Check if both forks are available
        if (isTaken[leftFork] || isTaken[rightFork]) {
            return false;
        }

        // Ensure the philosopher has precedence compared to neighbors
        int myIndex = queue.indexOf(philosopher);
        int leftNeighborIndex = queue.indexOf((philosopher + 4) % 5);
        int rightNeighborIndex = queue.indexOf((philosopher + 1) % 5);
        int secondLeftNeighborIndex = queue.indexOf((philosopher + 3) % 5);
        int secondRightNeighborIndex = queue.indexOf((philosopher + 2) % 5);

        boolean higherThanLeft = (leftNeighborIndex == -1) || (myIndex < leftNeighborIndex);
        boolean higherThanRight = (rightNeighborIndex == -1) || (myIndex < rightNeighborIndex);
        boolean leftUpSlope = (leftNeighborIndex > -1 && secondLeftNeighborIndex > -1 && (secondLeftNeighborIndex < leftNeighborIndex));
        boolean rightUpSlope = (rightNeighborIndex > -1 && secondRightNeighborIndex > -1 && (secondRightNeighborIndex < rightNeighborIndex));

        boolean hasPrecedence = (higherThanLeft && higherThanRight)
                             || (higherThanLeft && rightUpSlope)
                             || (leftUpSlope && higherThanRight)
                             || (leftUpSlope && rightUpSlope);

        return hasPrecedence;
    }
}
