package com.ccp.rest.api.spring.exceptions.handler;

import org.eclipse.jetty.server.Server;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * The errors that Jetty answers by itself, before any filter or controller (an ambiguous path separator such as
 * {@code %2F}, a control character in the path, a malformed request line), go to the error handler of the server, whose
 * page carried the message, the cause and the whole stack trace of Jetty until 2026-11-10 (found by the local dynamic
 * scan, {@code documentation/deploy/50-seguranca-varredura-dinamica.py}). This handler keeps the status and the page,
 * with no stack trace and no cause.
 */
@Component
public class CcpRestApiJettyErrorPagesWithoutDetails implements WebServerFactoryCustomizer<JettyServletWebServerFactory>, JettyServerCustomizer {

	/**
	 * Adds this customizer of the server to the factory of the web server.
	 * @param factory the factory of the Jetty web server
	 */
	public void customize(JettyServletWebServerFactory factory) {
		factory.addServerCustomizers(this);
	}

	/**
	 * Replaces the error handler of the server by one that shows neither the stack trace nor the cause.
	 * @param server the Jetty server
	 */
	public void customize(Server server) {
		CcpRestApiJettyErrorHandlerWithoutCauses errorHandler = new CcpRestApiJettyErrorHandlerWithoutCauses();
		server.setErrorHandler(errorHandler);
	}
}
