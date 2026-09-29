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
    private final InputStream jsonInputStream;
    
    public CcpJsonServletInputStream(CcpJsonRepresentation json) {
    	this.jsonInputStream = json.toInputStream();
    }
	
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
   
    public boolean isReady() {
        return true;
    }

   
    public void setReadListener(ReadListener listener) {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException();
        throw unsupportedOperationException;
    }

   
    public int read() throws IOException {
        int read = this.jsonInputStream.read();
		return read;
    }


	@SuppressWarnings("serial")
	private static class CcpErrorServletInputStreamAvailable extends RuntimeException {
		private CcpErrorServletInputStreamAvailable(Throwable cause) {
			super(cause);
		}
	}
}
