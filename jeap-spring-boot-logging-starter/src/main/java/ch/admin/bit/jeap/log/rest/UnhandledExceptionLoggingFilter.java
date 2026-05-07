package ch.admin.bit.jeap.log.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs exceptions that escape Spring's MVC handling before they bubble up to the servlet container,
 * while the tracing context (trace id etc.) is still present in the MDC. Re-throws the exception
 * unchanged so the application's response behavior is not affected.
 * <p>
 * Registered just inside Spring Boot's {@code ServerHttpObservationFilter} (which opens the
 * observation scope that, via Micrometer Tracing, populates {@code traceId}/{@code spanId} in the
 * MDC) but outside Spring Security's filter chain and Spring Boot's {@code ErrorPageFilter}, so
 * exceptions those layers handle themselves are not logged here.
 */
@Slf4j
public class UnhandledExceptionLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("An unhandled exception occurred during {} {}", request.getMethod(), request.getRequestURI(), ex);
            throw ex;
        }
    }
}
