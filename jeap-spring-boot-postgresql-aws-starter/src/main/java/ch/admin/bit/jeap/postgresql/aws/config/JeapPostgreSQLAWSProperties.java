package ch.admin.bit.jeap.postgresql.aws.config;

import lombok.Data;
import software.amazon.awssdk.regions.Region;

@Data
public class JeapPostgreSQLAWSProperties {

    private String region = Region.EU_CENTRAL_2.id();
    private String hostname;
    private String port = "5432";
    private String databaseName;

}
