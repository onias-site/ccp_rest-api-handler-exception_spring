package com.ccp.rest.api.utils;

import com.ccp.decorators.CcpPropertiesDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
/**
 * Utilitários compartilhados para a camada REST. Oferece {@code isLocalEnvironment()} que
 * consulta {@code application_properties} para determinar se a execução é local.
 */
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
