package com.ccp.rest.api.spring.servlet.request;
 
import java.io.IOException;
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
		/**
		 * Taken from the {@code Host} header (so it is the host requested, not the client address), {@code 127.0.0.1} for
		 * localhost.
		 */
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
	 * A missing or blank body becomes an empty JSON (GET, DELETE and the POSTs that only use the e-mail from the URL send no
	 * body). A body that is present but is not JSON gets a 400. Until 2026-09-27 any read error was swallowed and the body
	 * became an empty JSON: malformed JSON went through silently, and where the body did not matter the operation happened
	 * anyway ({@code POST /token} with garbage created a token).
	 * <p>
	 * A body of another media type is returned as it is (Spring answers 415). A non-empty JSON body gets the session values
	 * and goes through the task; an empty one gets only the session values (and the e-mail as an object, see
	 * {@code getEmptyJsonInputStream}), without running the task.
	 * @return the body stream
	 * @throws IOException when the original body cannot be read
	 * @throws CcpErrorFlowDisturb with status 400 when the JSON body is invalid
	 */
	@SuppressWarnings("unchecked")
	public ServletInputStream getInputStream() throws IOException {
		ServletRequest request = super.getRequest();
		byte[] body = request.getInputStream().readAllBytes();
		String bodyAsText = new String(body, StandardCharsets.UTF_8);

		String contentType = request.getContentType();
		boolean isNotJson = contentType != null && false == contentType.toLowerCase().contains("json");

		if(isNotJson) {
			// another media type: not this wrapper's business; Spring answers 415 when it sees the Content-Type
			CcpRawServletInputStream inputStream = new CcpRawServletInputStream(body);
			return inputStream;
		}

		boolean bodyNotReceived = bodyAsText.trim().isEmpty();

		if(bodyNotReceived) {
			CcpJsonServletInputStream inputStream = this.getEmptyJsonInputStream();
			return inputStream;
		}

		Map<String, Object> originalJson;
		try {
			originalJson = new ObjectMapper().readValue(body, Map.class);
		} catch (IOException e) {
			CcpJsonRepresentation details = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.body, bodyAsText);
			String message = "The request body is not a valid json";
			throw new CcpErrorFlowDisturb(details, CcpProcessStatusDefault.BAD_REQUEST, message, new CcpJsonFieldName[0]);
		}

		boolean jsonNotReceived = originalJson == null || originalJson.isEmpty();

		if(jsonNotReceived) {
			CcpJsonServletInputStream inputStream = this.getEmptyJsonInputStream();
			return inputStream;
		}

		CcpJsonRepresentation sessionValues = this.getSessionValues(originalJson);
		CcpJsonRepresentation transformedJson = sessionValues.getTransformedJson(this.task);
		CcpJsonServletInputStream inputStream = new CcpJsonServletInputStream(transformedJson);
		return inputStream;
	}

	/**
	 * Builds the body of a request without JSON: the session values plus {@code email}, stored as the
	 * {@code CcpEmailDecorator} object instead of its text.
	 * @return the body stream
	 */
	private CcpJsonServletInputStream getEmptyJsonInputStream() {
		StringBuffer requestURL = this.request.getRequestURL();
		String requestUrlText = requestURL.toString();
		CcpStringDecorator requestUrlDecorator = new CcpStringDecorator(requestUrlText);
		CcpEmailDecorator urlEmails = requestUrlDecorator.email();
		CcpEmailDecorator email = urlEmails.findFirst("/");
		CcpJsonRepresentation sessionValues = this.getSessionValues(CcpOtherConstants.EMPTY_JSON.content);
		CcpJsonRepresentation jsonWithEmail = sessionValues.put(JsonFieldNames.email, email);
		CcpJsonServletInputStream inputStream = new CcpJsonServletInputStream(jsonWithEmail);
		return inputStream;
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
	 * Returns the host of the {@code Host} header, without port, lowercase; {@code 127.0.0.1} for localhost.
	 * @return the host
	 */
	private String getIp() {
		String host = this.request.getHeader("Host");
		String[] split = host.split(":");
		String ipWithoutPortNumber = split[0].toLowerCase();
		boolean isLocalhost = "localhost".equalsIgnoreCase(ipWithoutPortNumber);
		
		if(isLocalhost) {
			return "127.0.0.1";
		}
		return ipWithoutPortNumber;
	}
}
