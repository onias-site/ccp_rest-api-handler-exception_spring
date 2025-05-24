package com.ccp.web.servlet.exceptions;

import java.util.Arrays;

@SuppressWarnings("serial")
public class CcpInvalidUrlToFilter extends RuntimeException {

	public CcpInvalidUrlToFilter(String url, String... filtered) {
		super("The url '"  + url + "' is not composed by none of these values: " + Arrays.asList(filtered));
	}
	
}
