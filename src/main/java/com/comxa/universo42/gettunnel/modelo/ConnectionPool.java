package com.comxa.universo42.gettunnel.modelo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionPool {

    private InetSocketAddress address;
    private int poolSize;
    private LinkedList<Socket> socketsPool;
    private LinkedList<ThreadConnect> threadsPool;
    private Lock poolLock = new ReentrantLock();
    private boolean isRunning;

    public ConnectionPool(InetSocketAddress address, int poolSize) {
        this.poolSize = poolSize;
        this.address = address;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getCurrentSize() {
        if (socketsPool != null) {
            synchronized (poolLock) {
                return socketsPool.size();
            }
        }

        return 0;
    }

    public void close() {
        isRunning = false;
        if (threadsPool != null) {
            synchronized (threadsPool) {
                for (ThreadConnect thread : threadsPool) {
                    thread.close();
                }
            }
        }

        if (socketsPool != null) {
            synchronized (socketsPool) {
                for (Socket socket : socketsPool) {
                    try {
                        socket.close();
                    } catch(Throwable e) {}
                }
            }
            socketsPool.clear();
        }
    }

    public synchronized Socket getConnectedSocket() {
        if (socketsPool == null) {
            initPool();
        }

        while (isRunning && socketsPool.size() == 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (isRunning) {
            Socket s = pollSocket();
            startThreads(1);
            return s;
        }

        return null;
    }

    private void initPool() {
        isRunning = true;
        socketsPool = new LinkedList<Socket>();
        threadsPool = new LinkedList<ThreadConnect>();

        startThreads(getPoolSize());
    }

    private void startThreads(int qtd) {
        synchronized (threadsPool) {
            for (int i = 0; i < qtd; i++) {
                ThreadConnect thread = new ThreadConnect();
                thread.start();
                threadsPool.add(thread);
            }
        }
    }

    private void removeThread(ThreadConnect thread) {
        synchronized (threadsPool) {
            threadsPool.remove(thread);
        }
    }

    private void addSocket(Socket socket) {
        if (socket != null) {
            synchronized (socketsPool) {
                socketsPool.add(socket);
            }
        }
    }

    private Socket pollSocket() {
        synchronized (socketsPool) {
            return socketsPool.poll();
        }
    }


    private class ThreadConnect implements Runnable {
        private Socket socket;

        private void start() {
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                while (isRunning) {
                    try {
                        addSocket(connectTo(address));
                        break;
                    } catch(IOException e) {
                        if (isRunning) {
                            e.printStackTrace();
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e1) {}
                        }
                    }
                }
            } finally {
                removeThread(this);
            }
        }

        private Socket connectTo(InetSocketAddress address) throws IOException {
            while (isRunning) {
                socket = new Socket();
                try {
                    socket.connect(address, 30000);
                    break;
                } catch(SocketTimeoutException e) {
                    socket.close();
                }
            }

            return socket;
        }

        public void close() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
