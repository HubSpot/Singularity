package com.hubspot.singularity.config;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTestBaseNoDb;

import io.dropwizard.configuration.ConfigurationSourceProvider;

public class MergingSourceProviderTest extends SingularityTestBaseNoDb {
    private static final String OVERRIDE_PATH = "override.yaml";

    private static final YAMLFactory YAML_FACTORY = new YAMLFactory();

    @Inject
    private ObjectMapper objectMapper;

    @Test
    public void testMergedConfigs() throws Exception {
        final String base = "s3: {s3AccessKey: test-access-key}";
        final String override = "s3: {s3SecretKey: test-secret-key}";
        final ConfigurationSourceProvider mergedProvider = new MergingSourceProvider(new TestSourceProvider(base, override), OVERRIDE_PATH, objectMapper, YAML_FACTORY);

        final SingularityConfiguration mergedConfig = objectMapper.readValue(YAML_FACTORY.createParser(mergedProvider.open("config.yaml")), SingularityConfiguration.class);

        assertEquals("test-access-key", mergedConfig.getS3Configuration().get().getS3AccessKey());
        assertEquals("test-secret-key", mergedConfig.getS3Configuration().get().getS3SecretKey());
    }

    private static class TestSourceProvider implements ConfigurationSourceProvider {
        private final String defaultValue;
        private final String overrideValue;

        public TestSourceProvider(String defaultValue, String overrideValue) {
            this.defaultValue = defaultValue;
            this.overrideValue = overrideValue;
        }

        @Override
        public InputStream open(String path) throws IOException {
            return new ByteArrayInputStream((path.equals(OVERRIDE_PATH) ? overrideValue : defaultValue).getBytes(Charsets.UTF_8));
        }
    }
}
