package com.ccp.rest.api.spring.servlet.request;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;

/** Reads the JSON body of a request with Jackson. */
public interface CcpJsonExtractorFromHttpServletRequest {
	/**
	 * Reads the body of the request as a JSON object.
	 * @param request the request
	 * @return the body as a map
	 * @throws IOException when the body cannot be read
	 * @throws StreamReadException when the body is not valid JSON
	 * @throws DatabindException when the body is not a JSON object
	 */
	@SuppressWarnings("unchecked")
	default Map<String, Object> extractJsonFromHttpServletRequest(ServletRequest request)
			throws IOException, StreamReadException, DatabindException {
		ObjectMapper mapper = new ObjectMapper();
		ServletInputStream inputStream = request.getInputStream();
		Map<String, Object> originalJson = mapper.readValue(inputStream, Map.class);
		return originalJson;
	}

}
