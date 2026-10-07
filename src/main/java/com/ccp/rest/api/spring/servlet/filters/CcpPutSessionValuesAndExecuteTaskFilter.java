package com.ccp.rest.api.spring.servlet.filters;


import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.ccp.rest.api.spring.exceptions.handler.CcpRestApiExceptionHandlerSpring;
import com.ccp.rest.api.spring.servlet.request.CcpPutSessionValuesRequestWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

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
 * The task runs here, before the chain, for every request (with or without body, read by the controller or not); a
 * refusal of the task answers its own status and the request never reaches the controller.
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
			requestWrapper.prepareBody();
		} catch (Throwable e) {
			this.answerError(e, response);
			return;
		}

		try {
			chain.doFilter(requestWrapper, response);
		} catch (Exception e) {
			CcpErrorPutSessionValuesFilterChain ccpErrorPutSessionValuesFilterChain = new CcpErrorPutSessionValuesFilterChain(e);
			throw ccpErrorPutSessionValuesFilterChain;
		} 
	}

	/**
	 * Answers a failure of the body preparation (invalid JSON, or the task refusing the request) the way
	 * {@link CcpRestApiExceptionHandlerSpring} answers it inside the controllers: the advice does not reach a filter, and
	 * without this the failure would become a 500.
	 * @param e the failure
	 * @param response the response
	 * @throws CcpErrorPutSessionValuesFilterChain when the error body cannot be written
	 */
	private void answerError(Throwable e, HttpServletResponse response) {
		CcpRestApiExceptionHandlerSpring exceptionHandler = new CcpRestApiExceptionHandlerSpring();
		Map<String, Object> body;

		if(e instanceof CcpJsonValidationError validationError) {
			response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
			body = exceptionHandler.handle(validationError);
		} else if(e instanceof CcpErrorFlowDisturb flowDisturb) {
			try {
				body = exceptionHandler.handle(flowDisturb, response);
			} catch (IOException ioException) {
				CcpErrorPutSessionValuesFilterChain ccpErrorPutSessionValuesFilterChain = new CcpErrorPutSessionValuesFilterChain(ioException);
				throw ccpErrorPutSessionValuesFilterChain;
			}
		} else {
			exceptionHandler.handle(e, response);
			return;
		}

		try {
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			String bodyAsText = objectMapper.writeValueAsString(body);
			response.getWriter().write(bodyAsText);
		} catch (IOException ioException) {
			CcpErrorPutSessionValuesFilterChain ccpErrorPutSessionValuesFilterChain = new CcpErrorPutSessionValuesFilterChain(ioException);
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
