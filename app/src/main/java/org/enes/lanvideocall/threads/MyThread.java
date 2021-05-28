package org.enes.lanvideocall.threads;

public class MyThread extends Thread {

    public int test;

    public void kill() {
        if(!isInterrupted()) {
            interrupt();
        }
    }

}
