package com.ccp.rest.api.spring.servlet.filters;


import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.rest.api.spring.servlet.request.CcpPutSessionValuesRequestWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that answers the CORS headers, ends OPTIONS requests right away and wraps the request in
 * {@code CcpPutSessionValuesRequestWrapper}, which adds the session values to the JSON body and runs the task over it.
 * The task runs only when the body is read and is a non-empty JSON: a request without body skips it.
 */
public class CcpPutSessionValuesAndExecuteTaskFilter implements Filter{
	
	/** Filter that only adds the session values. */
	public static final CcpPutSessionValuesAndExecuteTaskFilter TASKLESS = new  CcpPutSessionValuesAndExecuteTaskFilter(CcpOtherConstants.DO_NOTHING);
	
	/** Business run over the body enriched with the session values (e.g. a session validation). */
	private final CcpBusiness task;
	
	/**
	 * Builds the filter.
	 * @param task the business run over the enriched body
	 */
	public CcpPutSessionValuesAndExecuteTaskFilter(CcpBusiness task) {
		this.task = task;
	}

	/**
	 * Sets the CORS headers, ends OPTIONS requests and goes on with the wrapped request.
	 * @param req the request
	 * @param res the response
	 * @param chain the filter chain
	 * @throws CcpErrorPutSessionValuesFilterChain wrapping any failure of the chain
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

		CcpPutSessionValuesRequestWrapper requestWrapper = new CcpPutSessionValuesRequestWrapper(request, this.task);
		try {
			chain.doFilter(requestWrapper, response);
		} catch (Exception e) {
			CcpErrorPutSessionValuesFilterChain ccpErrorPutSessionValuesFilterChain = new CcpErrorPutSessionValuesFilterChain(e);
			throw ccpErrorPutSessionValuesFilterChain;
		} 
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

	/** Wraps a failure of the filter chain. */
	@SuppressWarnings("serial")
	private static class CcpErrorPutSessionValuesFilterChain extends RuntimeException {
		/**
		 * Wraps the cause.
		 * @param cause the original failure
		 */
		private CcpErrorPutSessionValuesFilterChain(Throwable cause) {
			super(cause);
		}
	}
}
