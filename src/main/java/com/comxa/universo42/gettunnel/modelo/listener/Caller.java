package com.comxa.universo42.gettunnel.modelo.listener;

public abstract class Caller implements Runnable {
    private long milliseconds;
    private boolean running;

    public Caller(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public void start() {
        new Thread(this).start();
    }

    public void stop() {
        this.running = false;
    }

    public abstract void call();

    @Override
    public void run() {
        this.running = true;

        while (this.running) {
            call();

            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
