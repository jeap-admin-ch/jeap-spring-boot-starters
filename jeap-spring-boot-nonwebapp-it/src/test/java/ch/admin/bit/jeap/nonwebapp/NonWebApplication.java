package ch.admin.bit.jeap.nonwebapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Plain Spring Boot application used to start the logging and monitoring starters in a non-web
 * (non-servlet) context. There is intentionally no web starter on the classpath, so the servlet
 * API (jakarta.servlet) is absent - this mirrors a real non-webapp deployment.
 */
@SpringBootApplication
public class NonWebApplication {
}
