package com.ccp.rest.api.spring.exceptions.handler;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.Callback;

import com.ccp.aop.CcpAllowNullParameter;

/**
 * The error handler of the Jetty server with no stack trace and no cause: the page keeps the status and the message
 * ({@code Ambiguous URI path separator}), but no longer the line {@code CAUSED BY: org.eclipse.jetty...}, which the
 * Jetty 12 handler writes whenever there is a cause, even with the stacks turned off.
 */
public class CcpRestApiJettyErrorHandlerWithoutCauses extends ErrorHandler {

	/** Turns off the stacks and the message in the title. */
	public CcpRestApiJettyErrorHandlerWithoutCauses() {
		this.setShowStacks(false);
		this.setShowMessageInTitle(false);
	}

	/**
	 * Generates the page of the error without its cause.
	 * @param request the request (Jetty may call with any argument null)
	 * @param response the response
	 * @param code the status
	 * @param message the message of the error, or null
	 * @param cause the cause, which is dropped
	 * @param callback the callback of the response
	 * @throws IOException when the page cannot be written
	 */
	@CcpAllowNullParameter
	protected void generateResponse(Request request, Response response, int code, String message, Throwable cause, Callback callback) throws IOException {
		super.generateResponse(request, response, code, message, null, callback);
	}
}
