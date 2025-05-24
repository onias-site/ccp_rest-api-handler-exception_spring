package com.ccp.web.servlet.request;

import java.io.IOException;
import java.io.InputStream;

import com.ccp.decorators.CcpJsonRepresentation;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

public class CcpJsonServletInputStream extends ServletInputStream{
    private final InputStream jsonInputStream;
    
    public CcpJsonServletInputStream(CcpJsonRepresentation json) {
    	this.jsonInputStream = json.toInputStream();
    }
	
	public boolean isFinished() {
        try {
			int available = jsonInputStream.available();
			return available == 0;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

   
    public boolean isReady() {
        return true;
    }

   
    public void setReadListener(ReadListener listener) {
        throw new UnsupportedOperationException();
    }

   
    public int read() throws IOException {
        int read = this.jsonInputStream.read();
		return read;
    }

}
