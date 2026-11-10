package com.ccp.rest.api.spring.servlet.filters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The only place of the CORS of the APIs, and the refusal of the methods that echo the request. Until 2026-11-10 every
 * controller was {@code @CrossOrigin} (any origin) and two filters wrote {@code Access-Control-Allow-Origin: *} by hand:
 * any site could call the APIs from the browser of a visitor. Now only the origins of {@code ccp.cors.allowed-origins}
 * (the {@code application.properties} of each API: the front end in production and on the developer's machine) receive
 * the CORS headers; a preflight of another origin answers 403. Every answer carries {@code Vary: Origin}, so the CDN
 * of the public routes does not hand the headers of one origin to another. {@code TRACE} and {@code TRACK} answer 405:
 * Jetty echoed the request (with its headers) under 200. Registered by each API right after the filter of the
 * measurements, for every path.
 */
public class CcpCorsAndMethodsFilter implements Filter {

	/** The texts of the filter. */
	enum Fields implements CcpJsonFieldName {
		Origin, Vary, OPTIONS, TRACE, TRACK,
		Access_Control_Request_Method {
			public String getValue() {
				return "Access-Control-Request-Method";
			}
		},
		ALLOWED_METHODS {
			public String getValue() {
				return "POST, GET, OPTIONS, DELETE, HEAD";
			}
		},
		ALLOWED_HEADERS {
			public String getValue() {
				return "X-Requested-With, Sessiontoken, Email, Content-Type, Authorization";
			}
		},
		MAX_AGE {
			public String getValue() {
				return "3600";
			}
		},
		COMMA {
			public String getValue() {
				return ",";
			}
		},
	}

	/** The origins that may call the API from a browser. */
	private final List<String> allowedOrigins;

	/**
	 * Reads the allowed origins.
	 * @param allowedOrigins the origins separated by commas ({@code ccp.cors.allowed-origins}); empty allows none
	 */
	public CcpCorsAndMethodsFilter(String allowedOrigins) {
		String separator = Fields.COMMA.getValue();
		String[] origins = allowedOrigins.split(separator);
		this.allowedOrigins = Arrays.stream(origins).map(String::trim).filter(origin -> false == origin.isEmpty()).toList();
	}

	/**
	 * Refuses the echo methods, answers the preflights and puts the CORS headers of an allowed origin.
	 * @param req the request
	 * @param res the response
	 * @param chain the filter chain
	 * @throws IOException from the chain
	 * @throws ServletException from the chain
	 */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String method = request.getMethod();
		String traceMethod = Fields.TRACE.name();
		String trackMethod = Fields.TRACK.name();
		boolean echoMethod = traceMethod.equalsIgnoreCase(method) || trackMethod.equalsIgnoreCase(method);

		if(echoMethod) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}

		String originHeader = Fields.Origin.name();
		String varyHeader = Fields.Vary.name();
		response.addHeader(varyHeader, originHeader);
		String origin = request.getHeader(originHeader);
		boolean allowedOrigin = origin != null && this.allowedOrigins.contains(origin);
		String preflightHeader = Fields.Access_Control_Request_Method.getValue();
		String optionsMethod = Fields.OPTIONS.name();
		boolean preflight = optionsMethod.equalsIgnoreCase(method) && request.getHeader(preflightHeader) != null;

		if(preflight && false == allowedOrigin) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		if(allowedOrigin) {
			this.putTheCorsHeaders(response, origin);
		}

		if(preflight) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		chain.doFilter(request, response);
	}

	private void putTheCorsHeaders(HttpServletResponse response, String origin) {
		String allowOrigin = CcpCorsSpecialWords.Access_Control_Allow_Origin.getValue();
		String allowMethods = CcpCorsSpecialWords.Access_Control_Allow_Methods.getValue();
		String allowHeaders = CcpCorsSpecialWords.Access_Control_Allow_Headers.getValue();
		String maxAge = CcpCorsSpecialWords.Access_Control_Max_Age.getValue();
		String methods = Fields.ALLOWED_METHODS.getValue();
		String headers = Fields.ALLOWED_HEADERS.getValue();
		String seconds = Fields.MAX_AGE.getValue();
		response.setHeader(allowOrigin, origin);
		response.setHeader(allowMethods, methods);
		response.setHeader(allowHeaders, headers);
		response.setHeader(maxAge, seconds);
	}
}
