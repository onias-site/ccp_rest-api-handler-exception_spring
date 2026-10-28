package com.ccp.rest.api.spring.response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.ccp.aop.CcpAllowNullParameter;
import com.ccp.aop.CcpAllowNullReturn;
import com.ccp.rest.api.spring.servlet.request.CcpPutSessionValuesRequestWrapper;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Takes out of every JSON response the session values that {@link CcpPutSessionValuesRequestWrapper} put in the request
 * body ({@code sessionToken}, {@code userAgent}, {@code ip}): the services answer the body they received, and until
 * 2026-10-26 the skill suggestion, the hierarchy adjustment, the contact us and the refusals of the login echoed them. A
 * field is taken out only when it holds the very value injected, so a value produced by the service (the new
 * {@code sessionToken} of a login) still goes to the client. Covers the controllers and the exception handlers.
 */
@RestControllerAdvice
public class CcpRestApiSessionValuesRemoverSpring implements ResponseBodyAdvice<Object> {

	/**
	 * Every response is checked.
	 * @param returnType the return type of the handler
	 * @param converterType the converter chosen
	 * @return always {@code true}
	 */
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	/**
	 * Answers the body without the session values injected in the request.
	 * @param body the body of the response ({@code null} when there is none)
	 * @param returnType the return type of the handler
	 * @param selectedContentType the content type of the response
	 * @param selectedConverterType the converter chosen
	 * @param request the request
	 * @param response the response
	 * @return the body, without the injected session values when it is a JSON object
	 */
	@SuppressWarnings("unchecked")
	@CcpAllowNullParameter
	@CcpAllowNullReturn
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

		boolean notAJsonObject = false == body instanceof Map;
		if(notAJsonObject) {
			return body;
		}

		boolean notAServletRequest = false == request instanceof ServletServerHttpRequest;
		if(notAServletRequest) {
			return body;
		}

		ServletServerHttpRequest servletServerHttpRequest = (ServletServerHttpRequest) request;
		HttpServletRequest servletRequest = servletServerHttpRequest.getServletRequest();
		String injectedSessionValuesAttribute = CcpPutSessionValuesRequestWrapper.RequestAttributes.injectedSessionValues.getValue();
		Object injectedSessionValuesAttributeValue = servletRequest.getAttribute(injectedSessionValuesAttribute);

		boolean noSessionValuesInjected = injectedSessionValuesAttributeValue == null;
		if(noSessionValuesInjected) {
			return body;
		}

		Map<String, Object> injectedSessionValues = (Map<String, Object>) injectedSessionValuesAttributeValue;
		Map<String, Object> responseBody = (Map<String, Object>) body;
		Map<String, Object> responseBodyWithoutSessionValues = new LinkedHashMap<>(responseBody);

		for (Map.Entry<String, Object> injectedSessionValue : injectedSessionValues.entrySet()) {
			String fieldName = injectedSessionValue.getKey();
			boolean notAnswered = false == responseBodyWithoutSessionValues.containsKey(fieldName);
			if(notAnswered) {
				continue;
			}
			Object injectedValue = injectedSessionValue.getValue();
			Object answeredValue = responseBodyWithoutSessionValues.get(fieldName);
			boolean producedByTheService = false == Objects.equals(injectedValue, answeredValue);
			if(producedByTheService) {
				continue;
			}
			responseBodyWithoutSessionValues.remove(fieldName);
		}
		return responseBodyWithoutSessionValues;
	}
}
