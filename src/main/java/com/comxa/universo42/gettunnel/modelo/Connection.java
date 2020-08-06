package com.comxa.universo42.gettunnel.modelo;

import com.comxa.universo42.gettunnel.modelo.listener.ByteCounter;
import com.comxa.universo42.embaralhador.Embaralhador;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.comxa.universo42.gettunnel.modelo.Constants.*;
import static com.comxa.universo42.gettunnel.modelo.Constants.Client.*;

public class Connection implements Runnable {

	private InetSocketAddress server;
	private String target;

	private Socket appSocket;
	private InputStream appIn;
	private OutputStream appOut;

	private Socket readSocket;
	private InputStream readIn;
	private OutputStream readOut;

	private ConnectionPool pool;
	private BlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<byte[]>(TAM_SEND_QUEUE);
	private boolean appEndOfStream;

	private ByteCounter counter;
	private int id;
	private String connectionId;

	private ConnectionConfig config;
	private boolean isStopped;

	public Connection(Socket appSocket, InetSocketAddress server, String target, ConnectionConfig config) {
		this.id = appSocket.getPort();
		this.appSocket = appSocket;
		this.server = server;
		this.target = target;
		this.config = config;
		this.pool = new ConnectionPool(server, 3);
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

	public int getId() {
		return this.id;
	}

	public String getConnectionId() {
		return this.connectionId;
	}

	private void setConnectionId(String connId) {
		this.connectionId = connId;
	}

	public void start() {
		new Thread(this).start();
	}

	public  synchronized void close() {
		handleLog("closed");
		if (!isStopped) {
			isStopped = true;

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

			pool.close();

			onClosed();
		}
	}

	@Override
	public void run() {
		try {
			if (this.getConfig().getHostHeader() == null) {
				this.getConfig().setHostHeader(this.server.getAddress().getHostAddress() + ":" + this.server.getPort());
			}

			appIn = appSocket.getInputStream();
			appOut = appSocket.getOutputStream();

			new ThreadAppRead().start();

			if (!establishConnection())
				return;

			new ThreadRead().start();

			ServerResponse response;

			handleLog("running...");

			while (!appEndOfStream && !isStopped) {
				response = sendQueueData();

				if (!response.isValid()) {
					handleLog("server sends a invalid response!");
					break;
				}

				if (MSG_END_OF_STREAM.equals(response.getStatusMsg())) {
					handleLog("server sends end of stream signal");
					return;
				}
			}

			if (appEndOfStream) {
				handleLog("sending connection close");
				sendConnectionClose();
			}
		} catch(IOException e) {
			handleLog("error. " + e.getMessage());
		} finally {
			close();
		}
	}

	private void setReadSocket(Socket socket) throws IOException {
		readSocket = socket;
		readIn = socket.getInputStream();
		readOut = socket.getOutputStream();
	}

	private class ThreadRead implements Runnable {
		public void start() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			try {
            	byte[] buffer = new byte[TAM_BUFFER_SERVER_RESPONDE_CONTENT];
                int len;
                while (!readSocket.isClosed() && (len = readIn.read(buffer)) != -1) {
					if (counter != null) {
						counter.addDownloadBytes(len);
					}

					appOut.write(buffer, 0, len);
				}

			} catch(IOException e) {
				handleLog("thread read error. " + e.getMessage());
			} finally {
				close();
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
                while (!appSocket.isClosed() && (len = appIn.read(buffer)) != -1) {
					if (!addToSendQueue(buffer, len)) {
						break;
					}
				}

                if (len == -1)
                	sendQueue.offer(END_OF_STREAM);
            } catch (IOException e) {
            	handleLog("thread app read error. " + e.getMessage());
				close();
			}
		}

		private boolean addToSendQueue(byte[] buffer, int len) {
	        byte[] bytes = Arrays.copyOf(buffer, len);

	        while (!isStopped) {
	            try {
	                if (sendQueue.offer(bytes, MAX_SECONDS_TIME_OUT_OFFER_SEND_QUEUE, TimeUnit.SECONDS))
	                    return true;
	            } catch (InterruptedException e) {
	            	handleLog("interruptedException on addToQueue!");
	            }
	        }

	        return false;
		}
	}


