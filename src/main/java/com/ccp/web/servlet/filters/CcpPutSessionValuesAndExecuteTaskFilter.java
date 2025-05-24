package com.ccp.web.servlet.filters;

import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.web.servlet.request.CcpPutSessionValuesRequestWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CcpPutSessionValuesAndExecuteTaskFilter implements Filter{
	
	public static final CcpPutSessionValuesAndExecuteTaskFilter TASKLESS = new  CcpPutSessionValuesAndExecuteTaskFilter(CcpOtherConstants.DO_NOTHING);
	
	private final Function<CcpJsonRepresentation, CcpJsonRepresentation> task;
	
	public CcpPutSessionValuesAndExecuteTaskFilter(Function<CcpJsonRepresentation, CcpJsonRepresentation> task) {
		this.task = task;
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain){

		HttpServletRequest request = (HttpServletRequest) req;

		HttpServletResponse response = (HttpServletResponse) res;

		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, HEAD, PATCH");
		response.setHeader("Access-Control-Max-Age", "3600");

		response.setHeader("Access-Control-Allow-Headers",
				"Access-Control-Allow-Headers, X-Requested-With, authorization, token, email, Content-Type, Authorization, Access-Control-Request-Methods, Access-Control-Request-Headers");

		String method = request.getMethod();

		boolean optionsMethod = "OPTIONS".equalsIgnoreCase(method);

		if (optionsMethod) {
			return;
		}

		try {
			CcpPutSessionValuesRequestWrapper wraper = new CcpPutSessionValuesRequestWrapper(request, this.task);
			chain.doFilter(wraper, response);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}


	public void init(FilterConfig filterConfig) throws ServletException {
		
	}
	
	public void destroy() {
		
	}
}
