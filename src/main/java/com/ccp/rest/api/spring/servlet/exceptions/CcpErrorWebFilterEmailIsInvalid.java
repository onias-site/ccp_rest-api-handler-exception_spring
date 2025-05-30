package com.ccp.rest.api.spring.servlet.exceptions;

import java.util.Arrays;

@SuppressWarnings("serial")
public class CcpErrorWebFilterEmailIsInvalid extends RuntimeException {

	public CcpErrorWebFilterEmailIsInvalid(String url, String... filtered) {
		super("The url '"  + url + "' is not composed by none of these values: " + Arrays.asList(filtered));
	}
	
}