	private byte[] readSendQueue() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] bytes = null;
		try {
			bytes = sendQueue.poll(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int count = 0, max = MAX_QTD_BYTES_TO_SEND - TAM_RECEIVE_BUFFER_APP;

		while (bytes != null) {
			count += bytes.length;

			baos.write(bytes);

			if (count > max)
				break;

			if (bytes == END_OF_STREAM) {
				appEndOfStream = true;
				break;
			}
			bytes = sendQueue.poll();
		}

		if (appEndOfStream)
			handleLog("receive app stream ended!");

		return baos.toByteArray();
	}

	private boolean establishConnection() throws IOException {
		ServerResponse response = sendConnectionCreate();

		if (!response.isValid()) {
			handleLog("server sends a invalid response!");
			return false;
		}

		String id = response.getId();
		setConnectionId(id);

		if (!MSG_CONNECTION_CREATED.equals(response.getStatusMsg()) || id == null) {
			handleLog("connection couldn't be created!");
			if (response.getStatusMsg() != null)
				handleLog("server msg - " + response.getStatusMsg());
			return false;
		}
		handleLog("connection created - " + id);

		Socket readSocket = pool.getConnectedSocket();

		if (readSocket != null) {
			setReadSocket(readSocket);

			handleLog("sending connection complete");
			sendConnectionComplete(readOut, id);

			response = new ServerResponse(readIn);

			if (!response.read()) {
				handleLog("server sends a invalid response!");
				return false;
			}

			if (!MSG_CONNECTION_COMPLETED.equals(response.getStatusMsg())) {
				handleLog("connetion couldn't be completed!");
				if (response.getStatusMsg() != null)
					handleLog("server msg - " + response.getStatusMsg());
				return false;
			}

			handleLog("connection completed");

			return true;
		}

		return false;
	}

	private ServerResponse sendConnectionCreate() throws IOException {
		Socket serverSocket = pool.getConnectedSocket();

		ServerResponse response = new ServerResponse();

		if (serverSocket != null) {
			try {
				StringBuilder builder = new StringBuilder();

				builder.append("GET / HTTP/1.1\r\n");
				builder.append("Host: " + this.config.getHostHeader() + "\r\n");
				builder.append("User-Agent: MySuperGetTunnel\r\n");
				builder.append(ACTION_HEADER + ACTION_CREATE + "\r\n");
				builder.append(TARGET_HEADER + target + "\r\n");
				if (this.config.getPass() != null) {
					builder.append(PASS_HEADER + new String(Embaralhador.embaralhar(this.config.getPass().getBytes())) + "\r\n");
				}
				if (this.config.getBodyInject() != null) {
					builder.append(BODY_HEADER + this.config.getBodyInject().length() + "\r\n");
				}

				builder.append("Connection: close\r\n\r\n");

				if (this.config.getBodyInject() != null) {
					builder.append(this.config.getBodyInject());
				}

				handleLog("sending connection create");
				OutputStream out = serverSocket.getOutputStream();
				out.write(builder.toString().getBytes());

				InputStream in = serverSocket.getInputStream();
				response.setInputStream(in);
				response.read();

				out.close();
				in.close();
			} finally {
				serverSocket.close();
			}
		}

		return response;
	}

	private void sendConnectionComplete(OutputStream out, String id) throws IOException {
		if (out != null) {
			StringBuilder builder = new StringBuilder();

			builder.append("GET / HTTP/1.1\r\n");
			builder.append("Host: " + this.config.getHostHeader() + "\r\n");
			builder.append("User-Agent: MySuperGetTunnel\r\n");
			builder.append(ACTION_HEADER + ACTION_COMPLETE + "\r\n");
			builder.append(ID_HEADER + id + "\r\n");
			if (this.config.getBodyInject() != null) {
				builder.append(BODY_HEADER + this.config.getBodyInject().length() + "\r\n");
			}

			builder.append("Connection: close\r\n\r\n");

			if (this.config.getBodyInject() != null) {
				builder.append(this.config.getBodyInject());
			}

			out.write(builder.toString().getBytes());
		}
	}

	private DataSender sender;
	private ServerResponse serverResp = new ServerResponse();
	private ServerResponse sendQueueData() throws IOException {
		byte []bytes = readSendQueue();
		int len = bytes.length;

		boolean retry = false;

		do {
			Socket serverSocket = pool.getConnectedSocket();

			if (serverSocket != null) {
				try {
					try {
						OutputStream out = serverSocket.getOutputStream();
						InputStream in = serverSocket.getInputStream();

						bytes = Base64.toBase64(bytes, 0, bytes.length);
						if (sender == null) {
							sender = new DataSender(getConnectionId());
						}
						sender.send(bytes, out);
						serverResp.setInputStream(in);
						serverResp.read();
						out.close();
						in.close();

						if (serverResp.isValid()) {
							retry = false;
						} else {
							throw new IOException("invalid server response");
						}
					} finally {
						serverSocket.close();
					}
				} catch(IOException e) {
					handleLog("error on sending data: " + e.getMessage());
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					retry = true;
				}
			}
		} while (!isStopped && !appEndOfStream && retry);

		if (counter != null) {
			counter.addUploadBytes(len);
		}

		return serverResp;
	}

	private ServerResponse sendConnectionClose() throws IOException {
		Socket serverSocket = pool.getConnectedSocket();

		ServerResponse response = new ServerResponse();

		if (serverSocket != null) {
			try {
				serverSocket.connect(server);
				OutputStream out = serverSocket.getOutputStream();
				InputStream in = serverSocket.getInputStream();

				StringBuilder builder = new StringBuilder();

				builder.append("GET / HTTP/1.1\r\n");
				builder.append("Host: " + this.config.getHostHeader() + "\r\n");
				builder.append("User-Agent: MySuperGetTunnel\r\n");
				builder.append(ACTION_HEADER + ACTION_CLOSE + "\r\n");
				builder.append(ID_HEADER + getConnectionId() + "\r\n");

				if (this.config.getBodyInject() != null) {
					builder.append(BODY_HEADER + this.config.getBodyInject().length() + "\r\n");
				}

				builder.append("Connection: close\r\n\r\n");

				if (this.config.getBodyInject() != null) {
					builder.append(this.config.getBodyInject());
				}

				out.write(builder.toString().getBytes());

				response.setInputStream(in);
				response.read();

				out.close();
				in.close();
			} finally {
				serverSocket.close();
			}
		}

		return response;
	}


	private void handleLog(String log) {
		if (!isStopped)
			onLog("Connection: " + this.id + " - " +log);
	}

	public void onLog(String log) {}

	public void onClosed() {}

	private class DataSender {
		private String reqHead;
		private String reqTail;

		public DataSender(String conntectionId) {
			this.reqHead = "GET / HTTP/1.1\r\n"
					+ "Host: " + getConfig().getHostHeader() + "\r\n"
					+ "User-Agent: MySuperGetTunnel\r\n"
					+ ACTION_HEADER + ACTION_DATA + "\r\n"
					+ ID_HEADER + conntectionId + "\r\n"
					+ LENGTH_HEADER;

			String str = "\r\n";

			if (getConfig().getBodyInject() != null) {
				str += BODY_HEADER + getConfig().getBodyInject().length() + "\r\n";
			}

			str += "Connection: close\r\n\r\n";

			if (getConfig().getBodyInject() != null) {
				str += getConfig().getBodyInject();
			}

			this.reqTail = str;
		}

		public void send(byte[] bytes, OutputStream out) throws IOException {
			String head = reqHead + bytes.length;

			if (bytes.length > 0) {
				head += "\r\n" + DATA_HEADER;
				out.write(head.getBytes());
				out.write(bytes);
			} else {
				out.write(head.getBytes());
			}

			out.write(reqTail.getBytes());
		}
	}
}
