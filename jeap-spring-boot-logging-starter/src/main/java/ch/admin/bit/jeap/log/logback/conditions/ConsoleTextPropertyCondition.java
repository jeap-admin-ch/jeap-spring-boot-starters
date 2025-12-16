package ch.admin.bit.jeap.log.logback.conditions;

import ch.qos.logback.core.boolex.PropertyConditionBase;

public class ConsoleTextPropertyCondition extends PropertyConditionBase {

    @Override
    public boolean evaluate() {
        return property("consoletext").equals("true");
    }
}
