package com.comxa.universo42.gettunnel.modelo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import com.comxa.universo42.gettunnel.modelo.listener.ByteCounter;

import static com.comxa.universo42.gettunnel.modelo.Constants.*;
import static com.comxa.universo42.gettunnel.modelo.Constants.Client.*;

public class Connection implements Runnable {

    private InetSocketAddress server;
    private String target;

    private Socket appSocket;
    private InputStream appIn;
    private OutputStream appOut;

    private Socket writeSocket;
    private InputStream writeIn;
    private OutputStream writeOut;

    private Socket readSocket;
    private InputStream readIn;

    private ConnectionConfig config;
    private int id;

    private ByteCounter counter;
    private int threadEndCount;
    private AtomicBoolean isStopped = new AtomicBoolean(false);

    public Connection(Socket appSocket, InetSocketAddress server, String target, ConnectionConfig config) {
        this.id = appSocket.getPort();
        this.appSocket = appSocket;
        this.server = server;
        this.target = target;
        this.config = config;
    }

    public ConnectionConfig getConfig() {
        return config;
    }

    public void setConfig(ConnectionConfig config) {
        this.config = config;
    }

    public int getId() {
        return this.id;
    }

    public void setByteCounter(ByteCounter counter) {
        this.counter = counter;
    }

    public void start() {
        new Thread(this).start();
    }

    public void close() {
        handleLog("closed");
        if (!isStopped.getAndSet(true)) {
            if (writeSocket != null) {
                try {
                    writeSocket.close();
                } catch(IOException e) {}
            }

            if (readSocket != null) {
                try {
                    readSocket.close();
                } catch(IOException e) {}
            }

            if (appSocket != null) {
                try {
                    appSocket.close();
                } catch(IOException e) {}
            }

            onClosed();
        }
    }

    private synchronized void finallyClose() {
        threadEndCount++;

        if (threadEndCount == 2) {
            close();
        }
    }

    @Override
    public void run() {
        try {
            if (!establishConnection()) {
                return;
            }

            appIn = appSocket.getInputStream();
            appOut = appSocket.getOutputStream();

            sendDataRequest(writeOut);

            new ThreadAppRead().start();
            new ThreadRead().start();
        } catch(IOException e) {
            handleLog("error. " + e.getMessage());
            close();
        }
    }

    private class ThreadRead implements Runnable {
        public void start() {
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[TAM_BUFFER_SERVER_RESPONSE_CONTENT];
                int len;
                while ((len = readIn.read(buffer)) != -1) {
                    if (counter != null) {
                        counter.addDownloadBytes(len);
                    }

                    appOut.write(buffer, 0, len);
                }

                appSocket.shutdownOutput();
            } catch(IOException e) {
                handleLog("thread read error. " + e.getMessage());
                close();
            } finally {
                finallyClose();
            }
        }
    }

    private class ThreadAppRead implements Runnable {
        public void start() {
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[TAM_RECEIVE_BUFFER_APP];
                int len = 0;
                while ((len = appIn.read(buffer)) != -1) {
                    if (counter != null) {
                        counter.addUploadBytes(len);
                    }

                    writeOut.write(buffer, 0, len);
                }

                writeSocket.shutdownOutput();
            } catch (IOException e) {
                handleLog("thread app read error. " + e.getMessage());
                close();
            } finally {
                finallyClose();
            }
        }
    }

    private boolean establishConnection() throws IOException {
        writeSocket = new Socket();
        writeSocket.connect(server);
        writeIn = writeSocket.getInputStream();
        writeOut = writeSocket.getOutputStream();

        handleLog("sending connection create");
        sendConnectionCreate(writeOut);

        ServerResponse response = new ServerResponse(writeIn);

        if (!response.read()) {
            throw new IOException("server sends a invalid response!");
        }

        String id = response.getId();

        if (!response.getStatusMsg().equals(MSG_CONNECTION_CREATED) || id == null) {
            throw new IOException("connection couldn't be created: " + response.getStatusMsg());
        }
        handleLog("connection created - " + id);

        readSocket = new Socket();
        readSocket.connect(server);
        readIn = readSocket.getInputStream();

        handleLog("sending connection complete");
        sendConnectionComplete(readSocket.getOutputStream(), id);

        response = new ServerResponse(readIn);

        if (!response.read()) {
            throw new IOException("server sends a invalid response!");
        }

        if (!response.getStatusMsg().equals(MSG_CONNECTION_COMPLETED)) {
            throw new IOException("connetion couldn't be completed!");
        }
        handleLog("connection completed");

        return true;
    }

    private void sendConnectionCreate(OutputStream out) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append("GET / HTTP/1.1\r\n");
        builder.append("Host: " + this.config.getHostHeader() + "\r\n");
        builder.append("User-Agent: GetTunnelClient\r\n");
        builder.append(ACTION_HEADER + ACTION_CREATE + "\r\n");
        if (this.config.getPass() != null)
            builder.append(PASS_HEADER + this.config.getPass() + "\r\n");
        builder.append(TARGET_HEADER + target + "\r\n");

        builder.append("Content-Type: application/octet-stream\r\n");
        builder.append(CONTENT_HEADER + "0\r\n");
        builder.append("Connection: Keep-Alive\r\n\r\n");

        out.write(builder.toString().getBytes());
    }

    private void sendConnectionComplete(OutputStream out, String id) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append("GET / HTTP/1.1\r\n");
        builder.append("Host: " + this.config.getHostHeader() + "\r\n");
        builder.append("User-Agent: GetTunnelClient\r\n");
        builder.append(ACTION_HEADER + ACTION_COMPLETE + "\r\n");
        builder.append(ID_HEADER + id + "\r\n");
        //builder.append("Content-Type: application/octet-stream\r\n");
        //builder.append(CONTENT_HEADER + "0\r\n");
        //builder.append("Connection: Keep-Alive\r\n\r\n");
        //builder.append("X-Body: 5\r\n");
        builder.append("Connection: close\r\n\r\n");

        out.write(builder.toString().getBytes());
    }

    private void sendDataRequest(OutputStream out) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append("GET / HTTP/1.1\r\n");
        builder.append("Host: " + this.config.getHostHeader() + "\r\n");
        builder.append("User-Agent: GetTunnelClient\r\n");
        builder.append(ACTION_HEADER + ACTION_DATA + "\r\n");
        builder.append("Content-Type: application/octet-stream\r\n");
        builder.append(CONTENT_HEADER + Integer.MAX_VALUE + "\r\n");
        builder.append("Connection: Keep-Alive\r\n\r\n");

        out.write(builder.toString().getBytes());
    }

    private void handleLog(String log) {
        if (!isStopped.get()) {
            onLog("Connection: " + this.id + " - " +log);
        }
    }

    public void onLog(String log) {}

    public void onClosed() {}
}
