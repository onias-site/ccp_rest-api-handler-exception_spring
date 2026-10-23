package com.ccp.rest.api.spring.openapi;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;

import com.ccp.decorators.CcpJsonFieldName;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;

/**
 * Adds to the OpenAPI document the path variables that an operation uses in its path but does not declare. Shared by
 * the REST APIs of every cost center; until 2026-10-22 each starter declared it as a lambda of its own.
 */
public class CcpMissingPathParamsCustomizer implements GlobalOpenApiCustomizer {

	/** The single instance. */
	public static final CcpMissingPathParamsCustomizer INSTANCE = new CcpMissingPathParamsCustomizer();

	enum Fields implements CcpJsonFieldName {
		/** The location of a path parameter. */
		path,
		/** The variables of a path template, as {@code {name}}. */
		TEMPLATE_VARIABLE { public String getValue() { return "\\{(\\w+)\\}"; } },
	}

	/** Finds the variables of a path template. */
	private final Pattern templateVariable;

	private CcpMissingPathParamsCustomizer() {
		String templateVariableRegex = Fields.TEMPLATE_VARIABLE.getValue();
		this.templateVariable = Pattern.compile(templateVariableRegex);
	}

	/**
	 * Declares the missing path variables of every operation of the document.
	 * @param openApi the document
	 */
	public void customise(OpenAPI openApi) {
		Paths paths = openApi.getPaths();
		boolean pathsMissing = paths == null;
		if (pathsMissing) {
			return;
		}
		Set<Entry<String, PathItem>> pathEntries = paths.entrySet();
		for (Entry<String, PathItem> pathEntry : pathEntries) {
			String pathTemplate = pathEntry.getKey();
			Set<String> templateVars = this.getTemplateVars(pathTemplate);
			boolean templateVarsEmpty = templateVars.isEmpty();
			if (templateVarsEmpty) {
				continue;
			}
			PathItem pathItem = pathEntry.getValue();
			List<Operation> readOperations = pathItem.readOperations();
			for (Operation operation : readOperations) {
				addMissingPathParameters(operation, templateVars);
			}
		}
	}

	/**
	 * Lists the variables of a path template.
	 * @param pathTemplate the path template
	 * @return the names of its variables
	 */
	private Set<String> getTemplateVars(String pathTemplate) {
		Set<String> templateVars = new HashSet<>();
		Matcher matcher = this.templateVariable.matcher(pathTemplate);
		while (matcher.find()) {
			String group = matcher.group(1);
			templateVars.add(group);
		}
		return templateVars;
	}

	/**
	 * Declares in the operation the path variables it does not declare yet.
	 * @param operation the operation
	 * @param templateVars the variables of its path
	 */
	private static void addMissingPathParameters(Operation operation, Set<String> templateVars) {
		Set<String> declared = getDeclaredPathParameters(operation);
		for (String templateVar : templateVars) {
			boolean alreadyDeclared = declared.contains(templateVar);
			if (alreadyDeclared) {
				continue;
			}
			StringSchema schema = new StringSchema();
			PathParameter parameter = new PathParameter();
			Parameter namedParameter = parameter.name(templateVar);
			Parameter requiredParameter = namedParameter.required(true);
			Parameter parameterWithSchema = requiredParameter.schema(schema);
			operation.addParametersItem(parameterWithSchema);
		}
	}

	/**
	 * Lists the path parameters the operation already declares.
	 * @param operation the operation
	 * @return the names of its path parameters
	 */
	private static Set<String> getDeclaredPathParameters(Operation operation) {
		Set<String> declared = new HashSet<>();
		List<Parameter> parameters = operation.getParameters();
		boolean hasNoParameters = parameters == null;
		if (hasNoParameters) {
			return declared;
		}
		String pathLocation = Fields.path.getValue();
		for (Parameter parameter : parameters) {
			String location = parameter.getIn();
			boolean isPathParameter = pathLocation.equals(location);
			if (isPathParameter == false) {
				continue;
			}
			String name = parameter.getName();
			declared.add(name);
		}
		return declared;
	}
}
