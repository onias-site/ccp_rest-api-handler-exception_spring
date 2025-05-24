package com.ccp.web.servlet.exceptions;

@SuppressWarnings("serial")
public class GenericExceptionHandlerMissing extends RuntimeException{
	
	public GenericExceptionHandlerMissing(Throwable e) {
		super("genericExceptionHandler must has an instance ", e);
	}
	
}
