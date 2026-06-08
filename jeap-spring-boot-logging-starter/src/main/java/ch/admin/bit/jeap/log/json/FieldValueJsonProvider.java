package ch.admin.bit.jeap.log.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import tools.jackson.core.JsonGenerator;

/**
 * A logstash/logback JSON field provider that writes a configured, fixed field value to the output
 */
public class FieldValueJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    private String value;

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent iLoggingEvent) {
        JsonWritingUtils.writeStringField(generator, getFieldName(), value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
