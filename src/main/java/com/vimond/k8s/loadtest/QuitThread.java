package com.vimond.k8s.loadtest;

public class QuitThread extends Thread {
    private final int sleep;

    public QuitThread(int sleep) {
        this.sleep = sleep;
    }

    @Override
    public void run() {
        super.run();

        System.out.println("QuitThread: Quitting in " + sleep + "ms");
        try {
            Thread.sleep(sleep);
        } catch (Exception e) {
            System.out.println("Got exception while sleeping: " + e.getMessage());
        }
        System.out.println("Exiting after " + this.sleep + "ms");
        System.exit(0);
    }
}
