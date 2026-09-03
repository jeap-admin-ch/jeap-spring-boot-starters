package ch.admin.bit.jeap.web.configuration.servlet;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.http.HttpHeaders;

import java.io.IOException;

final class NoStoreOnErrorResponseWrapper extends HttpServletResponseWrapper {

    NoStoreOnErrorResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int status) {
        super.setStatus(status);
        if (status >= SC_BAD_REQUEST) {
            preventCaching();
        }
    }

    @Override
    public void sendError(int status) throws IOException {
        preventCachingIfError(status);
        super.sendError(status);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        preventCachingIfError(status);
        super.sendError(status, message);
    }

    void preventCachingIfError() {
        preventCachingIfError(getStatus());
    }

    private void preventCachingIfError(int status) {
        if (status >= SC_BAD_REQUEST) {
            preventCaching();
        }
    }

    void preventCaching() {
        setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        setHeader(HttpHeaders.EXPIRES, "0");
    }
}
