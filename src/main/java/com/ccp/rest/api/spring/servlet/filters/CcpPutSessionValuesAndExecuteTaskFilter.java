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
 * Spring filter that wraps the request in {@code CcpPutSessionValuesRequestWrapper},
 * injecting session values (email, IP, sessionToken, userAgent) and running an optional
 * {@code CcpBusiness} before handing over to the next filter. Configures CORS and
 * ignores OPTIONS requests.
 */
public class CcpPutSessionValuesAndExecuteTaskFilter implements Filter{
	
	public static final CcpPutSessionValuesAndExecuteTaskFilter TASKLESS = new  CcpPutSessionValuesAndExecuteTaskFilter(CcpOtherConstants.DO_NOTHING);
	
	private final CcpBusiness task;
	
	public CcpPutSessionValuesAndExecuteTaskFilter(CcpBusiness task) {
		this.task = task;
	}

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


	public void init(FilterConfig filterConfig) throws ServletException {
		
	}
	
	public void destroy() {
		
	}

	@SuppressWarnings("serial")
	private static class CcpErrorPutSessionValuesFilterChain extends RuntimeException {
		private CcpErrorPutSessionValuesFilterChain(Throwable cause) {
			super(cause);
		}
	}
}
