package com.comxa.universo42.gettunnel.modelo.listener;

import java.util.Iterator;
import java.util.LinkedList;

public class LogBox extends Caller {
    private LogListener listener;
    private LinkedList<String> logList;
    private int boxSize = 50;
    private long lastCount = -1;
    private long logCount;

    public LogBox(long milliseconds) {
        super(milliseconds);
        this.logList = new LinkedList<String>();
    }

    public void setBoxSize(int boxSize) {
        this.boxSize = boxSize;
    }

    public void setListener(LogListener listener) {
        this.listener = listener;
    }

    public synchronized void clearLog() {
        this.logList.clear();
    }

    public synchronized void addLog(String log) {
        if (this.logList.size() == boxSize) {
            this.logList.poll();
        }
        this.logList.add(log);
        logCount++;
    }

    @Override
    public void stop() {
        super.stop();
        call();
    }

    @Override
    public synchronized void call() {
        if (listener != null) {
            if (lastCount != logCount) {
                listener.onLog(toString());
                lastCount = logCount;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        Iterator<String> iterator = this.logList.iterator();

        while (iterator.hasNext()) {
            builder.append(iterator.next() + "\n");
        }

        return builder.toString();
    }
}
