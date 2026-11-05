package com.ccp.rest.api.spring.servlet.filters;

import java.io.IOException;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.CcpDbCallsOfTheThread;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet filter that measures the calls to the database of each request ({@link CcpDbCallsOfTheThread}) and, when there
 * were calls, writes one JSON line on the standard output with the trace id, the method, the path, each call and the
 * totals. On Cloud Run the line becomes a {@code jsonPayload} of the log {@code run.googleapis.com/stdout}, and the trace
 * id is the one of the request log ({@code X-Cloud-Trace-Context}), so the local panel of the requests joins the request,
 * its calls and the slowlog of Elasticsearch by it. Outside Cloud Run (no trace header) a new trace id is made.
 */
public class CcpDbCallsLogFilter implements Filter {

	/** Headers, fields and texts of this filter. */
	enum Fields implements CcpJsonFieldName {
		/** What the line is: always {@link #jobsnowDbCalls}. */
		kind,
		/** The value of {@link #kind}. */
		jobsnowDbCalls,
		/** The HTTP method of the request. */
		httpMethod,
		/** The path of the request. */
		uri,
		/** The W3C trace header. */
		traceparent,
		/** The trace header of Google Cloud: {@code <trace id>/<span id>;o=<flag>}. */
		X_Cloud_Trace_Context { public String getValue() { return "X-Cloud-Trace-Context"; } },
		/** What ends the trace id in {@link #X_Cloud_Trace_Context}. */
		CLOUD_TRACE_END { public String getValue() { return "/"; } },
		/** The separator of the parts of {@link #traceparent}. */
		TRACEPARENT_SEPARATOR { public String getValue() { return "-"; } },
		/** The characters of a trace id. */
		TRACE_ID_PATTERN { public String getValue() { return "[0-9a-f]{32}"; } }
	}

	/** The single instance: the filter keeps no state. */
	public static final CcpDbCallsLogFilter INSTANCE = new CcpDbCallsLogFilter();

	private CcpDbCallsLogFilter() {}

	/**
	 * Starts the measurement, runs the request and writes the line of its calls, even when the request fails.
	 * @param req the request
	 * @param res the response
	 * @param chain the filter chain
	 * @throws IOException from the chain
	 * @throws ServletException from the chain
	 */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		String traceId = getTraceId(request);
		CcpDbCallsOfTheThread.start(traceId);
		try {
			chain.doFilter(req, res);
		} finally {
			CcpJsonRepresentation summary = CcpDbCallsOfTheThread.finish();
			writeLine(request, summary);
		}
	}

	/**
	 * Writes the line of the calls, when there were calls to the database or commands to the cache.
	 * @param request the request
	 * @param summary the calls and their totals
	 */
	private static void writeLine(HttpServletRequest request, CcpJsonRepresentation summary) {
		Integer dbCallsCount = summary.getAsIntegerNumber(CcpDbCallsOfTheThread.Fields.dbCallsCount);
		Integer cacheCallsCount = summary.getAsIntegerNumber(CcpDbCallsOfTheThread.Fields.cacheCallsCount);
		int callsCount = dbCallsCount + cacheCallsCount;
		boolean noCalls = callsCount == 0;
		if(noCalls) {
			return;
		}
		String kindValue = Fields.jobsnowDbCalls.getValue();
		String method = request.getMethod();
		String uri = request.getRequestURI();
		CcpJsonRepresentation line = summary
				.put(Fields.kind, kindValue)
				.put(Fields.httpMethod, method)
				.put(Fields.uri, uri);
		String lineText = line.asUgglyJson();
		System.out.println(lineText);
	}

	/**
	 * The trace id of the request: from {@code X-Cloud-Trace-Context}, else from {@code traceparent}, else a new one.
	 * @param request the request
	 * @return 32 hexadecimal characters
	 */
	private static String getTraceId(HttpServletRequest request) {
		String cloudTraceHeader = Fields.X_Cloud_Trace_Context.getValue();
		String cloudTrace = String.valueOf(request.getHeader(cloudTraceHeader));
		String cloudTraceEnd = Fields.CLOUD_TRACE_END.getValue();
		String[] cloudTraceParts = cloudTrace.split(cloudTraceEnd);
		String cloudTraceId = cloudTraceParts[0];
		boolean validCloudTrace = isTraceId(cloudTraceId);
		if(validCloudTrace) {
			return cloudTraceId;
		}
		String traceparentHeader = Fields.traceparent.getValue();
		String traceparent = String.valueOf(request.getHeader(traceparentHeader));
		String traceparentSeparator = Fields.TRACEPARENT_SEPARATOR.getValue();
		String[] traceparentParts = traceparent.split(traceparentSeparator);
		boolean withTraceIdPart = traceparentParts.length > 1;
		String traceparentId = withTraceIdPart ? traceparentParts[1] : traceparent;
		boolean validTraceparent = isTraceId(traceparentId);
		if(validTraceparent) {
			return traceparentId;
		}
		String newTraceId = CcpDbCallsOfTheThread.newTraceId();
		return newTraceId;
	}

	/**
	 * Whether the text is a trace id (32 lowercase hexadecimal characters).
	 * @param text the text
	 * @return true when it is
	 */
	private static boolean isTraceId(String text) {
		String pattern = Fields.TRACE_ID_PATTERN.getValue();
		boolean traceId = text.matches(pattern);
		return traceId;
	}

	/**
	 * Nothing to initialize.
	 * @param filterConfig the filter configuration
	 */
	public void init(FilterConfig filterConfig) {
	}

	/** Nothing to release. */
	public void destroy() {
	}
}
