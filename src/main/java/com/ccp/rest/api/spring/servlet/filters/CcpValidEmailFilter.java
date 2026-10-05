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
 * Servlet filter that answers the CORS headers, ends OPTIONS requests and answers 400 when the e-mail of the URL (the
 * path segment right after one of the filtered prefixes) is not valid.
 */
public class CcpValidEmailFilter implements Filter{
	
	/** The path prefixes that precede the e-mail (e.g. "login/"). */
	private final String[] filtered;
	
	/**
	 * Builds the filter.
	 * @param filtered the path prefixes that precede the e-mail
	 */
	public CcpValidEmailFilter(String... filtered) {
		this.filtered = filtered;
	}

	/**
	 * Builds the filter.
	 * @param filtered the path prefixes that precede the e-mail
	 * @return the filter
	 */
	public static CcpValidEmailFilter getEmailSyntaxFilter(String... filtered) {
		CcpValidEmailFilter ccpValidEmailFilter = new CcpValidEmailFilter(filtered);
		return ccpValidEmailFilter;
	}

	/**
	 * Checks the e-mail of the decoded URL and goes on only when it is valid.
	 * @param req the request
	 * @param res the response
	 * @param chain the filter chain
	 * @throws CcpErrorWebFilterEmailIsInvalid when the URL has none of the prefixes
	 */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain){

		HttpServletRequest request = (HttpServletRequest) req;

		HttpServletResponse response = (HttpServletResponse) res;

		response.setHeader(CcpCorsSpecialWords.Access_Control_Allow_Origin.getValue(), "*");
		response.setHeader(CcpCorsSpecialWords.Access_Control_Allow_Methods.getValue(), "POST, GET, OPTIONS, DELETE, HEAD, PATCH");
		response.setHeader(CcpCorsSpecialWords.Access_Control_Max_Age.getValue(), "3600");

		response.setHeader(CcpCorsSpecialWords.Access_Control_Allow_Headers.getValue(),
				"Access-Control-Allow-Headers, X-Requested-With, authorization, Sessiontoken, Email, Content-Type, Authorization, Access-Control-Request-Methods, Access-Control-Request-Headers");

		String method = request.getMethod();

		boolean optionsMethod = "OPTIONS".equalsIgnoreCase(method);

		if (optionsMethod) {
			return;
		}

		StringBuffer requestURL = request.getRequestURL();
		String requestUrlText = requestURL.toString();
		CcpStringDecorator requestUrlDecorator = new CcpStringDecorator(requestUrlText);
		CcpUrlDecorator urlDecorator = requestUrlDecorator.url();
		String url = urlDecorator.asDecoded();
		String email = this.extractEmail(url);
		CcpStringDecorator emailText = new CcpStringDecorator(email);
		CcpEmailDecorator emailDecorator = emailText.email();
		var isValidEmail = emailDecorator.isValid();
		boolean invalidEmail = false == isValidEmail;
		if(invalidEmail) {
			int badRequestStatus = CcpProcessStatusDefault.BAD_REQUEST.asNumber();
			response.setStatus(badRequestStatus);
			return;
		}
		try {
			chain.doFilter(request, response);
			
		} catch (Exception e) {
			CcpErrorValidEmailFilterChain ccpErrorValidEmailFilterChain = new CcpErrorValidEmailFilterChain(e);
			throw ccpErrorValidEmailFilterChain;
		} 
	}

	/**
	 * Returns the path segment right after the first filtered prefix found in the URL.
	 * @param url the decoded URL
	 * @return the e-mail candidate
	 * @throws CcpErrorWebFilterEmailIsInvalid when the URL has none of the prefixes
	 */
	private String extractEmail(String url) {
		
		for (String filteredPrefix : this.filtered) {
			int prefixIndex = url.indexOf(filteredPrefix);
			boolean prefixNotFound = prefixIndex < 0;
			if(prefixNotFound) {
				continue;
			}
			int prefixStart = url.indexOf(filteredPrefix);
			int stringLength = filteredPrefix.length();
			int sum = prefixStart + stringLength;
			String urlSecondPiece = url.substring(sum);
			String[] split = urlSecondPiece.split("/");
			String email = split[0];
			return email;
		}
		CcpErrorWebFilterEmailIsInvalid ccpErrorWebFilterEmailIsInvalid = new CcpErrorWebFilterEmailIsInvalid(url, this.filtered);

		throw ccpErrorWebFilterEmailIsInvalid;
	}
	
	/**
	 * Nothing to initialize.
	 * @param filterConfig the filter configuration
	 * @throws ServletException never
	 */
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	
	/** Nothing to release. */
	public void destroy() {
		
	}

	/**
	 * Describes the filter (the array is shown by its identity).
	 * @return the description
	 */
	public String toString() {
		String textWithFiltered = "CcpValidEmailFilter [filtered=" + filtered;
		String filterAsText = textWithFiltered + "]";
		return filterAsText;
	}

	/** Raised when the URL has none of the filtered prefixes. */
	@SuppressWarnings("serial")
	public static class CcpErrorWebFilterEmailIsInvalid extends RuntimeException {
		/**
		 * Builds the error naming the URL and the prefixes.
		 * @param url the URL
		 * @param filtered the prefixes
		 */
		private CcpErrorWebFilterEmailIsInvalid(String url, String... filtered) {
			super("The url '"  + url + "' is not composed by none of these values: " + Arrays.asList(filtered));
		}
	}

	/** Wraps a failure of the filter chain. */
	@SuppressWarnings("serial")
	private static class CcpErrorValidEmailFilterChain extends RuntimeException {
		/**
		 * Wraps the cause.
		 * @param cause the original failure
		 */
		private CcpErrorValidEmailFilterChain(Throwable cause) {
			super(cause);
		}
	}
}
