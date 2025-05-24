package com.ccp.web.servlet.request;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;

public interface CcpJsonExtractorFromHttpServletRequest {
	@SuppressWarnings("unchecked")
	default Map<String, Object> extractJsonFromHttpServletRequest(ServletRequest request)
			throws IOException, StreamReadException, DatabindException {
		ObjectMapper mapper = new ObjectMapper();
		ServletInputStream inputStream = request.getInputStream();
		Map<String, Object> originalJson = mapper.readValue(inputStream, Map.class);
		return originalJson;
	}

}
