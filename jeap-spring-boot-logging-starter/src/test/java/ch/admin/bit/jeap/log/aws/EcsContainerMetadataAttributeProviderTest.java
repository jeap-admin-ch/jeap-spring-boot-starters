package ch.admin.bit.jeap.log.aws;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EcsContainerMetadataAttributeProviderTest {

    @Test
    void parseTaskDefinitionVersion() throws Exception {
            String content = readFileToString("ecsContainerMetadataExample.json");
            String returnValue = EcsContainerMetadataAttributeProvider.parseTaskDefinitionVersion(content);
            assertEquals("10", returnValue);

    }

    public String readFileToString(String filePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

}