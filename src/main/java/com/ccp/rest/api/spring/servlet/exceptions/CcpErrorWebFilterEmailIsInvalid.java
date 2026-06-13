package com.ccp.rest.api.spring.servlet.exceptions;

import java.util.Arrays;

@SuppressWarnings("serial")
/**
 * Exceção lançada por {@code CcpValidEmailFilter} quando nenhum dos segmentos configurados
 * é encontrado na URL, impossibilitando a extração do e-mail.
 */
public class CcpErrorWebFilterEmailIsInvalid extends RuntimeException {

	public CcpErrorWebFilterEmailIsInvalid(String url, String... filtered) {
		super("The url '"  + url + "' is not composed by none of these values: " + Arrays.asList(filtered));
	}
	
}
