package com.ccp.web.servlet.filters;

import com.ccp.decorators.CcpStringDecorator;
import com.ccp.process.CcpDefaultProcessStatus;
import com.ccp.web.servlet.exceptions.CcpInvalidUrlToFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CcpValidEmailFilter implements Filter{
	
	private final String[] filtered;
	
	public CcpValidEmailFilter(String... filtered) {
		this.filtered = filtered;
	}

	public static CcpValidEmailFilter getEmailSyntaxFilter(String... filtered) {
		CcpValidEmailFilter ccpValidEmailFilter = new CcpValidEmailFilter(filtered);
		return ccpValidEmailFilter;
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain){

		HttpServletRequest request = (HttpServletRequest) req;

		HttpServletResponse response = (HttpServletResponse) res;

		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, HEAD, PATCH");
		response.setHeader("Access-Control-Max-Age", "3600");

		response.setHeader("Access-Control-Allow-Headers",
				"Access-Control-Allow-Headers, X-Requested-With, authorization, token, email, Content-Type, Authorization, Access-Control-Request-Methods, Access-Control-Request-Headers");

		String method = request.getMethod();

		boolean optionsMethod = "OPTIONS".equalsIgnoreCase(method);

		if (optionsMethod) {
			return;
		}

		StringBuffer requestURL = request.getRequestURL();
		String url = new CcpStringDecorator(requestURL.toString()).url().asDecoded();
		String email = this.extractEmail(url);
		boolean invalidEmail = new CcpStringDecorator(email).email().isValid() == false;
		if(invalidEmail) {
			response.setStatus(CcpDefaultProcessStatus.BAD_REQUEST.asNumber());
			return;
		}
		try {
			chain.doFilter(request, response);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}

	private String extractEmail(String url) {
		
		for (String string : this.filtered) {
			int indexOf = url.indexOf(string);
			if(indexOf < 0) {
				continue;
			}
			int sum = url.indexOf(string) + string.length();
			String urlSecondPiece = url.substring(sum);
			String[] split = urlSecondPiece.split("/");
			String email = split[0];
			return email;
		}
		
		throw new CcpInvalidUrlToFilter(url, this.filtered);
	}
	
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	
	public void destroy() {
		
	}

	public String toString() {
		return "CcpValidEmailFilter [filtered=" + filtered + "]";
	}

}
