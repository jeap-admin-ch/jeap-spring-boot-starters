package ch.admin.bit.jeap.security.resource.properties;

public enum IntrospectionMode {

    NONE, EXPLICIT, ALWAYS, LIGHTWEIGHT, CUSTOM;

    /**
     * Whether this introspection mode activates introspection.
     */
    public boolean doesActivateIntrospection() {
        return this != NONE;
    }

}
