package com.ccp.rest.api.spring.servlet.filters;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Nomes de header CORS cujo valor real contém hífen e por isso não pode ser escrito como
 * identificador Java. Segue o mesmo padrão de {@code ElasticSearchDbRequesterSpecialWords}:
 * a constante tem nome legal e o valor de verdade vem do construtor, exposto por
 * {@code getValue()}.
 *
 * Serve aos dois filtros do pacote ({@code CcpPutSessionValuesAndExecuteTaskFilter} e
 * {@code CcpValidEmailFilter}), que declaram o mesmo bloco CORS.
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
