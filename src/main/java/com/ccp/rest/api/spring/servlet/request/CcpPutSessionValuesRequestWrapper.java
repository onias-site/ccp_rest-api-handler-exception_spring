package com.ccp.rest.api.spring.servlet.request;
 
import java.io.IOException;
import java.util.Map;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.business.CcpBusiness;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;


public class CcpPutSessionValuesRequestWrapper extends HttpServletRequestWrapper implements CcpJsonExtractorFromHttpServletRequest{
	enum JsonFieldNames implements CcpJsonFieldName{
		userAgent, sessionToken, ip, language, email
	}
	
	private final CcpBusiness task;
	
	private final HttpServletRequest request;
	
	public CcpPutSessionValuesRequestWrapper(HttpServletRequest request,CcpBusiness task) {
		super(request);
		this.request = request;
		this.task = task;
	}

	public ServletInputStream getInputStream() throws IOException {
		try {
			ServletRequest request = super.getRequest();
			Map<String, Object> originalJson = this.extractJsonFromHttpServletRequest(request);
			CcpJsonRepresentation sessionValues = this.getSessionValues(originalJson);
			CcpJsonRepresentation transformedJson = sessionValues.getTransformedJson(this.task);
			CcpJsonServletInputStream is = new CcpJsonServletInputStream(transformedJson);
			return is;
		} catch (IOException e) {
			StringBuffer requestURL = this.request.getRequestURL();
			CcpEmailDecorator email = new CcpStringDecorator(requestURL.toString()).email().findFirst("/");
			CcpJsonRepresentation sessionValues = this.getSessionValues(CcpOtherConstants.EMPTY_JSON.content);
			CcpJsonRepresentation put = sessionValues.put(JsonFieldNames.email, email);
			CcpJsonServletInputStream is = new CcpJsonServletInputStream(put);
			return is;
		}
	}


	protected CcpJsonRepresentation getSessionValues() {
		CcpJsonRepresentation sessionValues = this.getSessionValues(CcpOtherConstants.EMPTY_JSON.content);
		return sessionValues;
	}
	
	private CcpJsonRepresentation getSessionValues(Map<String, Object> originalJson) {

		String ip = this.getIp();
		String sessionToken = this.request.getHeader("sessionToken");
		String userAgent = this.request.getHeader("User-Agent");
		
		StringBuffer requestURL = this.request.getRequestURL();
		String uri = requestURL.toString();
		CcpEmailDecorator email = new CcpStringDecorator(uri).email().findFirst("/");
		CcpJsonRepresentation md = new CcpJsonRepresentation(originalJson);
		CcpJsonRepresentation jsonWithSessionValues = md.put(JsonFieldNames.sessionToken, sessionToken)
				.put(JsonFieldNames.userAgent, userAgent).put(JsonFieldNames.email, email.content).put(JsonFieldNames.ip, ip);
	
		String str = "language/";
		int languageIndex = uri.indexOf(str);
		
		boolean hasNotLanguage = languageIndex < 0;
		
		if(hasNotLanguage) {
			return jsonWithSessionValues;
		}
		
		String substring = uri.substring(languageIndex + str.length());
		String[] split = substring.split("/");
		String language = split[0];
		
		CcpJsonRepresentation jsonWithSessionValuesAndLanguage = jsonWithSessionValues.put(JsonFieldNames.language, language);
		
		return jsonWithSessionValuesAndLanguage;
	}

	private String getIp() {
		String host = this.request.getHeader("Host");
		String[] split = host.split(":");
		String ipWithoutPortNumber = split[0].toLowerCase();
		boolean equalsIgnoreCase = "localhost".equalsIgnoreCase(ipWithoutPortNumber);
		
		if(equalsIgnoreCase) {
			return "127.0.0.1";
		}
		return ipWithoutPortNumber;
	}
}
