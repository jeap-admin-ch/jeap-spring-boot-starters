package ch.admin.bit.jeap.featureflag;

import org.togglz.core.Feature;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

enum FeatureFlags implements Feature {

    @Label("My first feature flag")
    MY_FIRST_FEATURE_FLAG,

    @Label("My second feature flag")
    MY_SECOND_FEATURE_FLAG;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
