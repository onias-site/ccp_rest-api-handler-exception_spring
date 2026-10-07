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
	/** Fields of the error responses and of the logged errors. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The error message. */
		message,
		/** The stack trace reduced to the lines of the system packages. */
		stackTrace,
		/** The cause chain, one "type: message" line per cause. */
		cause,
		/** Property listing the package prefixes that belong to the system. */
		systems,
		/** Name of the properties resource. */
		application_properties,
		/** SHA-1 of the reduced stack trace, which groups repeated errors. */
		stackTraceHash,
		/** The status name. */
		status
	}

	/** Handler of the unexpected errors (e.g. records and notifies them); must be set at startup. */
	public static CcpBusiness genericExceptionHandler;
 
	/**
	 * A JSON validation failure answers 422 with the whole diagnosis.
	 * @param e the validation error
	 * @return the diagnosis
	 */
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	@ExceptionHandler({ CcpJsonValidationError.class })
	public Map<String, Object> handle(CcpJsonValidationError e) {
		return e.json.content;
	}

	/**
	 * A flow disturbance answers its own status with {@code message}, {@code status} (the name) and the fields of its JSON
	 * listed in the exception.
	 * @param e the flow disturbance
	 * @param res the response
	 * @return the body
	 * @throws IOException never in practice
	 */
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
	 * Last resort for whatever has no handler of its own. Spring exceptions that represent a client error implement
	 * {@code ErrorResponse} and already know their status (nonexistent route 404, method not supported 405, media type not
	 * supported 415, missing parameter or unreadable body 400): those answer their own status and are not recorded as
	 * system errors. Until 2026-09-27 all of them became 500 and were recorded in {@code JnEntityJobsnowError}, so every
	 * URL scan by a bot produced a recorded error and, the first time in the hour, a notice to support. Any other error
	 * answers 500 and goes to {@link #genericExceptionHandler}. The body of the 500 is {@code status} (the name) and
	 * {@code stackTraceHash}, which groups the recorded error and is what the user can hand to support; the message and
	 * the stack trace are not exposed. Until 2026-10-07 the 500 answered an empty body.
	 * @param e the error
	 * @param res the response
	 * @return the body: empty for a client error, {@code status} and {@code stackTraceHash} for the 500
	 * @throws CcpErrorExceptionHandlerIsMissing when no generic handler was set
	 */
	@ResponseBody
	@ExceptionHandler({ Throwable.class })
	public Map<String, Object> handle(Throwable e, HttpServletResponse res) {

		if(e instanceof ErrorResponse clientError) {
			int clientErrorStatus = clientError.getStatusCode().value();
			res.setStatus(clientErrorStatus);
			return CcpOtherConstants.EMPTY_JSON.content;
		}

		res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

		boolean handlerIsMissing = genericExceptionHandler == null;
		if(handlerIsMissing) {
			CcpErrorExceptionHandlerIsMissing ccpErrorExceptionHandlerIsMissing = new CcpErrorExceptionHandlerIsMissing(e);
			throw ccpErrorExceptionHandlerIsMissing;
		}
		CcpJsonRepresentation handledException = getHandledExceptionToLog(e);

		genericExceptionHandler.execute(handledException);

		String stackTraceHash = handledException.getAsString(JsonFieldNames.stackTraceHash);
		String statusName = HttpStatus.INTERNAL_SERVER_ERROR.name();
		CcpJsonRepresentation resultWithStatus = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.status, statusName);
		CcpJsonRepresentation result = resultWithStatus.put(JsonFieldNames.stackTraceHash, stackTraceHash);
		return result.content;
	}

	/**
	 * Builds the error details to be logged (see {@link #getHandledExceptionToLog(CcpJsonRepresentation)}).
	 * @param e the error
	 * @return the details to log
	 */
	public static CcpJsonRepresentation getHandledExceptionToLog(Throwable e) {
		
		CcpJsonRepresentation json = new CcpJsonRepresentation(e);
		
		CcpJsonRepresentation handledException = getHandledExceptionToLog(json);
		return handledException;
	}
 
	/**
	 * Tells whether the stack trace line belongs to none of the system packages.
	 * @param stack the stack trace line
	 * @param systems the package prefixes of the system
	 * @return {@code true} for a line outside the system
	 */
	private static boolean doesNotBelongToDomain(String stack, List<String> systems) {
		
		for (String system : systems) {
			boolean contains = stack.contains(system);
			if(contains) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Prepares the error details to be logged: flattens the cause chain into text lines and reduces the complete stack
	 * trace to its first contiguous block of system lines (plus the next line), adding its hash.
	 * @param json the error details
	 * @return the details to log
	 */
	public static CcpJsonRepresentation getHandledExceptionToLog(CcpJsonRepresentation json) {
		String propertiesFileName = JsonFieldNames.application_properties.name();
		CcpStringDecorator propertiesFileDecorator = new CcpStringDecorator(propertiesFileName);
		CcpPropertiesDecorator propertiesDecorator = propertiesFileDecorator.propertiesFrom();
		CcpJsonRepresentation systemProperties = propertiesDecorator.environmentVariablesOrClassLoaderOrFile();
		CcpStringDecorator causeDecorator = json.getAsStringDecorator(CcpJsonRepresentation.CcpStackTraceFields.cause);
		boolean causeIsList = causeDecorator.isList();
		boolean causeIsNotList = false == causeIsList;

		if(causeIsNotList) {
			List<String> causeChain = getCauseChain(json);
			json = json.put(CcpJsonRepresentation.CcpStackTraceFields.cause, causeChain);
		}

		CcpJsonRepresentation jsonWithStackTrace = getHandledExceptionToLog(json, systemProperties, CcpJsonRepresentation.CcpStackTraceFields.completeStackTrace);
		return jsonWithStackTrace;
	}

	/**
	 * The cause arrives as a nested JSON (type, message, stack trace and its own cause), but the error entity stores it as
	 * an array of text. Flattens the chain into one "type: message" line per cause, from the direct cause down to the root
	 * one; the stack trace lines of the causes are already in the complete stack trace. Until 2026-09-30 the nested JSON was
	 * replaced by an empty array, losing the root message.
	 * @param json the error details
	 * @return the cause lines
	 */
	private static List<String> getCauseChain(CcpJsonRepresentation json) {
		List<String> causeChain = new ArrayList<>();
		CcpJsonRepresentation current = json;

		while(current.isInnerJson(CcpJsonRepresentation.CcpStackTraceFields.cause)) {
			current = current.getInnerJson(CcpJsonRepresentation.CcpStackTraceFields.cause);
			String causeType = current.getAsString(CcpJsonRepresentation.CcpStackTraceFields.type);
			String causeMessage = current.getAsString(CcpJsonRepresentation.CcpStackTraceFields.message);
			String causeLine = causeType + ": " + causeMessage;
			causeChain.add(causeLine);
		}

		return causeChain;
	}

	/**
	 * Keeps, of the stack trace in {@code field}, the first contiguous block of lines of the system packages plus the line
	 * right after it, stores it as {@code stackTrace} and adds its SHA-1 as {@code stackTraceHash}.
	 * @param json the error details
	 * @param systemProperties the properties holding {@code systems}
	 * @param field the field holding the stack trace
	 * @return the reduced details
	 */
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
	
	/** A method not supported answers 405. */
	@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
	@ExceptionHandler({ org.springframework.web.HttpRequestMethodNotSupportedException.class })
	public void methodNoSupported() {

	}

	/** Raised when an unexpected error happens and no {@link #genericExceptionHandler} was set. */
	@SuppressWarnings("serial")
	public static class CcpErrorExceptionHandlerIsMissing extends RuntimeException {
		/**
		 * Wraps the unhandled error.
		 * @param e the unhandled error
		 */
		private CcpErrorExceptionHandlerIsMissing(Throwable e) {
			super("genericExceptionHandler must has an instance ", e);
		}
	}
}
