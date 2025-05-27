package com.ccp.rest.api.utils;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpPropertiesDecorator;
import com.ccp.decorators.CcpStringDecorator;

public class CcpRestApiUtils {
	public static boolean isLocalEnvironment() {
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator("application_properties");
		CcpPropertiesDecorator propertiesFrom = ccpStringDecorator.propertiesFrom();
		CcpJsonRepresentation systemProperties = propertiesFrom.environmentVariablesOrClassLoaderOrFile();
		boolean localEnvironment = systemProperties.getAsBoolean("localEnvironment");
		return localEnvironment;
	}


}
