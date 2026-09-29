package com.ccp.rest.api.spring.servlet.request;

import java.io.ByteArrayInputStream;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * Returns the original, already read request body without interpreting it. Used by
 * {@code CcpPutSessionValuesRequestWrapper} when the body is not json: the original stream can only be
 * read once, and Spring still needs it to decide the status (415 for an unsupported media type).
 */
class CcpRawServletInputStream extends ServletInputStream {

	private final ByteArrayInputStream body;

	CcpRawServletInputStream(byte[] body) {
		this.body = new ByteArrayInputStream(body);
	}

	public boolean isFinished() {
		boolean finished = this.body.available() == 0;
		return finished;
	}

	public boolean isReady() {
		return true;
	}

	public void setReadListener(ReadListener listener) {
		UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException();
		throw unsupportedOperationException;
	}

	public int read() {
		int read = this.body.read();
		return read;
	}
}
