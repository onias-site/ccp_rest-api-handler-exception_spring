package com.ccp.rest.api.spring.servlet.request;
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;


import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * {@code HttpServletRequest} wrapper that enriches the JSON body with session values
 * (email, IP, sessionToken, userAgent, language extracted from the URL/headers) and applies an
 * optional transforming {@code CcpBusiness} before exposing the modified InputStream.
 */
public class CcpPutSessionValuesRequestWrapper extends HttpServletRequestWrapper implements CcpJsonExtractorFromHttpServletRequest{
	/** Session values added to the body. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code User-Agent} header. */
		userAgent,
		/** The client address ({@code getRemoteAddr()}), {@code 127.0.0.1} for the loopback. */
		ip,
		/** The path segment after {@code language/}, when present. */
		language,
		/** The first valid e-mail found in the URL path. */
		email,
		/** The invalid body, in the 400 error. */
		body
	}
	
	/** Business run over the enriched body. */
	private final CcpBusiness task;

	/** The wrapped request. */
	private final HttpServletRequest request;

	/** The body as the controller sees it, built by {@link #prepareBody()}; {@code null} until then. */
	private CcpJsonRepresentation preparedJson;

	/** The original body, kept as it is when it is not JSON. */
	private byte[] rawBody;

	/**
	 * Wraps the request.
	 * @param request the request
	 * @param task the business run over the enriched body
	 */
	public CcpPutSessionValuesRequestWrapper(HttpServletRequest request,CcpBusiness task) {
		super(request);
		this.request = request;
		this.task = task;
	}

	/**
	 * Reads the body once and runs the task, for <b>every</b> request: the filter calls it before the chain, so the task
	 * (e.g. the session validation) never depends on the controller reading the body. Until 2026-10-06 the task ran inside
	 * {@link #getInputStream()} and only for a non-empty JSON body: a request without body, with an empty JSON, with another
	 * media type, or to an endpoint that does not read the body skipped the session validation.
	 * <p>
	 * A missing or blank body becomes an empty JSON (GET, DELETE and the POSTs that only use the e-mail from the URL send no
	 * body). A body that is present but is not JSON gets a 400. Until 2026-09-27 any read error was swallowed and the body
	 * became an empty JSON: malformed JSON went through silently, and where the body did not matter the operation happened
	 * anyway ({@code POST /token} with garbage created a token).
	 * <p>
	 * A body of another media type is kept as it is (Spring answers 415 where JSON is expected); the task still runs over
	 * the session values alone. A JSON body gets the session values and goes through the task.
	 * @return this wrapper
	 * @throws IOException when the original body cannot be read
	 * @throws CcpErrorFlowDisturb with status 400 when the JSON body is invalid, or whatever the task throws
	 */
	@SuppressWarnings("unchecked")
	public CcpPutSessionValuesRequestWrapper prepareBody() throws IOException {
		ServletRequest request = super.getRequest();
		byte[] body = request.getInputStream().readAllBytes();
		String bodyAsText = new String(body, StandardCharsets.UTF_8);

		String contentType = request.getContentType();
		boolean isNotJson = contentType != null && false == contentType.toLowerCase().contains("json");

		if(isNotJson) {
			CcpJsonRepresentation sessionValues = this.getSessionValues();
			sessionValues.getTransformedJson(this.task);
			this.rawBody = body;
			return this;
		}

		boolean bodyReceived = false == bodyAsText.trim().isEmpty();

		Map<String, Object> originalJson = CcpOtherConstants.EMPTY_JSON.content;

		if(bodyReceived) {
			try {
				originalJson = new ObjectMapper().readValue(body, Map.class);
			} catch (IOException e) {
				CcpJsonRepresentation details = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.body, bodyAsText);
				String message = "The request body is not a valid json";
				throw new CcpErrorFlowDisturb(details, CcpProcessStatusDefault.BAD_REQUEST, message, new CcpJsonFieldName[0]);
			}
		}

		boolean jsonNotReceived = originalJson == null;

		if(jsonNotReceived) {
			originalJson = CcpOtherConstants.EMPTY_JSON.content;
		}

		CcpJsonRepresentation sessionValues = this.getSessionValues(originalJson);
		this.preparedJson = sessionValues.getTransformedJson(this.task);
		return this;
	}

	/**
	 * The body prepared by {@link #prepareBody()} (which runs here when the filter did not call it): the original bytes when
	 * the body is not JSON, otherwise the JSON with the session values, after the task. Each call gives a new stream.
	 * @return the body stream
	 * @throws IOException when the original body cannot be read
	 */
	public ServletInputStream getInputStream() throws IOException {
		boolean notPreparedYet = this.preparedJson == null && this.rawBody == null;

		if(notPreparedYet) {
			this.prepareBody();
		}

		boolean keptAsItIs = this.rawBody != null;

		if(keptAsItIs) {
			CcpRawServletInputStream inputStream = new CcpRawServletInputStream(this.rawBody);
			return inputStream;
		}

		CcpJsonServletInputStream inputStream = new CcpJsonServletInputStream(this.preparedJson);
		return inputStream;
	}

	/**
	 * Reads the prepared body as text, so a reader never bypasses the session values and the task.
	 * @return the body reader
	 * @throws IOException when the original body cannot be read
	 */
	public BufferedReader getReader() throws IOException {
		ServletInputStream inputStream = this.getInputStream();
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		return bufferedReader;
	}


	/**
	 * Returns the session values of the request, over an empty JSON.
	 * @return the session values
	 */
	protected CcpJsonRepresentation getSessionValues() {
		CcpJsonRepresentation sessionValues = this.getSessionValues(CcpOtherConstants.EMPTY_JSON.content);
		return sessionValues;
	}
	
	/**
	 * Adds to the body: {@code sessionToken} (header, empty when absent), {@code userAgent}, {@code email} (first valid
	 * e-mail of the URL path), {@code ip} and, when the URL has {@code language/<code>}, {@code language}.
	 * @param originalJson the body
	 * @return the enriched body
	 */
	private CcpJsonRepresentation getSessionValues(Map<String, Object> originalJson) {

		String ip = this.getIp();
		String sessionToken = this.request.getHeader("sessionToken");
		boolean sessionTokenIsMissing = sessionToken == null;
		if(sessionTokenIsMissing) {
			sessionToken = "";
		}
		String userAgent = this.request.getHeader("User-Agent");
		
		StringBuffer requestURL = this.request.getRequestURL();
		String uri = requestURL.toString();
		CcpStringDecorator uriDecorator = new CcpStringDecorator(uri);
		CcpEmailDecorator uriEmails = uriDecorator.email();
		CcpEmailDecorator email = uriEmails.findFirst("/");
		CcpJsonRepresentation requestJson = new CcpJsonRepresentation(originalJson);
		CcpJsonRepresentation jsonWithSessionToken = requestJson.put(CcpJsonCommonsFields.sessionToken, sessionToken);
		CcpJsonRepresentation jsonWithUserAgent = jsonWithSessionToken
				.put(JsonFieldNames.userAgent, userAgent);
				CcpJsonRepresentation jsonWithEmail = jsonWithUserAgent.put(JsonFieldNames.email, email.content);
				CcpJsonRepresentation jsonWithSessionValues = jsonWithEmail.put(JsonFieldNames.ip, ip);
	
		String languagePathPrefix = "language/";
		int languageIndex = uri.indexOf(languagePathPrefix);
		
		boolean hasNotLanguage = languageIndex < 0;
		
		if(hasNotLanguage) {
			return jsonWithSessionValues;
		}
		int languagePathPrefixLength = languagePathPrefix.length();
		int languageStart = languageIndex + languagePathPrefixLength;

		String pathFromLanguage = uri.substring(languageStart);
		String[] split = pathFromLanguage.split("/");
		String language = split[0];
		
		CcpJsonRepresentation jsonWithSessionValuesAndLanguage = jsonWithSessionValues.put(JsonFieldNames.language, language);
		
		return jsonWithSessionValuesAndLanguage;
	}

	/**
	 * Returns the address of the client ({@code getRemoteAddr()}); {@code 127.0.0.1} for the loopback, also in IPv6, and
	 * empty when the container does not know it. Behind a proxy, the client address comes from the container configuration
	 * ({@code server.forward-headers-strategy}), never from a header read here, which the client could forge. Until
	 * 2026-10-07 it came from the {@code Host} header: it was the host requested (the same for every user), and a request
	 * without that header raised a NullPointerException that became a 500 recorded as a system error.
	 * @return the client address
	 */
	private String getIp() {
		String remoteAddress = this.request.getRemoteAddr();
		boolean remoteAddressIsMissing = remoteAddress == null;

		if(remoteAddressIsMissing) {
			return "";
		}
		boolean isIpv6Loopback = "0:0:0:0:0:0:0:1".equals(remoteAddress) || "::1".equals(remoteAddress);

		if(isIpv6Loopback) {
			return "127.0.0.1";
		}
		return remoteAddress;
	}
}
