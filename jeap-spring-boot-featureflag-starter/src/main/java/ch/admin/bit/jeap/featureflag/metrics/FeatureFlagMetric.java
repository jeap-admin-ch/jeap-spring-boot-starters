package ch.admin.bit.jeap.featureflag.metrics;

import org.togglz.core.Feature;

record FeatureFlagMetric(String client, Feature feature) {}
