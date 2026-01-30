package com.ccp.rest.api.utils;

import com.ccp.decorators.CcpPropertiesDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
public class CcpRestApiUtils {
	enum JsonFieldNames implements CcpJsonFieldName{
		localEnvironment
	}
	public static boolean isLocalEnvironment() {
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator("application_properties");
		CcpPropertiesDecorator propertiesFrom = ccpStringDecorator.propertiesFrom();
		CcpJsonRepresentation systemProperties = propertiesFrom.environmentVariablesOrClassLoaderOrFile();
		boolean localEnvironment = systemProperties.getAsBoolean(JsonFieldNames.localEnvironment);
		return localEnvironment;
	}


}
