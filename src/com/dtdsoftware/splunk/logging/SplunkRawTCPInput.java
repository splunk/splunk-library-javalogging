package com.dtdsoftware.splunk.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

/**
 * Common Raw TCP logic shared by all appenders/handlers
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */

public class SplunkRawTCPInput {

	// connection props
	private String host = "";
	private int port;

	// streaming objects
	private Socket streamSocket = null;
	private OutputStream ostream;
	private Writer writerOut = null;

	/**
	 * Create a SplunkRawTCPInput object to send events to Splunk via Raw TCP
	 * 
	 * @param host
	 *            REST endppoint host
	 * @param port
	 *            REST endpoint port
	 * @throws Exception
	 */
	public SplunkRawTCPInput(String host, int port) throws Exception {

		this.host = host;
		this.port = port;

		openStream();

	}

	/**
	 * open the stream
	 * 
	 */
	private void openStream() throws Exception {

		streamSocket = new Socket(host, port);
		if (streamSocket.isConnected()) {
			ostream = streamSocket.getOutputStream();
			writerOut = new OutputStreamWriter(ostream, "UTF8");
		}

	}

	/**
	 * close the stream
	 */
	public void closeStream() {
		try {

			if (writerOut != null) {
				writerOut.flush();
				writerOut.close();
				if (streamSocket != null)
					streamSocket.close();
			}
		} catch (Exception e) {
		}
	}

	/**
	 * send an event via stream
	 * 
	 * @param message
	 */
	public void streamEvent(String message) {

		try {

			if (writerOut != null) {

				writerOut.write(message + "\n");
				writerOut.flush();

			}

		} catch (IOException e) {
			try {
				closeStream();
			} catch (Exception e1) {
			}

			try {
				openStream();
			} catch (Exception e2) {
			}
		}
	}
}
