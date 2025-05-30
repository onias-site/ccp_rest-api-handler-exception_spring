package com.ccp.rest.api.spring.servlet.exceptions;

@SuppressWarnings("serial")
public class CcpErrorExceptionHandlerIsMissing extends RuntimeException{
	
	public CcpErrorExceptionHandlerIsMissing(Throwable e) {
		super("genericExceptionHandler must has an instance ", e);
	}
	
}
