package com.ccp.rest.api.spring.exceptions.handler;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Answers the error dispatch of the servlet container ({@code /error}) in place of the {@code BasicErrorController} of
 * Spring Boot, which this bean disables. The container dispatches here only what never reached a controller, so nothing
 * is recorded as a system error. A dispatch that carries its status answers that status with an empty body. One that
 * carries none answers 404: Jetty rejects the protected paths ({@code /WEB-INF/*}, {@code /META-INF/*}) this way, and
 * until 2026-11-08 Spring answered them with status 999, which Jetty turned into a 500 (a URL scan on 2026-11-05 got a 500
 * for {@code /WEB-INF/web.xml}).
 */
@RestController
public class CcpRestApiErrorControllerSpring implements ErrorController {

	/** The smallest status that is an error. */
	private static final int FIRST_ERROR_STATUS = HttpStatus.BAD_REQUEST.value();

	/** The largest status there is. */
	private static final int LAST_STATUS = 599;

	/**
	 * Answers the status of the error dispatch, or 404 when the dispatch carries no valid status.
	 * @param request the request dispatched to {@code /error}
	 * @return an empty body with the status
	 */
	@RequestMapping("/error")
	public ResponseEntity<Void> handleError(HttpServletRequest request) {
		Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		boolean statusIsNotANumber = false == statusAttribute instanceof Integer;

		if(statusIsNotANumber) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		int status = (Integer) statusAttribute;
		boolean notAnErrorStatus = status < FIRST_ERROR_STATUS || status > LAST_STATUS;

		if(notAnErrorStatus) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		return ResponseEntity.status(status).build();
	}
}
