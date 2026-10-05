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

	/** The original body. */
	private final ByteArrayInputStream body;

	/**
	 * Exposes the original body again.
	 * @param body the bytes of the body
	 */
	CcpRawServletInputStream(byte[] body) {
		this.body = new ByteArrayInputStream(body);
	}

	/**
	 * Tells whether every byte was read.
	 * @return {@code true} when nothing is left
	 */
	public boolean isFinished() {
		boolean finished = this.body.available() == 0;
		return finished;
	}

	/**
	 * Always ready: the content is in memory.
	 * @return {@code true}
	 */
	public boolean isReady() {
		return true;
	}

	/**
	 * Non-blocking reading is not supported.
	 * @param listener the listener
	 * @throws UnsupportedOperationException always
	 */
	public void setReadListener(ReadListener listener) {
		UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException();
		throw unsupportedOperationException;
	}

	/**
	 * Reads the next byte.
	 * @return the byte, or -1 at the end
	 */
	public int read() {
		int read = this.body.read();
		return read;
	}
}
