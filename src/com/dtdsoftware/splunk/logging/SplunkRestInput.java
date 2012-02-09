package com.dtdsoftware.splunk.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.splunk.RequestMessage;
import com.splunk.Service;

/**
 * Common REST logic shared by all appenders/handlers
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */

public class SplunkRestInput {

	public static String RECEIVERS_SIMPLE_ENDPOINT = "/services/receivers/simple";
	public static String RECEIVERS_STREAM_ENDPOINT = "/services/receivers/stream";

	public static String RECEIVERS_SIMPLE_ARG_INDEX = "index";
	public static String RECEIVERS_SIMPLE_ARG_SOURCE = "source";
	public static String RECEIVERS_SIMPLE_ARG_SOURCETYPE = "sourcetype";
	public static String RECEIVERS_SIMPLE_ARG_HOST = "host";
	public static String RECEIVERS_SIMPLE_ARG_HOSTREGEX = "host_regex";

	public static String SCHEME_HTTPS = "https";
	public static String SCHEME_HTTP = "http";

	public static String HTTP_METHOD = "POST";

	private Service service;

	private OutputStream ostream;
	private Socket stream = null;
	private Writer writerOut = null;
	private String scheme = SCHEME_HTTPS;

	private RestEventData red;
	private String simpleURL;

	private TrustManager[] trustAll = new TrustManager[] { new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}
	} };

	public SplunkRestInput(String user, String pass, String host, int port,
			RestEventData red, boolean stream) throws Exception {

		this.service = new Service(host, port);
		this.service.login(user, pass);
		this.red = red;
		this.simpleURL = getURL(RECEIVERS_SIMPLE_ENDPOINT, red);
		if (stream)
			initStream();

	}

	private void initStream() throws Exception {

		this.stream = open(service.getHost(), service.getPort());
		this.ostream = stream.getOutputStream();
		this.writerOut = new OutputStreamWriter(this.ostream, "UTF8");

		String header = String.format("%s %s HTTP/1.1\r\n" + "Host: %s:%d\r\n"
				+ "Accept-Encoding: identity\r\n" + "Authorization: %s\r\n"
				+ "X-Splunk-Input-Mode: Streaming\r\n\r\n", HTTP_METHOD,
				getURL(RECEIVERS_STREAM_ENDPOINT, red), service.getHost(),
				service.getPort(), service.getToken());

		this.writerOut.write(header);
		this.writerOut.flush();

	}

	public void closeStream() {
		try {
			writerOut.flush();
			writerOut.close();
			stream.close();
		} catch (IOException e) {

		}
	}

	/**
	 * Open a socket to this service.
	 * 
	 * @return Socket
	 * @throws IOException
	 */
	private Socket open(String host, int port) throws IOException {
		if (this.scheme.equals("https")) {
			SSLSocketFactory sslsocketfactory;
			try {
				SSLContext context = SSLContext.getInstance("SSL");
				context.init(null, trustAll, new java.security.SecureRandom());
				sslsocketfactory = context.getSocketFactory();
			} catch (Exception e) {
				throw new RuntimeException("Error installing trust manager.");
			}
			return sslsocketfactory.createSocket(host, port);
		}
		return new Socket(host, port);
	}

	public void sendEvent(String message) {

		RequestMessage request = new RequestMessage(HTTP_METHOD);
		request.setContent(message);
		service.send(simpleURL, request);
	}

	private String getURL(String endpoint, RestEventData red) {

		StringBuffer url = new StringBuffer();

		url.append(endpoint).append("?");

		if (red.getIndex().length() > 0)
			url.append(RECEIVERS_SIMPLE_ARG_INDEX).append("=").append(
					red.getIndex());
		if (red.getSource().length() > 0)
			url.append("&").append(RECEIVERS_SIMPLE_ARG_SOURCE).append("=")
					.append(red.getSource());
		if (red.getSourcetype().length() > 0)
			url.append("&").append(RECEIVERS_SIMPLE_ARG_SOURCETYPE).append("=")
					.append(red.getSourcetype());
		if (red.getHost().length() > 0)
			url.append("&").append(RECEIVERS_SIMPLE_ARG_HOST).append("=")
					.append(red.getHost());
		if (red.getHostRegex().length() > 0)
			url.append("&").append(RECEIVERS_SIMPLE_ARG_HOSTREGEX).append("=")
					.append(red.getHostRegex());

		return url.toString();

	}

	public void streamEvent(String message) {

		try {
			writerOut.write(message + "\n");
			writerOut.flush();
		} catch (IOException e) {

		}

	}
}
