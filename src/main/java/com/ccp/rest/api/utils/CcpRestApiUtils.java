package com.ccp.rest.api.utils;

import com.ccp.decorators.CcpPropertiesDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
/**
 * Shared utilities for the REST layer. Provides {@code isLocalEnvironment()}, which
 * reads {@code application_properties} to determine whether this is a local run.
 */
public class CcpRestApiUtils {
	enum JsonFieldNames implements CcpJsonFieldName{
		localEnvironment
	}
	public static boolean isLocalEnvironment() {
		CcpStringDecorator propertiesFileName = new CcpStringDecorator("application_properties");
		CcpPropertiesDecorator propertiesDecorator = propertiesFileName.propertiesFrom();
		CcpJsonRepresentation systemProperties = propertiesDecorator.environmentVariablesOrClassLoaderOrFile();
		boolean localEnvironment = systemProperties.getAsBoolean(JsonFieldNames.localEnvironment);
		return localEnvironment;
	}


}
