package com.ccp.rest.api.spring.exceptions.handler;

import java.util.Map;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.exceptions.process.CcpErrorFlowDisturb;
import com.ccp.rest.api.spring.servlet.exceptions.CcpErrorExceptionHandlerIsMissing;
import com.ccp.validation.CcpErrorJsonInvalid;

import jakarta.servlet.http.HttpServletResponse;

@RestControllerAdvice
public class CcpSyncExceptionHandler {

	public static Function<CcpJsonRepresentation, CcpJsonRepresentation> genericExceptionHandler;
 
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	@ExceptionHandler({ CcpErrorJsonInvalid.class })
	public Map<String, Object> handle(CcpErrorJsonInvalid e) {
		return e.result.content;
	}

	@ResponseBody
	@ExceptionHandler({ CcpErrorFlowDisturb.class })
	public Map<String, Object> handle(CcpErrorFlowDisturb e, HttpServletResponse res){
		
		res.setStatus(e.status.asNumber());
		
		String message = e.getMessage();
		
		CcpJsonRepresentation result = CcpOtherConstants.EMPTY_JSON.put("message", message);
		
		boolean noFields = e.fields.length <= 0;
		
		if(noFields) {
			return result.content;
		}
		
		CcpJsonRepresentation subMap = e.json.getJsonPiece(e.fields);
		
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
	
//	@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED)
//	@ExceptionHandler({ org.springframework.web.HttpRequestMethodNotSupportedException.class })
	public void methodNoSupported() {
		
	}
}
