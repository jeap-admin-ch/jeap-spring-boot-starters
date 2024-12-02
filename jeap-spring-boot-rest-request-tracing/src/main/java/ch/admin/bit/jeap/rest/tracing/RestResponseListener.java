package ch.admin.bit.jeap.rest.tracing;

public interface RestResponseListener {

    void onResponse(RestResponseTrace restResponseTrace);

    boolean isResponseListenerActive();
}
