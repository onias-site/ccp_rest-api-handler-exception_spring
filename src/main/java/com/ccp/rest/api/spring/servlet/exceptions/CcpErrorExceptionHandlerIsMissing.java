package com.ccp.rest.api.spring.servlet.exceptions;

@SuppressWarnings("serial")
/**
 * Exceção lançada quando {@code CcpRestApiExceptionHandlerSpring.genericExceptionHandler} não foi
 * configurado e ocorre uma exceção genérica não tratada.
 */
public class CcpErrorExceptionHandlerIsMissing extends RuntimeException{
	
	public CcpErrorExceptionHandlerIsMissing(Throwable e) {
		super("genericExceptionHandler must has an instance ", e);
	}
	
}
