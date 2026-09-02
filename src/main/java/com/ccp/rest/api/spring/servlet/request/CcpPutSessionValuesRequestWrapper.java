package com.ccp.rest.api.spring.servlet.request;
 
import java.io.IOException;
import java.util.Map;

import org.aspectj.lang.SoftException;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;


/**
 * Wrapper de {@code HttpServletRequest} que enriquece o corpo JSON com valores de sessão
 * (email, IP, sessionToken, userAgent, language extraídos da URL/headers) e aplica uma
 * {@code CcpBusiness} opcional de transformação antes de expor o InputStream modificado.
 */
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
			boolean jsonNotReceived = originalJson.isEmpty();
			
			if(jsonNotReceived) {
				CcpJsonServletInputStream is = this.getEmptyJsonInputStream();
				return is;
			}
			
			CcpJsonRepresentation sessionValues = this.getSessionValues(originalJson);
			CcpJsonRepresentation transformedJson = sessionValues.getTransformedJson(this.task);
			CcpJsonServletInputStream is = new CcpJsonServletInputStream(transformedJson);
			return is;
		} catch (SoftException | IOException e) {
			CcpJsonServletInputStream is = this.getEmptyJsonInputStream();
			return is;
		}
	}

	private CcpJsonServletInputStream getEmptyJsonInputStream() {
		StringBuffer requestURL = this.request.getRequestURL();
		String toString = requestURL.toString();
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator(toString);
		CcpEmailDecorator email2 = ccpStringDecorator.email();
		CcpEmailDecorator email = email2.findFirst("/");
		CcpJsonRepresentation sessionValues = this.getSessionValues(CcpOtherConstants.EMPTY_JSON.content);
		CcpJsonRepresentation put = sessionValues.put(JsonFieldNames.email, email);
		CcpJsonServletInputStream is = new CcpJsonServletInputStream(put);
		return is;
	}


	protected CcpJsonRepresentation getSessionValues() {
		CcpJsonRepresentation sessionValues = this.getSessionValues(CcpOtherConstants.EMPTY_JSON.content);
		return sessionValues;
	}
	
	private CcpJsonRepresentation getSessionValues(Map<String, Object> originalJson) {

		String ip = this.getIp();
		String sessionToken = this.request.getHeader("sessionToken");
		boolean sessionTokenIgual = sessionToken == null;
		if(sessionTokenIgual) {
			sessionToken = "";
		}
		String userAgent = this.request.getHeader("User-Agent");
		
		StringBuffer requestURL = this.request.getRequestURL();
		String uri = requestURL.toString();
		CcpStringDecorator ccpStringDecorator2 = new CcpStringDecorator(uri);
		CcpEmailDecorator email3 = ccpStringDecorator2.email();
		CcpEmailDecorator email = email3.findFirst("/");
		CcpJsonRepresentation md = new CcpJsonRepresentation(originalJson);
		CcpJsonRepresentation put2 = md.put(JsonFieldNames.sessionToken, sessionToken);
		CcpJsonRepresentation put3 = put2
				.put(JsonFieldNames.userAgent, userAgent);
				CcpJsonRepresentation put4 = put3.put(JsonFieldNames.email, email.content);
				CcpJsonRepresentation jsonWithSessionValues = put4.put(JsonFieldNames.ip, ip);
	
		String str = "language/";
		int languageIndex = uri.indexOf(str);
		
		boolean hasNotLanguage = languageIndex < 0;
		
		if(hasNotLanguage) {
			return jsonWithSessionValues;
		}
		int strLength = str.length();
		int languageIndexMais = languageIndex + strLength;

		String substring = uri.substring(languageIndexMais);
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
