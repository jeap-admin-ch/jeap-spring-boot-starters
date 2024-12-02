package ch.admin.bit.jeap.rest.tracing.security;

public interface RestSecurityResponseListener {
    void onResponse(RestResponseSecurityTrace restResponseSecurityTrace);
}
