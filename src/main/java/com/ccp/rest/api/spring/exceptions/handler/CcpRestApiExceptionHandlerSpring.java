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
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpPropertiesDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.hash.CcpHashAlgorithm;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;


import jakarta.servlet.http.HttpServletResponse;
import com.ccp.decorators.CcpHashDecorator;


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
		int asNumber = e.status.asNumber();
	
		res.setStatus(asNumber);
		String message = e.getMessage();
		
		CcpJsonRepresentation result = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.message, message);
		
		boolean noFields = e.fields.length <= 0;
		
		if(noFields) {
			String statusName = e.status.name();
			CcpJsonRepresentation put2 = result.put(JsonFieldNames.status, statusName);
			return put2.content;
		}

		CcpJsonRepresentation subMap = e.json.getJsonPiece(e.fields);

		CcpJsonRepresentation putAll = result.mergeWithAnotherJson(subMap);
		String statusName2 = e.status.name();
		CcpJsonRepresentation put3 = putAll.put(JsonFieldNames.status, statusName2);

		return put3.content;
	}

	@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler({ Throwable.class })
	public void handle(Throwable e) {
		boolean genericExceptionHandlerIgual = genericExceptionHandler == null;
		if(genericExceptionHandlerIgual) {
			CcpErrorExceptionHandlerIsMissing ccpErrorExceptionHandlerIsMissing = new CcpErrorExceptionHandlerIsMissing(e);
			throw ccpErrorExceptionHandlerIsMissing;
		}
		CcpJsonRepresentation put = getHandledExceptionToLog(e);
		
		genericExceptionHandler.execute(put);
	}

	public static CcpJsonRepresentation getHandledExceptionToLog(Throwable e) {
		
		CcpJsonRepresentation json = new CcpJsonRepresentation(e);
		
		CcpJsonRepresentation put = getHandledExceptionToLog(json);
		return put;
	}
 
	private static boolean doesNotBelongToDomain(String stack, List<String> systems) {
		
		for (String system : systems) {
			boolean contains = stack.contains(system);
			if(contains) {
				return false;
			}
		}
		
		return true;
	}
	
	public static CcpJsonRepresentation getHandledExceptionToLog(CcpJsonRepresentation json) {
		String application_propertiesName = JsonFieldNames.application_properties.name();
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator(application_propertiesName);
		CcpPropertiesDecorator propertiesFrom = ccpStringDecorator.propertiesFrom();
		CcpJsonRepresentation systemProperties = propertiesFrom.environmentVariablesOrClassLoaderOrFile();
		CcpStringDecorator asStringDecorator = json.getAsStringDecorator(CcpJsonRepresentation.Fields.cause);
		boolean asStringDecoratorList = asStringDecorator.isList();
		boolean hasNoCause = false == asStringDecoratorList;
		
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
				int valor = -1;

				boolean settingEndIndex = startIndex > valor;
				
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
		int valor2 = -1;

		boolean settingEndIndex2 = startIndex > valor2;
		boolean found = settingEndIndex2;
		
		if(found) {
			int stackTraceSize = stackTrace.size();
			boolean endIndexMenor = endIndex <  stackTraceSize;
		
			if(endIndexMenor) {
				endIndex++;
			}
			
			newStackTrace = stackTrace.subList(startIndex, endIndex);
		}
		String toString = newStackTrace.toString();
		CcpStringDecorator ccpStringDecorator2 = new CcpStringDecorator(toString);
		CcpHashDecorator ccpStringDecorator2Hash = ccpStringDecorator2.hash();
		String stackTraceHash = ccpStringDecorator2Hash.asString(CcpHashAlgorithm.SHA1); 
		CcpJsonRepresentation put4 = json.put(JsonFieldNames.stackTraceHash, stackTraceHash);
		CcpJsonRepresentation put = put4.put(CcpJsonRepresentation.Fields.stackTrace, newStackTrace);
		
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
