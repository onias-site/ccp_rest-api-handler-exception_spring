package com.ccp.rest.api.spring.servlet.filters;

import java.util.Arrays;

import com.ccp.decorators.CcpStringDecorator;
import com.ccp.process.CcpProcessStatusDefault;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.ccp.decorators.CcpUrlDecorator;
import com.ccp.decorators.CcpEmailDecorator;

/**
 * Filtro Spring que valida o e-mail embutido na URL antes de encaminhar a requisição.
 * Configura os headers CORS e retorna 400 se o e-mail extraído da URL for inválido.
 */
public class CcpValidEmailFilter implements Filter{
	
	private final String[] filtered;
	
	public CcpValidEmailFilter(String... filtered) {
		this.filtered = filtered;
	}

	public static CcpValidEmailFilter getEmailSyntaxFilter(String... filtered) {
		CcpValidEmailFilter ccpValidEmailFilter = new CcpValidEmailFilter(filtered);
		return ccpValidEmailFilter;
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain){

		HttpServletRequest request = (HttpServletRequest) req;

		HttpServletResponse response = (HttpServletResponse) res;

		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, HEAD, PATCH");
		response.setHeader("Access-Control-Max-Age", "3600");

		response.setHeader("Access-Control-Allow-Headers",
				"Access-Control-Allow-Headers, X-Requested-With, authorization, Sessiontoken, Email, Content-Type, Authorization, Access-Control-Request-Methods, Access-Control-Request-Headers");

		String method = request.getMethod();

		boolean optionsMethod = "OPTIONS".equalsIgnoreCase(method);

		if (optionsMethod) {
			return;
		}

		StringBuffer requestURL = request.getRequestURL();
		String toString = requestURL.toString();
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator(toString);
		CcpUrlDecorator ccpStringDecoratorUrl = ccpStringDecorator.url();
		String url = ccpStringDecoratorUrl.asDecoded();
		String email = this.extractEmail(url);
		CcpStringDecorator ccpStringDecorator2 = new CcpStringDecorator(email);
		CcpEmailDecorator email2 = ccpStringDecorator2.email();
		var valid = email2.isValid();
		boolean invalidEmail = false == valid;
		if(invalidEmail) {
			int asNumber = CcpProcessStatusDefault.BAD_REQUEST.asNumber();
			response.setStatus(asNumber);
			return;
		}
		try {
			chain.doFilter(request, response);
			
		} catch (Exception e) {
			CcpErrorValidEmailFilterChain ccpErrorValidEmailFilterChain = new CcpErrorValidEmailFilterChain(e);
			throw ccpErrorValidEmailFilterChain;
		} 
	}

	private String extractEmail(String url) {
		
		for (String string : this.filtered) {
			int indexOf = url.indexOf(string);
			boolean indexOfMenor = indexOf < 0;
			if(indexOfMenor) {
				continue;
			}
			int indexOf2 = url.indexOf(string);
			int stringLength = string.length();
			int sum = indexOf2 + stringLength;
			String urlSecondPiece = url.substring(sum);
			String[] split = urlSecondPiece.split("/");
			String email = split[0];
			return email;
		}
		CcpErrorWebFilterEmailIsInvalid ccpErrorWebFilterEmailIsInvalid = new CcpErrorWebFilterEmailIsInvalid(url, this.filtered);

		throw ccpErrorWebFilterEmailIsInvalid;
	}
	
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	
	public void destroy() {
		
	}

	public String toString() {
		String valorMais = "CcpValidEmailFilter [filtered=" + filtered;
		String valorMaisMais = valorMais + "]";
		return valorMaisMais;
	}

	@SuppressWarnings("serial")
	public static class CcpErrorWebFilterEmailIsInvalid extends RuntimeException {
		private CcpErrorWebFilterEmailIsInvalid(String url, String... filtered) {
			super("The url '"  + url + "' is not composed by none of these values: " + Arrays.asList(filtered));
		}
	}

	@SuppressWarnings("serial")
	private static class CcpErrorValidEmailFilterChain extends RuntimeException {
		private CcpErrorValidEmailFilterChain(Throwable cause) {
			super(cause);
		}
	}
}
