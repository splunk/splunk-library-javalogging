package com.splunk.logging;

/**
 * @copyright
 *
 * Copyright 2013-2015 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import java.util.List;

/**
 * @brief Splunk http event collector middleware implementation.
 *
 * @details A user application can utilize HttpEventCollectorMiddleware to
 * customize the behavior of sending events to Splunk. A user application plugs
 * middleware components to the HttpEventCollectorSender by calling
 * addMiddleware method.
 *
 * HttpEventCollectorResendMiddleware.java is an example of how middleware can
 * be used.
 */
public class HttpEventCollectorMiddleware {

	private HttpSenderMiddleware httpSenderMiddleware = null;
	/**
	 * Post http event collector data
	 *
	 * @param events list
	 * @param sender is http sender
	 * @param callback async callback
	 */
	public void postEvents(final List<HttpEventCollectorEventInfo> events,
			IHttpSender sender,
			IHttpSenderCallback callback) {
		if (httpSenderMiddleware == null) {
			sender.postEvents(events, callback);
		} else {
			httpSenderMiddleware.postEvents(events, sender, callback);
		}
	}

	public void pollAcks(AckManager m,
			IHttpSender sender,
			IHttpSenderCallback callback) {
		if (httpSenderMiddleware == null) {
			sender.pollAcks(m, callback);
		} else {
			httpSenderMiddleware.pollAcks(m, sender, callback);
		}
	}

	/**
	 * Plug a middleware component to the middleware chain.
	 *
	 * @param middleware is a new middleware
	 */
	public void add(final HttpSenderMiddleware middleware) {
		if (httpSenderMiddleware == null) {
			httpSenderMiddleware = middleware;
		} else {
			middleware.next = httpSenderMiddleware;
			httpSenderMiddleware = middleware;
		}
	}  

	/**
	 * An interface that describes an abstract events sender working
	 * asynchronously.
	 */
	public interface IHttpSender {

		public void postEvents(final List<HttpEventCollectorEventInfo> events,
				IHttpSenderCallback callback);

		public void pollAcks(AckManager m,
				IHttpSenderCallback callback);
	}

	/**
	 * Callback methods invoked by events sender.
	 */
	public interface IHttpSenderCallback {

		public void completed(int statusCode, final String reply);

		public void failed(final Exception ex);
	}


  
  	/**
	 * An abstract middleware component.
	 */
	public static abstract class HttpSenderMiddleware {

		private HttpSenderMiddleware next;

		public void postEvents(
				final List<HttpEventCollectorEventInfo> events,
				IHttpSender sender,
				IHttpSenderCallback callback) {
			callNext(events, sender, callback);
		}

		protected void callNext(final List<HttpEventCollectorEventInfo> events,
				IHttpSender sender,
				IHttpSenderCallback callback) {
			if (next != null) {
				next.postEvents(events, sender, callback);
			} else {
				sender.postEvents(events, callback);
			}
		}

		public void pollAcks(AckManager m,
				IHttpSender sender, IHttpSenderCallback callback) {
			callNextAckMiddleware(m, sender, callback);
		}

		protected void callNextAckMiddleware(
				AckManager m, IHttpSender sender,
				IHttpSenderCallback callback) {
			if (next != null) {
				next.callNextAckMiddleware(m, sender, callback);
			} else {
				sender.pollAcks(m, callback);
			}
		}
	}
}
