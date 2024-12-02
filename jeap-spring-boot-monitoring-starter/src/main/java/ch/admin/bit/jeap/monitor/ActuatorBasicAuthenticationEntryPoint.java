package ch.admin.bit.jeap.monitor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@Slf4j
class ActuatorBasicAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final String REALM_NAME = "actuator";

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
		log.debug("Authorization missing for accessing actuator {}. Returning HTTP status {}.",
				request.getRequestURI(), HttpStatus.UNAUTHORIZED.value());
		// Set the same headers as the standard BasicAuthenticationEntryPoint ...
		response.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealmName() + "\"");
		// ... but just set the status, do not dispatch the error handling to the error page. Otherwise, we would have to
		// make the error page accessible for at least the dispatch type 'error'. But we shouldn't do that here in the
		// actuator security configuration as the error page and access to it should be under the control of the application,
		// not some library.
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
	}

	public String getRealmName() {
		return REALM_NAME;
	}

}