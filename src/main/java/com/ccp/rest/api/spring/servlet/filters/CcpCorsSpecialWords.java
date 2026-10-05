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
	/** The {@code Access-Control-Allow-Origin} header. */
	Access_Control_Allow_Origin("Access-Control-Allow-Origin"),
	/** The {@code Access-Control-Allow-Methods} header. */
	Access_Control_Allow_Methods("Access-Control-Allow-Methods"),
	/** The {@code Access-Control-Max-Age} header. */
	Access_Control_Max_Age("Access-Control-Max-Age"),
	/** The {@code Access-Control-Allow-Headers} header. */
	Access_Control_Allow_Headers("Access-Control-Allow-Headers"),
;
	/** The real header name. */
	private final String value;

	/**
	 * Associates the constant with its real name.
	 * @param value the real name
	 */
	private CcpCorsSpecialWords(String value) {
		this.value = value;
	}

	/**
	 * Returns the real header name.
	 * @return the real name
	 */
	public String getValue() {
		return this.value;
	}
}
