package ch.admin.bit.jeap.log.aws;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

public class EcsContainerMetadataAttributeProvider extends AbstractJsonProvider<ILoggingEvent>  {

    private static final String TASK_DEFINITION_VERSION = "taskDefinitionVersion";

    private String taskDefinitionVersion;


    /**
     * Starts the process of retrieving task definition version metadata for an Amazon ECS task.
     *
     * <p>
     * This method first calls the {@code start()} method of the superclass to ensure proper initialization.
     * It then retrieves the URI of the ECS container metadata endpoint from the environment variable
     * {@code ECS_CONTAINER_METADATA_URI_V4}. If the environment variable is present, it creates a
     * {@link RestClient} and makes a GET request to the ECS container metadata URI. If the response
     * body is not null, it extracts the task definition version from the metadata and assigns it to
     * the {@code taskDefinitionVersion} variable.
     * </p>
     *
     * <p>
     * The environment variable {@code ECS_CONTAINER_METADATA_URI_V4} is injected by default into the
     * containers of Amazon ECS tasks. More information about ECS container metadata can be found in
     * the <a href="https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-metadata-endpoint-v4.html">
     * Amazon ECS Developer Guide</a>.
     * </p>
     */
    @Override
    public void start() {
        super.start();

        // The environment variable is injected by default into the containers of Amazon ECS tasks
        // see https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-metadata-endpoint-v4.html
        String ecsContainerMetadataUriV4 = System.getenv("ECS_CONTAINER_METADATA_URI_V4");

        if (ecsContainerMetadataUriV4 != null) {
            RestClient restClient = RestClient.create();
            String ecsContainerMetadata = restClient.get().uri(ecsContainerMetadataUriV4).retrieve().body(String.class);
            if (ecsContainerMetadata != null) {
                taskDefinitionVersion = parseTaskDefinitionVersion(ecsContainerMetadata);
            }
        }
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent iLoggingEvent) throws IOException {
        JsonWritingUtils.writeStringField(generator, TASK_DEFINITION_VERSION, taskDefinitionVersion);
    }

    /**
     * Parses the task definition version from ECS container metadata.
     *
     * <p>
     * This method extracts the task definition version from the provided ECS container metadata.
     * It expects the metadata to be in JSON format. It retrieves the value associated with the
     * key {@code "com.amazonaws.ecs.task-definition-version"} from the metadata. If the metadata
     * is not in the expected format, or if the key-value pair is not found, it returns an empty
     * string.
     * </p>
     *
     * @param ecsContainerMetadata The ECS container metadata in JSON format.
     * @return The task definition version extracted from the metadata, or an empty string if
     *         the metadata is null, not in the expected format, or if the task definition version
     *         is not found.
     */
    static protected String parseTaskDefinitionVersion(String ecsContainerMetadata) {
        if (ecsContainerMetadata != null) {
            try {
                JsonParser jsonParser = JsonParserFactory.getJsonParser();
                Map<String, Object> map = jsonParser.parseMap(ecsContainerMetadata);
                Map<String, Object> labelsMap = (Map<String, Object>) map.get("Labels");
                return (String) labelsMap.get("com.amazonaws.ecs.task-definition-version");
            } catch (ClassCastException castException) {
                // In case of unexpected metadata format, return an empty string
                return "";
            }
        }

        // Return an empty string if metadata is null
        return "";
    }
}
