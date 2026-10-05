package com.ccp.rest.api.spring.servlet.request;

import java.io.IOException;
import java.io.InputStream;

import com.ccp.decorators.CcpJsonRepresentation;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * {@code ServletInputStream} implementation that delegates reading to the {@code InputStream}
 * obtained from a {@code CcpJsonRepresentation}, allowing the HTTP request body to be replaced
 * with a programmatically built JSON.
 */
public class CcpJsonServletInputStream extends ServletInputStream{
    /** The UTF-8 bytes of the compact JSON. */
    private final InputStream jsonInputStream;
    
    /**
     * Exposes the JSON as the request body.
     * @param json the body
     */
    public CcpJsonServletInputStream(CcpJsonRepresentation json) {
    	this.jsonInputStream = json.toInputStream();
    }
	
	/**
	 * Tells whether every byte was read.
	 * @return {@code true} when nothing is left
	 */
	public boolean isFinished() {
		int available;
		try {
			available = this.jsonInputStream.available();
			boolean nothingLeftToRead = available == 0;
			return nothingLeftToRead;
		} catch (IOException e) {
			CcpErrorServletInputStreamAvailable ccpErrorServletInputStreamAvailable = new CcpErrorServletInputStreamAvailable(e);
			throw ccpErrorServletInputStreamAvailable;
		}
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
     * @throws IOException never in practice
     */
    public int read() throws IOException {
        int read = this.jsonInputStream.read();
		return read;
    }


	/** Wraps a failure to check the available bytes. */
	@SuppressWarnings("serial")
	private static class CcpErrorServletInputStreamAvailable extends RuntimeException {
		/**
		 * Wraps the cause.
		 * @param cause the original failure
		 */
		private CcpErrorServletInputStreamAvailable(Throwable cause) {
			super(cause);
		}
	}
}
