package com.comxa.universo42.gettunnel.modelo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.comxa.universo42.gettunnel.modelo.Constants.*;
import static com.comxa.universo42.gettunnel.modelo.Constants.Client.*;

public class ServerResponse {

	private InputStream in;
	private int statusCode;
	private String statusMsg;
	private String id;
	private boolean isValid;

	public ServerResponse() {}

	public ServerResponse(InputStream in) {
		this.in = in;
	}

	public String getId() {
		return this.id;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusMsg() {
		return statusMsg;
	}

	public void setInputStream(InputStream in) {
		this.in = in;
	}

	public boolean isValid() {
		return this.isValid;
	}

	public boolean read() throws IOException {
		String linha;
		int count = 0;
		boolean ignore = false;
		isValid = true;

		linha = getLinha(in, MAX_LEN_SERVER_RESPONSE_HEAD);

		if (linha == null)
			return false;

		if (!setStatusLine(linha))
			return false;

		while (!linha.equals("\r\n") && count < MAX_LEN_SERVER_RESPONSE_HEAD) {
			linha = getLinha(in, MAX_LEN_SERVER_RESPONSE_HEAD);

			if (linha == null)
				break;

			if (!ignore && linha.startsWith(ID_HEADER)) {
				ignore = true;

				id = getHeaderVal(linha);

				if (id == null)
					isValid = false;
			}

			count += linha.length();
		}

		return isValid;
	}

	public boolean readToEof() throws IOException {
		isValid = true;

		String req = readToEof(in, 4096, MAX_LEN_SERVER_RESPONSE_HEAD);

		if (req.isEmpty()) {
			isValid = false;
		} else {
			if (!setStatusLine(req)) {
				isValid = false;
			} else {
				if (req.contains(ID_HEADER)) {
					id = getHeaderVal(req, ID_HEADER);

					if (id == null) {
						isValid = false;
					}
				}
			}
		}

		return isValid;
	}

	private String readToEof(InputStream in, int bufferLen, int maxBytes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte buffer[] = new byte[bufferLen];
		int len, count = 0;

		while ((len = in.read(buffer)) != -1 && count <= maxBytes) {
			baos.write(buffer, 0, len);
			count += len;
		}

		return new String(baos.toByteArray());
	}

	private String getLinha(InputStream in, int maxQtdBytes) throws IOException {
		StringBuilder builder = new StringBuilder();
		int b = 0;
		int count = 0;

		while ((b = in.read()) != -1 && count < maxQtdBytes) {
			count++;
			builder.append((char)b);

			if (b == '\r') {
				b = in.read();
				count++;

				if (b == -1)
					break;

				builder.append((char)b);

				if (b == '\n')
					break;
			}
		}

		return (b == -1) ? null : builder.toString();
	}

	private boolean setStatusLine(String statusLine) {
		int start = statusLine.indexOf(' ');

		if (start == -1)
			return false;

		start++;
		int end = statusLine.indexOf(' ', start);

		if (end == -1)
			return false;

		try {
			statusCode = Integer.parseInt(statusLine.substring(start, end));
		} catch(NumberFormatException e) {
			return false;
		}

		end++;
		start = statusLine.indexOf("\r", end);

		if (start == -1)
			statusMsg = statusLine.substring(end);
		else
			statusMsg = statusLine.substring(end, start);

		return true;
	}

	private String getHeaderVal(String req, String header) {
		int index = req.indexOf(header);

		if (index != -1) {
			req = req.substring(index);

			return getHeaderVal(req);
		}

		return null;
	}

	private String getHeaderVal(String header) {
		int ini = header.indexOf(':');

		if (ini == -1)
			return null;

		ini += 2;

		int fim = header.indexOf("\r\n");

		if (fim == -1)
			return header.substring(ini);

		return header.substring(ini, fim);
	}

    /*private void streamTransfer(InputStream in, OutputStream out, int qtdBytes, int tamBuffer) throws IOException {
    	if (tamBuffer > qtdBytes)
    		tamBuffer = qtdBytes;

    	byte []buffer = new byte[tamBuffer];
    	int len, count = 0, toRead = tamBuffer;

    	while (count < qtdBytes && (len = in.read(buffer, 0, toRead)) != -1) {
    		out.write(buffer, 0, len);
    		count += len;

    		if (qtdBytes - count < tamBuffer)
    			toRead = qtdBytes - count;
    	}
    }*/
}