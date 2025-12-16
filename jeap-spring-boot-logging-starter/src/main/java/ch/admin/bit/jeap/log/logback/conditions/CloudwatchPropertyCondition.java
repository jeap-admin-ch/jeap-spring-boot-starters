package ch.admin.bit.jeap.log.logback.conditions;

import ch.qos.logback.core.boolex.PropertyConditionBase;

public class CloudwatchPropertyCondition extends PropertyConditionBase {

    @Override
    public boolean evaluate() {
        return property("cloudwatch").equals("true");
    }
}
