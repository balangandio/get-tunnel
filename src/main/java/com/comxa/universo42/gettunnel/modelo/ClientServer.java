package com.comxa.universo42.gettunnel.modelo;

import com.comxa.universo42.gettunnel.modelo.listener.ByteCounter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientServer implements Runnable {

	private InetSocketAddress listening;
	private String target;
	private InetSocketAddress server;
	private String serverAddr;
	private int serverPort;

	private ServerSocket ss;

	private List<Connection> connections = new LinkedList<Connection>();
	private Lock connectionsLock = new ReentrantLock();
	private AtomicBoolean isStopped = new AtomicBoolean(false);
	private ByteCounter counter;
	private ConnectionConfig config;

	public ClientServer(String listeningAddr, int listeningPort, String target, String serverAddr, int serverPort) {
		this.listening = new InetSocketAddress(listeningAddr, listeningPort);
		this.target = target;
		this.config = new ConnectionConfig();
		this.config.setHostHeader(serverAddr);
		this.serverAddr = serverAddr;
		this.serverPort = serverPort;
	}

	public ConnectionConfig getConfig() {
		return config;
	}

	public void setConfig(ConnectionConfig config) {
		this.config = config;
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
			if (counter != null) {
				counter.stop();
			}

			if (ss != null) {
				try {
					ss.close();
				} catch(IOException e) {}
			}

			List<Connection> connections;

			synchronized (connectionsLock) {
				connections = new LinkedList<Connection>(this.connections);
			}

			for (Connection connection : connections) {
				connection.close();
			}
		}
	}

	@Override
	public void run() {
		try {
			handleLog("listening on " + listening.getPort());
			server = new InetSocketAddress(serverAddr, serverPort);

			ss = new ServerSocket();
			ss.bind(listening);

			while (true) {
				Socket socket = ss.accept();

				acceptConnection(socket);
			}
		} catch(IOException e) {
			handleLog("error. " + e.getMessage());
		} finally {
			close();
		}
	}

	private void acceptConnection(Socket socket) {
		Connection conn = new Connection(socket, server, target, config) {
			@Override
			public void onLog(String log) {
				ClientServer.this.onLog(log);
			}

			@Override
			public void onClosed() {
				synchronized (connectionsLock) {
					connections.remove(this);
				}
			}
		};

		if (counter != null) {
			conn.setByteCounter(counter);
		}

		synchronized (connectionsLock) {
			connections.add(conn);
		}

		conn.start();
		handleLog("opening connection - " + conn.getId());
	}

	private void handleLog(String log) {
		if (!isStopped.get()) {
			onLog("ClientServer: " + log);
		}
	}

	public void onLog(String log) {}
}
