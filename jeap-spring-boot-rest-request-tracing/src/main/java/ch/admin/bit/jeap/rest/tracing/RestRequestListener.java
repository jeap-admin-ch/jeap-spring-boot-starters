package ch.admin.bit.jeap.rest.tracing;

public interface RestRequestListener {

    void onRequest(RestRequestTrace restRequestTrace);

    boolean isRequestListenerActive();
}
