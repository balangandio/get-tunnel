package com.comxa.universo42.gettunnel.modelo.listener;

import com.comxa.universo42.gettunnel.modelo.listener.Caller;
import com.comxa.universo42.gettunnel.modelo.listener.CounterListener;

public class ByteCounter extends Caller {
    private long uploadBytes;
    private long downloadBytes;
    private CounterListener listener;

    public ByteCounter(long milliseconds) {
        super(milliseconds);
    }

    public void setListener(CounterListener listener) {
        this.listener = listener;
    }

    public synchronized void addUploadBytes(long count) {
        this.uploadBytes += count;
    }

    public synchronized void addDownloadBytes(long count) {
        this.downloadBytes += count;
    }

    public synchronized void reset() {
        this.uploadBytes = this.downloadBytes = 0;
    }

    private synchronized long[] getBytes() {
        return new long[] {uploadBytes, downloadBytes};
    }

    @Override
    public void call() {
        long []bytes = getBytes();

        listener.countBytes(bytes[0], bytes[1]);
    }
}
