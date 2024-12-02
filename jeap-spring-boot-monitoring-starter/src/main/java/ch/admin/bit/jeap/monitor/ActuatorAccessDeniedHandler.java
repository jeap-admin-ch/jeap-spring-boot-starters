package ch.admin.bit.jeap.monitor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@Slf4j
public class ActuatorAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)  {
		if (response.isCommitted()) {
			log.trace("Not touching response as it is already committed");
			return;
		}
		log.debug("Access denied to actuator {}. Returning HTTP status {}." + request.getRequestURI(), HttpStatus.FORBIDDEN.value());
		// Just set the status, do not dispatch the error handling to the error page. Otherwise, we would have to
		// make the error page accessible for at least the dispatch type 'error'. But we shouldn't do that here in the
		// actuator security configuration as the error page and access to it should be under the control of the application,
		// not some library.
		response.setStatus(HttpStatus.FORBIDDEN.value());
	}

}
