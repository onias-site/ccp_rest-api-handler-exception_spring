package com.ccp.rest.api.spring.exceptions.handler;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.ccp.rest.api.spring.servlet.exceptions.CcpErrorExceptionHandlerIsMissing;

import jakarta.servlet.http.HttpServletResponse;


@RestControllerAdvice
public class CcpRestApiExceptionHandlerSpring {
	enum JsonFieldNames implements CcpJsonFieldName{
		message
	}

	public static CcpBusiness genericExceptionHandler;
 
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	@ExceptionHandler({ CcpJsonValidationError.class })
	public Map<String, Object> handle(CcpJsonValidationError e) {
		return e.json.content;
	}

	@ResponseBody
	@ExceptionHandler({ CcpErrorFlowDisturb.class })
	public Map<String, Object> handle(CcpErrorFlowDisturb e, HttpServletResponse res){
		
		res.setStatus(e.status.asNumber());
		
		String message = e.getMessage();
		
		CcpJsonRepresentation result = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.message, message);
		
		boolean noFields = e.fields.length <= 0;
		
		if(noFields) {
			return result.content;
		}
		
		CcpJsonRepresentation subMap = e.json.getDynamicVersion().getJsonPiece(e.fields);
		
		CcpJsonRepresentation putAll = result.putAll(subMap);
		
		return putAll.content;
	}

	@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler({ Throwable.class })
	public void handle(Throwable e) {
		if(genericExceptionHandler == null) {
			throw new CcpErrorExceptionHandlerIsMissing(e);
		}
		CcpJsonRepresentation json = new CcpJsonRepresentation(e);
		genericExceptionHandler.apply(json);
	}
	
	@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
	@ExceptionHandler({ org.springframework.web.HttpRequestMethodNotSupportedException.class })
	public void methodNoSupported() {
		
	}
}
