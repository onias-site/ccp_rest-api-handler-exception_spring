package com.ccp.rest.api.spring.servlet.filters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import com.ccp.especifications.http.CcpHttpMethods;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.ccp.rest.api.spring.servlet.request.CcpJsonExtractorFromHttpServletRequest;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CcpValidJsonFilter implements Filter, CcpJsonExtractorFromHttpServletRequest{

	private final CcpHttpMethods[] allowedMethods;
	
	
	public CcpValidJsonFilter(CcpHttpMethods... allowedMethods) {
		this.allowedMethods = allowedMethods;
	}




	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;

		HttpServletResponse response = (HttpServletResponse) res;

		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, HEAD, PATCH");
		response.setHeader("Access-Control-Max-Age", "3600");

		response.setHeader("Access-Control-Allow-Headers",
				"Access-Control-Allow-Headers, X-Requested-With, authorization, token, email, Content-Type, Authorization, Access-Control-Request-Methods, Access-Control-Request-Headers");

		String method = request.getMethod();
		
		boolean isNotAllowedMethod = Arrays.asList(this.allowedMethods).stream().filter(x -> method.equals(x.name())).findFirst().isPresent() == false;
		if (isNotAllowedMethod) {
			return;
		}

		try {
			Map<String, Object> json = this.extractJsonFromHttpServletRequest(request);
//			CcpJsonValidatorEngine.INSTANCE.validateJson(jsonValidationClass, json, request.getRequestURI());
			chain.doFilter(request, response);
		} catch (CcpJsonValidationError e) {
			response.setStatus(422);
		}
	}

}
