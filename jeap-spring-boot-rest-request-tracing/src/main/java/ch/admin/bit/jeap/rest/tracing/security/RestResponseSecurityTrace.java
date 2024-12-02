package ch.admin.bit.jeap.rest.tracing.security;

public record RestResponseSecurityTrace(
        String method,
        String requestUriPattern,
        Integer statusCode) {
}
