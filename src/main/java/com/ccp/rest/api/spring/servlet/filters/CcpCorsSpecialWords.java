package com.ccp.rest.api.spring.servlet.filters;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * CORS header names whose real value contains a hyphen and therefore cannot be written as a
 * Java identifier. Follows the same pattern as {@code ElasticSearchDbRequesterSpecialWords}:
 * the constant has a legal name and the real value comes from the constructor, exposed by
 * {@code getValue()}.
 *
 * Used by both filters of the package ({@code CcpPutSessionValuesAndExecuteTaskFilter} and
 * {@code CcpValidEmailFilter}), which declare the same CORS block.
 */
enum CcpCorsSpecialWords implements CcpJsonFieldName {
	Access_Control_Allow_Origin("Access-Control-Allow-Origin"),
	Access_Control_Allow_Methods("Access-Control-Allow-Methods"),
	Access_Control_Max_Age("Access-Control-Max-Age"),
	Access_Control_Allow_Headers("Access-Control-Allow-Headers"),
;
	private final String value;

	private CcpCorsSpecialWords(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
