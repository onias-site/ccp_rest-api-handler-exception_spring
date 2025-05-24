package com.ccp.web.servlet.request;
 
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.constantes.CcpStringConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CcpPutSessionValuesRequestWrapper extends HttpServletRequestWrapper implements CcpJsonExtractorFromHttpServletRequest{
	
	private final Function<CcpJsonRepresentation, CcpJsonRepresentation> task;
	
	private final HttpServletRequest request;
	
	public CcpPutSessionValuesRequestWrapper(HttpServletRequest request,Function<CcpJsonRepresentation, CcpJsonRepresentation> task) {
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
			CcpJsonRepresentation put = sessionValues.put(CcpStringConstants.EMAIL.value, email);
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
		CcpJsonRepresentation jsonWithSessionValues = md.put("sessionToken", sessionToken)
				.put("userAgent", userAgent).put(CcpStringConstants.EMAIL.value, email.content).put("ip", ip);
	
		String str = "language/";
		int languageIndex = uri.indexOf(str);
		
		boolean hasNotLanguage = languageIndex < 0;
		
		if(hasNotLanguage) {
			return jsonWithSessionValues;
		}
		
		String substring = uri.substring(languageIndex + str.length());
		String[] split = substring.split("/");
		String language = split[0];
		
		CcpJsonRepresentation jsonWithSessionValuesAndLanguage = jsonWithSessionValues.put(CcpStringConstants.LANGUAGE.value, language);
		
		return jsonWithSessionValuesAndLanguage;
	}

	private String getIp() {
		String host = this.request.getHeader("Host");
		String[] split = host.split(":");
		String ipWithoutPortNumber = split[0].toLowerCase();
		return ipWithoutPortNumber;
	}
}
