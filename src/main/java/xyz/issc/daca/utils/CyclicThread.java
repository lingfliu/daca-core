package xyz.issc.daca.utils;

public class CyclicThread {
    Runnable task;
    volatile boolean isRunning = true;
    long sleep;
    Thread taskThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(isRunning) {
                try {
                    Thread.sleep(sleep);
                    task.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    });

    public CyclicThread(Runnable task, long sleep) {
        this.task = task;
        this.sleep = sleep;
    }

    public void start() {
        taskThread.start();
    }

    public void quit() {
        isRunning = false;
        this.taskThread.interrupt();
    }
}
