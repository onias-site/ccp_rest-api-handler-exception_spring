package com.ccp.rest.api.spring.exceptions.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
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
 * Global Spring Boot exception handler. Handles {@code CcpJsonValidationError} (422),
 * {@code CcpErrorFlowDisturb} (dynamic status) and any generic {@code Throwable} (500),
 * filtering the stack trace down to domain lines only and computing a SHA1 hash
 * for traceability.
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
		int statusCode = e.status.asNumber();
	
		res.setStatus(statusCode);
		String message = e.getMessage();
		
		CcpJsonRepresentation result = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.message, message);
		
		boolean noFields = e.fields.length <= 0;
		
		if(noFields) {
			String statusName = e.status.name();
			CcpJsonRepresentation resultWithStatus = result.put(JsonFieldNames.status, statusName);
			return resultWithStatus.content;
		}

		CcpJsonRepresentation requestedFields = e.json.getJsonPiece(e.fields);

		CcpJsonRepresentation resultWithFields = result.mergeWithAnotherJson(requestedFields);
		String statusName = e.status.name();
		CcpJsonRepresentation resultWithFieldsAndStatus = resultWithFields.put(JsonFieldNames.status, statusName);

		return resultWithFieldsAndStatus.content;
	}

	/**
	 * Last resort for whatever has no handler of its own. Spring exceptions that represent a client
	 * error implement {@code ErrorResponse} and already know their status — nonexistent route (404), method
	 * not supported (405), media type not supported (415), missing parameter or unreadable body (400). Those
	 * get their own status and are not recorded as system errors. Until 2026-09-27 all of them became
	 * 500 and were recorded in {@code JnEntityJobsnowError} — every URL scan by a bot produced a recorded
	 * error and, the first time in the hour, a notice to support.
	 */
	@ExceptionHandler({ Throwable.class })
	public void handle(Throwable e, HttpServletResponse res) {

		if(e instanceof ErrorResponse clientError) {
			int clientErrorStatus = clientError.getStatusCode().value();
			res.setStatus(clientErrorStatus);
			return;
		}

		res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

		boolean handlerIsMissing = genericExceptionHandler == null;
		if(handlerIsMissing) {
			CcpErrorExceptionHandlerIsMissing ccpErrorExceptionHandlerIsMissing = new CcpErrorExceptionHandlerIsMissing(e);
			throw ccpErrorExceptionHandlerIsMissing;
		}
		CcpJsonRepresentation handledException = getHandledExceptionToLog(e);
		
		genericExceptionHandler.execute(handledException);
	}

	public static CcpJsonRepresentation getHandledExceptionToLog(Throwable e) {
		
		CcpJsonRepresentation json = new CcpJsonRepresentation(e);
		
		CcpJsonRepresentation handledException = getHandledExceptionToLog(json);
		return handledException;
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
		String propertiesFileName = JsonFieldNames.application_properties.name();
		CcpStringDecorator propertiesFileDecorator = new CcpStringDecorator(propertiesFileName);
		CcpPropertiesDecorator propertiesDecorator = propertiesFileDecorator.propertiesFrom();
		CcpJsonRepresentation systemProperties = propertiesDecorator.environmentVariablesOrClassLoaderOrFile();
		CcpStringDecorator causeDecorator = json.getAsStringDecorator(CcpJsonRepresentation.CcpStackTraceFields.cause);
		boolean causeIsList = causeDecorator.isList();
		boolean hasNoCause = false == causeIsList;
		
		if(hasNoCause) {
			json = json.put(CcpJsonRepresentation.CcpStackTraceFields.cause, new ArrayList<>());
		}
		
		CcpJsonRepresentation jsonWithStackTrace = getHandledExceptionToLog(json, systemProperties, CcpJsonRepresentation.CcpStackTraceFields.completeStackTrace);
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
				int notFound = -1;

				boolean settingEndIndex = startIndex > notFound;
				
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
		int notFound = -1;

		boolean startIndexWasFound = startIndex > notFound;
		boolean found = startIndexWasFound;
		
		if(found) {
			int stackTraceSize = stackTrace.size();
			boolean endIndexWithinStackTrace = endIndex <  stackTraceSize;
		
			if(endIndexWithinStackTrace) {
				endIndex++;
			}
			
			newStackTrace = stackTrace.subList(startIndex, endIndex);
		}
		String newStackTraceAsText = newStackTrace.toString();
		CcpStringDecorator stackTraceDecorator = new CcpStringDecorator(newStackTraceAsText);
		CcpHashDecorator stackTraceHashDecorator = stackTraceDecorator.hash();
		String stackTraceHash = stackTraceHashDecorator.asString(CcpHashAlgorithm.SHA1); 
		CcpJsonRepresentation jsonWithStackTraceHash = json.put(JsonFieldNames.stackTraceHash, stackTraceHash);
		CcpJsonRepresentation handledException = jsonWithStackTraceHash.put(CcpJsonRepresentation.CcpStackTraceFields.stackTrace, newStackTrace);
		
		return handledException;
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
