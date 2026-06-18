package com.ccp.rest.api.spring.exceptions.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpPropertiesDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.hash.CcpHashAlgorithm;
import com.ccp.json.validations.global.engine.CcpJsonValidatorEngine.CcpJsonValidationError;


import jakarta.servlet.http.HttpServletResponse;


/**
 * Handler global de exceções Spring Boot. Trata {@code CcpJsonValidationError} (422),
 * {@code CcpErrorFlowDisturb} (status dinâmico) e qualquer {@code Throwable} genérico (500),
 * filtrando o stack trace para conter apenas linhas do domínio e calculando um hash SHA1
 * para rastreabilidade.
 */
@RestControllerAdvice
public class CcpRestApiExceptionHandlerSpring {
	enum JsonFieldNames implements CcpJsonFieldName{
		message, stackTrace, cause, systems, application_properties, stackTraceHash, status
	}

	public static CcpBusiness genericExceptionHandler;
 
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	@ExceptionHandler({ CcpJsonValidationError.class })
	public Map<String, Object> handle(CcpJsonValidationError e) {
		return e.json.content;
	}

	@ResponseBody
	@ExceptionHandler({ CcpErrorFlowDisturb.class })
	public Map<String, Object> handle(CcpErrorFlowDisturb e, HttpServletResponse res) throws IOException{
		
		res.setStatus(e.status.asNumber());
		String message = e.getMessage();
		
		CcpJsonRepresentation result = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.message, message);
		
		boolean noFields = e.fields.length <= 0;
		
		if(noFields) {
			return result.put(JsonFieldNames.status, e.status.name()).content;
		}

		CcpJsonRepresentation subMap = e.json.getJsonPiece(e.fields);

		CcpJsonRepresentation putAll = result.mergeWithAnotherJson(subMap);

		return putAll.put(JsonFieldNames.status, e.status.name()).content;
	}

	@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler({ Throwable.class })
	public void handle(Throwable e) {
		if(genericExceptionHandler == null) {
			throw new CcpErrorExceptionHandlerIsMissing(e);
		}
		CcpJsonRepresentation put = getHandledExceptionToLog(e);
		
		genericExceptionHandler.apply(put);
	}

	public static CcpJsonRepresentation getHandledExceptionToLog(Throwable e) {
		
		CcpJsonRepresentation json = new CcpJsonRepresentation(e);
		
		CcpJsonRepresentation put = getHandledExceptionToLog(json);
		return put;
	}
 
	private static boolean doesNotBelongToDomain(String stack, List<String> systems) {
		
		for (String system : systems) {
			if(stack.contains(system)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static CcpJsonRepresentation getHandledExceptionToLog(CcpJsonRepresentation json) {
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator(JsonFieldNames.application_properties.name());
		CcpPropertiesDecorator propertiesFrom = ccpStringDecorator.propertiesFrom();
		CcpJsonRepresentation systemProperties = propertiesFrom.environmentVariablesOrClassLoaderOrFile();
		boolean hasNoCause = false == json.getAsStringDecorator(CcpJsonRepresentation.Fields.cause).isList();
		
		if(hasNoCause) {
			json = json.put(CcpJsonRepresentation.Fields.cause, new ArrayList<>());
		}
		
		CcpJsonRepresentation jsonWithStackTrace = getHandledExceptionToLog(json, systemProperties, CcpJsonRepresentation.Fields.completeStackTrace);
		return jsonWithStackTrace;
	}

	private static CcpJsonRepresentation getHandledExceptionToLog(CcpJsonRepresentation json, CcpJsonRepresentation systemProperties, CcpJsonFieldName field) {
		List<String> stackTrace = json.getAsStringList(field);
		List<String> newStackTrace = new ArrayList<>();
		List<String> systems = systemProperties.getAsStringList(JsonFieldNames.systems);
		int endIndex = stackTrace.size();
		int startIndex = -1;
		int index = 0;
		
		for (String stack : stackTrace) {
			
			boolean doesNotBelongToDomain = doesNotBelongToDomain(stack, systems);
		
			if(doesNotBelongToDomain) {

				boolean settingEndIndex = startIndex > -1;
				
				if(settingEndIndex) {
					endIndex = index++;
					break;
				}
				continue;
			}
			
			boolean settingStartIndex = startIndex < 0;
			
			if(settingStartIndex) {
				startIndex = index;
			}

			index++;
		}
		
		boolean settingEndIndex2 = startIndex > -1;
		boolean found = settingEndIndex2;
		
		if(found) {
			
			if(endIndex <  stackTrace.size()) {
				endIndex++;
			}
			
			newStackTrace = stackTrace.subList(startIndex, endIndex);
		}
		String stackTraceHash = new CcpStringDecorator(newStackTrace.toString()).hash().asString(CcpHashAlgorithm.SHA1); 
		CcpJsonRepresentation put = json.put(JsonFieldNames.stackTraceHash, stackTraceHash).put(CcpJsonRepresentation.Fields.stackTrace, newStackTrace);
		
		return put;
	}
	
	@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
	@ExceptionHandler({ org.springframework.web.HttpRequestMethodNotSupportedException.class })
	public void methodNoSupported() {

	}

	@SuppressWarnings("serial")
	public static class CcpErrorExceptionHandlerIsMissing extends RuntimeException {
		private CcpErrorExceptionHandlerIsMissing(Throwable e) {
			super("genericExceptionHandler must has an instance ", e);
		}
	}
}
