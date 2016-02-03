package com.hubspot.singularity.config;

import static junit.framework.TestCase.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTestBaseNoDb;

import io.dropwizard.configuration.ConfigurationSourceProvider;

public class MergingSourceProviderTest extends SingularityTestBaseNoDb {
    private static final String DEFAULT_PATH = "/configs/default.yaml";
    private static final String OVERRIDE_PATH = "/configs/override.yaml";
    private static final String JUST_A_STRING_PATH = "/configs/just_a_string.yaml";
    private static final String DOESNT_EXIST_PATH = "/configs/doesnt_exist.yaml";

    private static final YAMLFactory YAML_FACTORY = new YAMLFactory();

    @Inject
    private ObjectMapper objectMapper;

    private ConfigurationSourceProvider buildConfigurationSourceProvider(String baseFilename) {
        final Class<?> klass = getClass();

        return new MergingSourceProvider(new ConfigurationSourceProvider() {
            @Override
            public InputStream open(String path) throws IOException {
                final InputStream stream = klass.getResourceAsStream(path);
                if (stream == null) {
                    throw new FileNotFoundException("File " + path + " not found in test resources directory");
                }
                return stream;
            }
        }, baseFilename, objectMapper, YAML_FACTORY);
    }

    @Test
    public void testMergedConfigs() throws Exception {
        final InputStream mergedConfigStream = buildConfigurationSourceProvider(DEFAULT_PATH).open(OVERRIDE_PATH);
        final SingularityConfiguration mergedConfig = objectMapper.readValue(YAML_FACTORY.createParser(mergedConfigStream), SingularityConfiguration.class);

        assertEquals(10000, mergedConfig.getCacheTasksMaxSize());
        assertEquals(500, mergedConfig.getCacheTasksInitialSize());
        assertEquals(100, mergedConfig.getCheckDeploysEverySeconds());
        assertEquals("baseuser", mergedConfig.getDatabaseConfiguration().get().getUser());
        assertEquals("overridepassword", mergedConfig.getDatabaseConfiguration().get().getPassword());
    }

    @Test( expected = SingularityConfigurationMergeException.class )
    public void testNonObjectFails() throws Exception {
        buildConfigurationSourceProvider(DEFAULT_PATH).open(JUST_A_STRING_PATH);
    }

    @Test( expected = FileNotFoundException.class)
    public void testFileNoExistFail() throws Exception {
        buildConfigurationSourceProvider(DEFAULT_PATH).open(DOESNT_EXIST_PATH);
        buildConfigurationSourceProvider(DOESNT_EXIST_PATH).open(OVERRIDE_PATH);
    }
}
