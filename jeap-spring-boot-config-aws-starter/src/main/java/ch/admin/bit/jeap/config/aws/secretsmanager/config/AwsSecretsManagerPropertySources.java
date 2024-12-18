/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.admin.bit.jeap.config.aws.secretsmanager.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Notice: This class is based on code from the Spring Cloud AWS project, which is licensed under the Apache License 2.0
 * and available at <a href="https://github.com/awspring/spring-cloud-aws">github.com/awspring/spring-cloud-aws</a>.
 */
public class AwsSecretsManagerPropertySources {

    @SuppressWarnings("FieldMayBeFinal")
    private static Log LOG = LogFactory.getLog(AwsSecretsManagerPropertySources.class);

    @Nullable
    public AwsSecretsManagerPropertySource createPropertySource(AwsSecretsManagerConfigDataResource resource, SecretsManagerClient client) {
        Assert.notNull(resource, "resource is required");
        Assert.notNull(client, "SecretsManagerClient is required");

        LOG.info("Loading secrets from AWS Secret Manager secret with name: " + resource.getSecretName() + ", optional: " + resource.isOptional());
        try {
            AwsSecretsManagerPropertySource propertySource = new AwsSecretsManagerPropertySource(resource, client);
            propertySource.initPropertiesFromAwsSecretsManager();
            return propertySource;
        } catch (Exception e) {
            LOG.warn("Unable to load AWS secret from " + resource.getSecretName() + ". " + e.getMessage());
            if (!resource.isOptional()) {
                throw new ConfigDataResourceNotFoundException(resource, e);
            }
        }
        return null;
    }
}
